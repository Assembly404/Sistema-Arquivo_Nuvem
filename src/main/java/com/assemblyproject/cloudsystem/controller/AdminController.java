package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.RegisterUserRequest;
import com.assemblyproject.cloudsystem.dto.response.AdminUserResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.entity.UserRole;
import com.assemblyproject.cloudsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;

    private boolean isSuperAdmin(User u) {
        return u.getRole() != null && u.getRole().getName() == UserRole.SUPER_ADMIN;
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@AuthenticationPrincipal JWTUserData requester,
                                        @Valid @RequestBody RegisterUserRequest req) {
        User requesterUser = authService.findById(requester.userID())
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        if (!isSuperAdmin(requesterUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only SUPER_ADMIN can create admin users");
        }
        User created = authService.createAdminBySuperAdmin(req.name(), req.surname(), req.email(), req.password(), req.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PostMapping("/suspend/{userID}")
    public ResponseEntity<?> suspendUser(@AuthenticationPrincipal JWTUserData requester,
                                           @PathVariable Long userID) {
        User requesterUser = authService.findById(requester.userID())
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        if (!isSuperAdmin(requesterUser) && requesterUser.getUserID().equals(userID)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not allowed");
        }
        User updated = authService.suspendUser(userID, true);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/unsuspend/{userID}")
    public ResponseEntity<?> unsuspendUser(@AuthenticationPrincipal JWTUserData requester,
                                             @PathVariable Long userID) {
        User requesterUser = authService.findById(requester.userID())
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        if (!isSuperAdmin(requesterUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only SUPER_ADMIN");
        }
        User updated = authService.suspendUser(userID, false);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PutMapping("/role/{userID}")
    public ResponseEntity<?> changeRole(@AuthenticationPrincipal JWTUserData requester,
                                        @PathVariable Long userID,
                                        @RequestBody RoleChangeRequest body) {
        User requesterUser = authService.findById(requester.userID())
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        if (!isSuperAdmin(requesterUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only SUPER_ADMIN");
        }
        User updated = authService.changeUserRole(userID, body.role());
        return ResponseEntity.ok(toResponse(updated));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers(@AuthenticationPrincipal JWTUserData requester) {
        User requesterUser = authService.findById(requester.userID())
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        if (!isSuperAdmin(requesterUser) && requesterUser.getRole().getName() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<AdminUserResponse> list = authService.listAllUsers().stream().map(u ->
                new AdminUserResponse(u.getUserID(), u.getName(), u.getSurname(), u.getEmail(),
                        u.getRole() != null ? u.getRole().getName() : UserRole.USER,
                        u.getRegistrationData(), u.getLastAccess(), u.isSuspended())
        ).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private AdminUserResponse toResponse(User u) {
        return new AdminUserResponse(u.getUserID(), u.getName(), u.getSurname(), u.getEmail(),
                u.getRole() != null ? u.getRole().getName() : UserRole.USER,
                u.getRegistrationData(), u.getLastAccess(), u.isSuspended());
    }

    public record RoleChangeRequest(UserRole role) {}
}
