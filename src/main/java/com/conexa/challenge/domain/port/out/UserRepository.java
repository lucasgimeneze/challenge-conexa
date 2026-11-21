package com.conexa.challenge.domain.port.out;

import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    /**
     * Guarda un usuario nuevo. El {@code id} lo asigna quien crea el usuario, no el repositorio.
     *
     * @throws UserAlreadyExistsException si el username ya está registrado
     */
    User save(User user);
}
