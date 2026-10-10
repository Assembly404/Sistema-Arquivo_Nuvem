package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterNodeRequest(
        @NotBlank(message = "O nome do nó é obrigatório")
        String name,
        Long totalSpaceBytes,
        String visibility,
        String address
) {
}
