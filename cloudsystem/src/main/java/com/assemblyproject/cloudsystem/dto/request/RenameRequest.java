package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RenameRequest(@NotBlank String name) {
}
