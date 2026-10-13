package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PermissaoRequest(
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 80)
        String codigo,
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120)
        String nome,
        @Size(max = 255)
        String descricao,
        @NotNull(message = "O recurso é obrigatório")
        Long recursoId,
        Integer ordem
) {
}
