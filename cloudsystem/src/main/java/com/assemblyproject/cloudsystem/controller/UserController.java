package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.ChangePasswordRequest;
import com.assemblyproject.cloudsystem.dto.request.UpdateMeRequest;
import com.assemblyproject.cloudsystem.dto.response.MeResponse;
import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final String PASSWORD_RULE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public UserController(UserRepository repository, PasswordEncoder passwordEncoder, PermissionService permissionService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Não autenticado."));
        }
        return ResponseEntity.ok(toMe(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMe(@Valid @RequestBody UpdateMeRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Não autenticado."));
        }
        user.setName(request.name());
        user.setSurname(request.surname());
        repository.save(user);
        return ResponseEntity.ok(toMe(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Não autenticado."));
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "A password atual está incorreta."));
        }
        if (request.newPassword() == null || !request.newPassword().matches(PASSWORD_RULE)) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "A nova password não cumpre os requisitos."));
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "A nova password deve ser diferente da atual."));
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        repository.save(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Não autenticado."));
        }
        repository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test")
    public String test() {
        return "Aqui está o user controller";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JWTUserData data)) {
            return null;
        }
        if (data.userID() != null) {
            return repository.findById(data.userID()).orElse(null);
        }
        return repository.findByEmail(data.email()).orElse(null);
    }

    private MeResponse toMe(User user) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        return new MeResponse(user.getName(), user.getSurname(), user.getEmail(), role.name(),
                permissionService.permissionsFor(role), user.getRegistrationData(), user.getLastAccess());
    }
}

