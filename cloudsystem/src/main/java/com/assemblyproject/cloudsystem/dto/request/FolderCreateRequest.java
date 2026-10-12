package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FolderCreateRequest(@NotBlank String name, Long parentId) {
}
