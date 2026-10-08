package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100)
        String name,
        @NotBlank(message = "O apelido é obrigatório")
        @Size(max = 100)
        String surname,
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 100)
        String email,
        @NotBlank(message = "A password é obrigatória")
        String password,
        @Pattern(regexp = "USER|ADMIN", message = "O papel deve ser USER ou ADMIN")
        String role
) {
}
