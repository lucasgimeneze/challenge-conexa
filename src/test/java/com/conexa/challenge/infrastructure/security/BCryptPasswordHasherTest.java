package com.conexa.challenge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashDoesNotContainTheRawPassword() {
        assertThat(hasher.hash("skywalker123")).doesNotContain("skywalker123");
    }

    @Test
    void matchesOnlyTheOriginalPassword() {
        String hash = hasher.hash("skywalker123");

        assertThat(hasher.matches("skywalker123", hash)).isTrue();
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }
}
