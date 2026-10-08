package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.UserRole;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long userID;
    private String name;
    private String surname;
    private String email;
    private UserRole role;
    private LocalDateTime registrationData;
    private LocalDateTime lastAccess;
    private boolean suspended;

    public AdminUserResponse(Long userID, String name, String surname, String email, UserRole role, LocalDateTime registrationData, LocalDateTime lastAccess, boolean suspended) {
        this.userID = userID;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.role = role;
        this.registrationData = registrationData;
        this.lastAccess = lastAccess;
        this.suspended = suspended;
    }
}
