package com.conexa.challenge.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Usuario registrado. Solo guarda el hash de la contraseña, nunca el texto plano.
 * El {@code username} es único y es la clave de búsqueda del login.
 */
public record User(UUID id, String username, String passwordHash) {

    public User {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(username, "username is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
    }

    /** Excluye el hash para que no termine en logs. */
    @Override
    public String toString() {
        return "User[id=%s, username=%s]".formatted(id, username);
    }
}
