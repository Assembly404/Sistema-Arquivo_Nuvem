package com.assemblyproject.cloudsystem.dto.response;

public record RegisterUserResponse(
        String name,
        String email,
        String token
) {
}
