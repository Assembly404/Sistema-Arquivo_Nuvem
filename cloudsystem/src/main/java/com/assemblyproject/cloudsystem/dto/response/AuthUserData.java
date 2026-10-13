package com.assemblyproject.cloudsystem.dto.response;

import java.util.List;

public record AuthUserData(
        String name,
        String surname,
        String email,
        String role,
        String roleNome,
        List<String> permissions
) {
}
