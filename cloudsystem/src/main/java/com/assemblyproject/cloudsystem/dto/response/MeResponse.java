package com.assemblyproject.cloudsystem.dto.response;

public record MeResponse(
        String name,
        String surname,
        String email,
        String role,
        java.util.List<String> permissions,
        java.time.LocalDateTime dataRegistro,
        java.time.LocalDateTime ultimoAcesso,
        Long quotaBytes,
        String storagePolicy
) {
}
