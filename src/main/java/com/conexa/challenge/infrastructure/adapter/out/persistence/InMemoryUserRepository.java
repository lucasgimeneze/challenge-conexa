package com.conexa.challenge.infrastructure.adapter.out.persistence;

import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.domain.port.out.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Repositorio en memoria: los usuarios se pierden al reiniciar la aplicación.
 * <p>
 * El mapa está indexado por username porque es la única búsqueda (login): la clave del mapa
 * cumple el rol de índice único. En un adapter JPA equivaldría a {@code @Id UUID id} +
 * {@code @Column(unique = true)} indexado sobre username.
 */
@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public User save(User user) {
        // putIfAbsent es atómico: evita que dos registros concurrentes con el mismo username se pisen.
        User existing = usersByUsername.putIfAbsent(user.username(), user);
        if (existing != null) {
            throw new UserAlreadyExistsException(user.username());
        }
        return user;
    }
}
