package com.conexa.challenge.infrastructure.adapter.out.persistence;

import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.domain.port.out.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {

    // Indexado por username (la búsqueda del login): la clave del mapa funciona como índice único.
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public User save(User user) {
        // putIfAbsent es atómico: dos registros concurrentes con el mismo username no se pisan.
        User existing = usersByUsername.putIfAbsent(user.username(), user);
        if (existing != null) {
            throw new UserAlreadyExistsException(user.username());
        }
        return user;
    }
}
