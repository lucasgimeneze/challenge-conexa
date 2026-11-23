package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.INVALID_RESPONSE;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import java.net.http.HttpClient;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sin {@code sync = true} en la cache: en Java 21 bloquearía los hilos portadores de los virtual threads
 * mientras se espera a SWAPI. La cache queda por fuera de la resiliencia: un hit no pasa por retry ni circuito.
 * Cada método tiene su cache porque la clave se arma solo con los argumentos: compartiéndola, un 404 que
 * {@code find} guardó como vacío haría que {@code fetch} devuelva {@code null} en vez de fallar.
 */
@Component
public class SwapiClient {

    private static final Logger log = LoggerFactory.getLogger(SwapiClient.class);

    private final RestClient restClient;
    private final SwapiErrorTranslator errorTranslator;
    private final SwapiResilience resilience;

    public SwapiClient(SwapiProperties properties, SwapiErrorTranslator errorTranslator) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
        this.errorTranslator = errorTranslator;
        this.resilience = new SwapiResilience(properties.retry(), properties.circuitBreaker());
    }

    @Cacheable("swapi-responses")
    public <R> R fetch(ParameterizedTypeReference<R> responseType, String uriTemplate, Object... uriVariables) {
        return find(responseType, uriTemplate, uriVariables)
                .orElseThrow(() -> new ExternalServiceException(INVALID_RESPONSE));
    }

    @Cacheable("swapi-lookups")
    public <R> Optional<R> find(ParameterizedTypeReference<R> responseType, String uriTemplate, Object... uriVariables) {
        return resilience.execute(() -> request(responseType, uriTemplate, uriVariables));
    }

    private <R> Optional<R> request(ParameterizedTypeReference<R> responseType, String uriTemplate, Object... uriVariables) {
        try {
            R body = restClient.get()
                    .uri(uriTemplate, uriVariables)
                    .retrieve()
                    .body(responseType);
            if (body == null) {
                throw new ExternalServiceException(INVALID_RESPONSE);
            }
            return Optional.of(body);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            ExternalServiceException failure = errorTranslator.translate(ex);
            log.warn("SWAPI request {} failed: {} ({})", uriTemplate, failure.reason(), ex.getClass().getSimpleName());
            throw failure;
        }
    }

    private static JdkClientHttpRequestFactory requestFactory(SwapiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }
}
