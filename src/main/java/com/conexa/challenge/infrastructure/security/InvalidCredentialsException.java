package com.conexa.challenge.infrastructure.security;

/**
 * Login fallido. Mismo mensaje si el usuario no existe o si la contraseña es incorrecta,
 * para no revelar qué usernames están registrados.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
