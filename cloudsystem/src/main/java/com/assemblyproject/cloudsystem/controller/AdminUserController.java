package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.ChangeRoleRequest;
import com.assemblyproject.cloudsystem.dto.request.CreateUserRequest;
import com.assemblyproject.cloudsystem.dto.response.AdminUserResponse;
import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD de utilizadores e definição de papeis — restrito a ROLE_ADMIN
 * pelo SecurityConfig (.requestMatchers("/admin/**").hasRole("ADMIN")).
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final String PASSWORD_RULE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list() {
        List<AdminUserResponse> users = repository.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request) {
        if (repository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Já existe uma conta com esse email."));
        }
        if (!request.password().matches(PASSWORD_RULE)) {
            return ResponseEntity.status(400).body(Map.of("message", "A password não cumpre os requisitos (8-24, maiúscula, minúscula, número, símbolo)."));
        }
        User user = new User();
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRegistrationData(java.time.LocalDateTime.now());
        user.setRole(parseRole(request.role()));
        user.setQuotaBytes(AuthController.DEFAULT_QUOTA_BYTES);
        user.setStoragePolicy(AuthController.DEFAULT_STORAGE_POLICY);
        repository.save(user);
        return ResponseEntity.status(201).body(toResponse(user));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request, Authentication authentication) {
        User user = repository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        Role newRole = parseRole(request.role());
        Role currentRole = user.getRole() != null ? user.getRole() : Role.USER;
        if (newRole == currentRole) {
            return ResponseEntity.ok(toResponse(user));
        }
        /* guarda: nunca remover o próprio acesso ADMIN (auto-demote) */
        if (currentRole == Role.ADMIN && newRole == Role.USER && isSelf(authentication, user)) {
            return ResponseEntity.status(409).body(Map.of("message", "Não podes remover o teu próprio papel de administrador."));
        }
        /* guarda: nunca deixar a plataforma sem nenhum ADMIN */
        if (currentRole == Role.ADMIN && newRole == Role.USER && repository.countByRole(Role.ADMIN) <= 1) {
            return ResponseEntity.status(409).body(Map.of("message", "Tem de existir pelo menos um administrador na plataforma."));
        }
        user.setRole(newRole);
        repository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        User user = repository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        /* guarda: não deixar apagar a própria conta */
        if (isSelf(authentication, user)) {
            return ResponseEntity.status(409).body(Map.of("message", "Não podes excluir a tua própria conta aqui — usa o Profile."));
        }
        /* guarda: nunca apagar o último ADMIN */
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        if (role == Role.ADMIN && repository.countByRole(Role.ADMIN) <= 1) {
            return ResponseEntity.status(409).body(Map.of("message", "Tem de existir pelo menos um administrador na plataforma."));
        }
        repository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long id, Authentication authentication) {
        User user = repository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        if (isSelf(authentication, user)) {
            return ResponseEntity.status(409).body(Map.of("message", "Não podes suspender a tua própria conta."));
        }
        if (user.getRole() == Role.ADMIN && repository.countByRole(Role.ADMIN) <= 1) {
            return ResponseEntity.status(409).body(Map.of("message", "Tem de existir pelo menos um administrador na plataforma."));
        }
        user.setSuspended(true);
        repository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/{id}/unsuspend")
    public ResponseEntity<?> unsuspendUser(@PathVariable Long id) {
        User user = repository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        user.setSuspended(false);
        repository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PatchMapping("/{id}/quota")
    public ResponseEntity<?> setQuota(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = repository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        Object quotaValue = body.get("quotaBytes");
        if (quotaValue == null) {
            user.setQuotaBytes(null); /* ilimitado */
        } else {
            long quota;
            try {
                quota = ((Number) quotaValue).longValue();
            } catch (ClassCastException | NullPointerException e) {
                return ResponseEntity.status(400).body(Map.of("message", "quotaBytes inválido."));
            }
            if (quota < 0) {
                return ResponseEntity.status(400).body(Map.of("message", "A cota não pode ser negativa."));
            }
            user.setQuotaBytes(quota);
        }
        repository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    private boolean isSelf(Authentication authentication, User target) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JWTUserData data)) {
            return false;
        }
        if (data.userID() != null && target.getUserID() != null && data.userID().equals(target.getUserID())) {
            return true;
        }
        return data.email() != null && data.email().equalsIgnoreCase(target.getEmail());
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return Role.USER;
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.USER;
        }
    }

    private AdminUserResponse toResponse(User user) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        return new AdminUserResponse(user.getUserID(), user.getName(), user.getSurname(),
                user.getEmail(), role.name(), user.getRegistrationData(), user.getLastAccess(), user.isSuspended(),
                user.getQuotaBytes());
    }
}
