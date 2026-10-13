package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PapelRequest(
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 40)
        String codigo,
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120)
        String nome,
        @Size(max = 255)
        String descricao,
        Integer ordem
) {
}
