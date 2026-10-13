package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.Size;

public record ChangeRoleRequest(
        @Size(max = 40)
        String role
) {
}