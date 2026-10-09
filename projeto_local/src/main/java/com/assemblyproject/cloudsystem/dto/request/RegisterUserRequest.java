package com.assemblyproject.cloudsystem.dto.request;

import com.assemblyproject.cloudsystem.entity.UserRole;
import jakarta.validation.constraints.NotEmpty;

public record RegisterUserRequest(
        @NotEmpty(message = "O nome obrigatório")
        String name,
        @NotEmpty(message = "O apelido é obrigatório")
        String surname,
        @NotEmpty(message = "O email é obrigatório")
        String email,
        @NotEmpty(message = "A password é obrigatória")
        String password,
        UserRole role
) {
    public UserRole getRole() { return role; }
}
