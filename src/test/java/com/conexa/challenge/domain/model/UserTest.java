package com.conexa.challenge.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void toStringDoesNotExposePasswordHash() {
        User user = new User(UUID.randomUUID(), "luke", "$2a$10$secret-hash");

        assertThat(user.toString())
                .contains("luke")
                .doesNotContain("secret-hash");
    }
}
