package com.assemblyproject.cloudsystem.configuration;

import lombok.Builder;

@Builder
public record JWTUserData(
        Long userID,
        String email
) {
}
