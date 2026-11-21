package com.conexa.challenge.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryUserRepositoryTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void savesAndFindsUserByUsername() {
        User user = user("luke", "hash");

        repository.save(user);

        assertThat(repository.findByUsername("luke")).contains(user);
    }

    @Test
    void returnsEmptyWhenUserDoesNotExist() {
        assertThat(repository.findByUsername("vader")).isEmpty();
    }

    @Test
    void rejectsDuplicatedUsernameAndKeepsOriginalUser() {
        User original = user("luke", "hash-1");
        repository.save(original);

        assertThatThrownBy(() -> repository.save(user("luke", "hash-2")))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("luke");

        assertThat(repository.findByUsername("luke")).contains(original);
    }

    private static User user(String username, String passwordHash) {
        return new User(UUID.randomUUID(), username, passwordHash);
    }
}
