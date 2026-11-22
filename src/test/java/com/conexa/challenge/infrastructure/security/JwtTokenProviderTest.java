package com.conexa.challenge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.conexa.challenge.domain.model.AccessToken;
import com.conexa.challenge.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class JwtTokenProviderTest {

    private static final AuthProperties PROPERTIES = new AuthProperties(
            new AuthProperties.Jwt(null, Duration.ofHours(1)));

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final SecretKey signingKey = securityConfig.jwtSigningKey(PROPERTIES);
    private final JwtDecoder decoder = securityConfig.jwtDecoder(signingKey);

    @Test
    void issuesSignedTokenWithUserIdAsSubjectAndConfiguredExpiration() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtTokenProvider provider = new JwtTokenProvider(
                securityConfig.jwtEncoder(signingKey), PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));
        User user = new User(UUID.randomUUID(), "luke", "hash");

        AccessToken token = provider.issue(user);
        Jwt decoded = decoder.decode(token.value());

        assertThat(decoded.getSubject()).isEqualTo(user.id().toString());
        assertThat(decoded.getClaimAsString("username")).isEqualTo("luke");
        assertThat(decoded.getIssuedAt()).isEqualTo(now);
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1)));
        assertThat(token.expiresInSeconds()).isEqualTo(3600);
    }
}
