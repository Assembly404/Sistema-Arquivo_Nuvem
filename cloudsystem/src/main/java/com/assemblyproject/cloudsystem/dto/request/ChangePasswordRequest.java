package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "A password atual é obrigatória")
        String currentPassword,
        @NotBlank(message = "A nova password é obrigatória")
        String newPassword
) {
}
