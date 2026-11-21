package com.conexa.challenge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.conexa.challenge.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class JwtTokenProviderTest {

    private static final AuthProperties PROPERTIES = new AuthProperties(
            new AuthProperties.Jwt("test-secret-0123456789abcdef0123456789", Duration.ofHours(1)),
            new AuthProperties.DemoUser(false, null, null));

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtDecoder decoder = securityConfig.jwtDecoder(PROPERTIES);

    @Test
    void issuesSignedTokenWithUserIdAsSubjectAndConfiguredExpiration() {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        JwtTokenProvider provider = new JwtTokenProvider(
                securityConfig.jwtEncoder(PROPERTIES), PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));
        User user = new User(UUID.randomUUID(), "luke", "hash");

        JwtTokenProvider.AccessToken token = provider.issue(user);
        Jwt decoded = decoder.decode(token.value());

        assertThat(decoded.getSubject()).isEqualTo(user.id().toString());
        assertThat(decoded.getClaimAsString("username")).isEqualTo("luke");
        assertThat(decoded.getIssuedAt()).isEqualTo(now);
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1)));
        assertThat(token.expiresInSeconds()).isEqualTo(3600);
    }
}
