package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecursoRequest(
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 60)
        String codigo,
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120)
        String nome,
        @Size(max = 255)
        String descricao,
        @Size(max = 60)
        String icone,
        Integer ordem
) {
}
