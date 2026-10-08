package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Email inválido")
        String email
) {
}
