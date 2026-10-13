package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Atualização de um utilizador pela administração: dados pessoais, perfil e quota.
 */
public record UpdateUserAdminRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100)
        String name,
        @NotBlank(message = "O apelido é obrigatório")
        @Size(max = 100)
        String surname,
        @Size(max = 40)
        String role,
        Long quotaBytes
) {
}