package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.ChangeRoleRequest;
import com.assemblyproject.cloudsystem.dto.request.CreateUserRequest;
import com.assemblyproject.cloudsystem.dto.request.UpdateUserAdminRequest;
import com.assemblyproject.cloudsystem.dto.response.AdminUserResponse;
import com.assemblyproject.cloudsystem.entity.Papel;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.PapelRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD de utilizadores e atribuição de perfis/papeis.
 * Área restrita a ROLE_ADMIN (ou perfis com ADMIN_USUARIOS) pelo SecurityConfig.
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final String PASSWORD_RULE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$";
    private static final String ADMIN_ROLE = PermissionService.ROLE_ADMIN;

    private final UserRepository repository;
    private final PapelRepository papelRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public AdminUserController(UserRepository repository, PapelRepository papelRepository,
                               PasswordEncoder passwordEncoder, PermissionService permissionService) {
        this.repository = repository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list(@RequestParam(value = "search", required = false) String search) {
        List<User> users;
        if (search == null || search.isBlank()) {
            users = repository.findAll();
        } else {
            users = repository.searchByTerm("%" + search.trim().toLowerCase() + "%");
        }
        users.sort((a, b) -> {
            int byName = a.getName().compareToIgnoreCase(b.getName());
            return byName != 0 ? byName : a.getSurname().compareToIgnoreCase(b.getSurname());
        });
        return ResponseEntity.ok(users.stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request) {
        if (repository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Já existe uma conta com esse email."));
        }
        if (!request.password().matches(PASSWORD_RULE)) {
            return ResponseEntity.status(400).body(Map.of("message", "A password não cumpre os requisitos (8-24, maiúscula, minúscula, número, símbolo)."));
        }
        Papel papel = resolvePapel(request.role());
        if (papel == null) {
            return ResponseEntity.status(400).body(Map.of("message", "O perfil selecionado não existe."));
        }
        User user = new User();
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRegistrationData(java.time.LocalDateTime.now());
        user.setRole(papel.getCodigo());
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
        Papel novoPapel = resolvePapel(request.role());
        if (novoPapel == null) {
            return ResponseEntity.status(400).body(Map.of("message", "O perfil selecionado não existe."));
        }
        String currentRole = user.getRole() != null ? user.getRole() : ADMIN_ROLE.equals(request.role()) ? ADMIN_ROLE : PermissionService.ROLE_USER;
        if (novoPapel.getCodigo().equals(currentRole)) {
            return ResponseEntity.ok(toResponse(user));
        }
        /* guarda: nunca remover o próprio acesso ADMIN (auto-demote) */
        if (ADMIN_ROLE.equals(currentRole) && !ADMIN_ROLE.equals(novoPapel.getCodigo()) && isSelf(authentication, user)) {
            return ResponseEntity.status(409).body(Map.of("message", "Não podes remover o teu próprio papel de administrador."));
        }
        /* guarda: nunca deixar a plataforma sem nenhum ADMIN */
        if (ADMIN_ROLE.equals(currentRole) && !ADMIN_ROLE.equals(novoPapel.getCodigo()) && repository.countByRole(ADMIN_ROLE) <= 1) {
            return ResponseEntity.status(409).body(Map.of("message", "Tem de existir pelo menos um administrador na plataforma."));
        }
        user.setRole(novoPapel.getCodigo());
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
            return ResponseEntity.status(409).body(Map.of("message", "Não podes excluir a tua própria conta aqui — usa o Perfil."));
        }
        /* guarda: nunca apagar o último ADMIN */
        String role = user.getRole() != null ? user.getRole() : PermissionService.ROLE_USER;
        if (ADMIN_ROLE.equals(role) && repository.countByRole(ADMIN_ROLE) <= 1) {
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
        if (ADMIN_ROLE.equals(user.getRole()) && repository.countByRole(ADMIN_ROLE) <= 1) {
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

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UpdateUserAdminRequest request,
                                        Authentication authentication) {
        User user = repository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        String role = user.getRole();
        if (request.role() != null && !request.role().isBlank()) {
            Papel papel = resolvePapel(request.role());
            if (papel == null) {
                return ResponseEntity.status(400).body(Map.of("message", "O perfil selecionado não existe."));
            }
            String current = user.getRole() != null ? user.getRole() : PermissionService.ROLE_USER;
            if (!papel.getCodigo().equals(current) && ADMIN_ROLE.equals(current) && !ADMIN_ROLE.equals(papel.getCodigo())) {
                if (isSelf(authentication, user)) {
                    return ResponseEntity.status(409).body(Map.of("message", "Não podes remover o teu próprio papel de administrador."));
                }
                if (repository.countByRole(ADMIN_ROLE) <= 1) {
                    return ResponseEntity.status(409).body(Map.of("message", "Tem de existir pelo menos um administrador na plataforma."));
                }
            }
            role = papel.getCodigo();
        }
        /* quota: null = ilimitada */
        if (request.quotaBytes() != null && request.quotaBytes() < 0) {
            return ResponseEntity.status(400).body(Map.of("message", "A cota não pode ser negativa."));
        }
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setRole(role);
        user.setQuotaBytes(request.quotaBytes());
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

    private Papel resolvePapel(String value) {
        if (value == null || value.isBlank()) {
            return papelRepository.findByCodigoIgnoreCase(PermissionService.ROLE_USER).orElse(null);
        }
        return papelRepository.findByCodigoIgnoreCase(value.trim()).orElse(null);
    }

    private AdminUserResponse toResponse(User user) {
        String role = user.getRole() != null ? user.getRole() : PermissionService.ROLE_USER;
        return new AdminUserResponse(user.getUserID(), user.getName(), user.getSurname(),
                user.getEmail(), role, permissionService.roleNome(role),
                user.getRegistrationData(), user.getLastAccess(), user.isSuspended(),
                user.getQuotaBytes());
    }
}