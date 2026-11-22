package com.conexa.challenge.infrastructure.adapter.in.web;

import com.conexa.challenge.application.service.AuthService;
import com.conexa.challenge.domain.model.AccessToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "User registration and login to obtain a bearer token")
@SecurityRequirements
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a user", description = "Creates the user. Then log in to get a token.")
    @ApiResponse(responseCode = "201", description = "User created. No response body.")
    @ApiResponse(responseCode = "400", description = "Invalid username or password. `errors` maps each field to its message.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The username is already taken.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests from this IP. Includes `Retry-After`.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.password());
    }

    @Operation(summary = "Log in", description = "Returns a bearer token to use in the `Authorization` header.")
    @ApiResponse(responseCode = "200", description = "Valid credentials.")
    @ApiResponse(responseCode = "400", description = "Missing username or password.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Invalid username or password.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "Too many requests from this IP. Includes `Retry-After`.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
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

            // Solo ASCII: así 72 caracteres son 72 bytes, el máximo que acepta BCrypt.
            @NotBlank(message = "is required")
            @Size(min = 8, max = 72, message = "must have between 8 and 72 characters")
            @Pattern(regexp = "^[\\x20-\\x7E]*$",
                    message = "may only contain letters, digits, spaces and symbols (no accents or emojis)")
            String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "is required") String username,
            @NotBlank(message = "is required") String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
