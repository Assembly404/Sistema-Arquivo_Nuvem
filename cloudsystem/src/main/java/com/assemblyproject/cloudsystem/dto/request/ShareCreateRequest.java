package com.assemblyproject.cloudsystem.dto.request;

import com.assemblyproject.cloudsystem.entity.SharePermission;
import jakarta.validation.constraints.NotBlank;

public record ShareCreateRequest(
        @NotBlank(message = "O email é obrigatório")
        String email,
        Long fileId,
        Long folderId,
        SharePermission permission
) {
}