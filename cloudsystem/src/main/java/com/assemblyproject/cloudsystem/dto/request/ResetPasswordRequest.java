package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "O token de reposição é obrigatório")
        String resetToken,
        @NotBlank(message = "A nova password é obrigatória")
        @Size(min = 8, max = 24, message = "A password deve ter 8 a 24 caracteres")
        String password
) {
}
