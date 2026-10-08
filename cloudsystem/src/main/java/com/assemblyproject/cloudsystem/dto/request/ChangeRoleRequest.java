package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.Pattern;

public record ChangeRoleRequest(
        @Pattern(regexp = "USER|ADMIN", message = "O papel deve ser USER ou ADMIN")
        String role
) {
}
