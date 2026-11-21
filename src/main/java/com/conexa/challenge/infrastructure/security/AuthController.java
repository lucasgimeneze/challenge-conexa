package com.conexa.challenge.infrastructure.security;

import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.infrastructure.security.JwtTokenProvider.AccessToken;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.username(), request.password());
        return new RegisterResponse(user.id(), user.username());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AccessToken token = authService.login(request.username(), request.password());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    public record RegisterRequest(
            @NotBlank(message = "is required")
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                    message = "must have 3 to 50 characters: letters, digits, '.', '_' or '-'")
            String username,

            @NotBlank(message = "is required")
            @Size(min = 8, message = "must have at least 8 characters")
            @MaxBytes(value = 72, message = "is too long (max 72 bytes)")
            String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "is required") String username,
            @NotBlank(message = "is required") String password) {
    }

    public record RegisterResponse(UUID id, String username) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
