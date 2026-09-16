package com.assemblyproject.cloudsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record AuthRequest (
        String email,
        String password
){
}
