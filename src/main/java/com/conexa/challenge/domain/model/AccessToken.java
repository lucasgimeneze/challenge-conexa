package com.conexa.challenge.domain.model;

public record AccessToken(String value, long expiresInSeconds) {
}
