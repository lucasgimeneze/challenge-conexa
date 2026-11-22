package com.conexa.challenge.infrastructure.adapter.out.swapi;

import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.model.StarWarsResource;
import com.conexa.challenge.domain.port.out.StarWarsCatalog;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiItem;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiList;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiPage;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiResourceProperties;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiSingle;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

public class RemoteSwapiCatalog<T extends StarWarsResource, P extends SwapiResourceProperties<T>>
        implements StarWarsCatalog<T> {

    private static final Pattern REGEX_METACHARACTERS = Pattern.compile("[\\\\^$.|?*+()\\[\\]{}]");

    private final SwapiClient client;
    private final String resource;
    private final ParameterizedTypeReference<SwapiPage<P>> pageType;
    private final ParameterizedTypeReference<SwapiList<P>> listType;
    private final ParameterizedTypeReference<SwapiSingle<P>> singleType;

    public RemoteSwapiCatalog(SwapiClient client, String resource, Class<P> propertiesType) {
        this.client = client;
        this.resource = resource;
        this.pageType = typeOf(SwapiPage.class, propertiesType);
        this.listType = typeOf(SwapiList.class, propertiesType);
        this.singleType = typeOf(SwapiSingle.class, propertiesType);
    }

    @Override
    public PageResult<T> findPage(int page, int size) {
        SwapiPage<P> response = client.fetch(pageType, "/{resource}?page={page}&limit={limit}&expanded=true",
                resource, page, size);
        if (page > response.totalPages()) {
            return PageResult.empty(page, size, response.totalRecords());
        }
        return new PageResult<>(toDomain(response.results()), page, size, response.totalRecords());
    }

    @Override
    public Optional<T> findById(int id) {
        return client.find(singleType, "/{resource}/{id}", resource, id)
                .flatMap(SwapiSingle::item)
                .map(this::toDomain);
    }

    // SWAPI arma una regex de MongoDB con el nombre: sin escapar, "(" rompe la búsqueda y "." trae todo.
    @Override
    public List<T> findByName(String name) {
        String literalName = REGEX_METACHARACTERS.matcher(name).replaceAll("\\\\$0");
        return toDomain(client.fetch(listType, "/{resource}?name={name}", resource, literalName).result());
    }

    private List<T> toDomain(List<SwapiItem<P>> items) {
        return items.stream().map(this::toDomain).toList();
    }

    private T toDomain(SwapiItem<P> item) {
        return item.properties().toDomain(item.uid());
    }

    private static <R> ParameterizedTypeReference<R> typeOf(Class<?> envelope, Class<?> properties) {
        return ParameterizedTypeReference.forType(ResolvableType.forClassWithGenerics(envelope, properties).getType());
    }
}
