package com.conexa.challenge.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Registra el usuario demo al arrancar usando el mismo flujo que {@code /auth/register}.
 */
@Component
@ConditionalOnBooleanProperty("auth.demo-user.enabled")
class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final AuthService authService;
    private final AuthProperties properties;

    DemoUserSeeder(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        AuthProperties.DemoUser demoUser = properties.demoUser();
        Assert.hasText(demoUser.username(), "auth.demo-user.username (DEMO_USERNAME) is required when the demo user is enabled");
        Assert.hasText(demoUser.password(), "auth.demo-user.password (DEMO_PASSWORD) is required when the demo user is enabled");

        authService.register(demoUser.username(), demoUser.password());
        log.info("Demo user '{}' registered", demoUser.username());
    }
}
