package com.conexa.challenge.infrastructure.security;

/**
 * Mismo mensaje si el usuario no existe o si la contraseña es incorrecta,
 * para no indicarle al cliente cuál de los dos falló.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
