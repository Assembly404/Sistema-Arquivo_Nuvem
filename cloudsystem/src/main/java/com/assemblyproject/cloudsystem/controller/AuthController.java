package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.TokenConfig;
import com.assemblyproject.cloudsystem.dto.request.ForgotPasswordRequest;
import com.assemblyproject.cloudsystem.dto.request.LoginRequest;
import com.assemblyproject.cloudsystem.dto.request.RegisterUserRequest;
import com.assemblyproject.cloudsystem.dto.request.ResetPasswordRequest;
import com.assemblyproject.cloudsystem.dto.request.VerifyOtpRequest;
import com.assemblyproject.cloudsystem.dto.response.AuthUserData;
import com.assemblyproject.cloudsystem.dto.response.LoginResponse;
import com.assemblyproject.cloudsystem.dto.response.RegisterUserResponse;
import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.PasswordResetService;
import com.assemblyproject.cloudsystem.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Cota padrão de 5 GiB para novas contas (nuvem do sistema). */
    public static final long DEFAULT_QUOTA_BYTES = 5L * 1024L * 1024L * 1024L;
    public static final String DEFAULT_STORAGE_POLICY = "SYSTEM_FIRST";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;
    private final PermissionService permissionService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserRepository repository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenConfig tokenConfig, PermissionService permissionService, PasswordResetService passwordResetService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
        this.permissionService = permissionService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userAndPass);

        User user = (User) authentication.getPrincipal();
        if (user.isSuspended()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        user.setLastAccess(LocalDateTime.now());
        repository.save(user);
        String token = tokenConfig.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(toAuthUser(user), token, "Bearer", 86400));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest registerRequest) {
        if (repository.findByEmail(registerRequest.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User newUser = new User();
        newUser.setPassword(passwordEncoder.encode(registerRequest.password()));
        newUser.setEmail(registerRequest.email().trim().toLowerCase());
        newUser.setName(registerRequest.name());
        newUser.setSurname(registerRequest.surname());
        newUser.setRole(Role.USER);
        newUser.setQuotaBytes(DEFAULT_QUOTA_BYTES);
        newUser.setStoragePolicy(DEFAULT_STORAGE_POLICY);

        LocalDateTime dateTime = LocalDateTime.now();
        newUser.setRegistrationData(dateTime);

        repository.save(newUser);
        String token = tokenConfig.generateToken(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterUserResponse(toAuthUser(newUser), token, "Bearer", 86400));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(Map.of("message", "Se existir conta, enviámos instruções."));
    }

    @PostMapping("/password/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            String resetToken = passwordResetService.verifyOtp(request.email(), request.otp());
            return ResponseEntity.ok(Map.of("resetToken", resetToken));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.resetToken(), request.password(), "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$");
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    private AuthUserData toAuthUser(User user) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        return new AuthUserData(user.getName(), user.getSurname(), user.getEmail(), role.name(), permissionService.permissionsFor(role));
    }
}
