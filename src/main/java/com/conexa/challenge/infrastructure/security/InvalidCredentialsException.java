package com.conexa.challenge.infrastructure.security;

/**
 * Login fallido. Mismo mensaje si el usuario no existe o si la contraseña es incorrecta,
 * para no darle al cliente detalle de cuál de los dos falló.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
