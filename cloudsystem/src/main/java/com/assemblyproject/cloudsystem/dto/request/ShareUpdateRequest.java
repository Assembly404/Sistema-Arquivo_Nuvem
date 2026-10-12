package com.assemblyproject.cloudsystem.dto.request;

import com.assemblyproject.cloudsystem.entity.SharePermission;
import jakarta.validation.constraints.NotNull;

public record ShareUpdateRequest(
        @NotNull(message = "A permissão é obrigatória")
        SharePermission permission
) {
}