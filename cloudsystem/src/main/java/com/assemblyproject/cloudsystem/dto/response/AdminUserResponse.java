package com.assemblyproject.cloudsystem.dto.response;

public record AdminUserResponse(
        Long id,
        String name,
        String surname,
        String email,
        String role,
        java.time.LocalDateTime dataRegistro,
        java.time.LocalDateTime ultimoAcesso,
        boolean suspended,
        Long quotaBytes
) {
}
