package com.assemblyproject.cloudsystem.dto;

public record UserRegisterDto(
        String name,
        String surname,
        String email,
        String password
) {
}