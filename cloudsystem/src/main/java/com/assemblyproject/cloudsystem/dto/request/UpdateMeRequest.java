package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMeRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,
        @NotBlank(message = "O apelido é obrigatório")
        String surname
) {
}
