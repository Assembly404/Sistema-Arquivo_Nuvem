package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.TokenConfig;
import com.assemblyproject.cloudsystem.dto.request.LoginRequest;
import com.assemblyproject.cloudsystem.dto.request.RegisterUserRequest;
import com.assemblyproject.cloudsystem.dto.response.LoginResponse;
import com.assemblyproject.cloudsystem.dto.response.RegisterUserResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenConfig tokenConfig) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userAndPass);

        User user = (User) authentication.getPrincipal();
        if (user.isSuspended()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String token = tokenConfig.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest registerRequest) {
        User saved = authService.registerNewUser(
                registerRequest.name(), registerRequest.surname(), registerRequest.email(),
                passwordEncoder.encode(registerRequest.password()), registerRequest.getRole()
        );
        String token = tokenConfig.generateToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterUserResponse(saved.getName() + " " + saved.getSurname(), saved.getEmail(), token));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<RegisterUserResponse> registerAdmin(@AuthenticationPrincipal UserDetails requester,
                                                              @Valid @RequestBody RegisterUserRequest registerRequest) {
        User requesterUser = userRepository.findUserByEmail(requester.getUsername())
                .map(u -> (User) u)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        if (requesterUser.getRole() == null || requesterUser.getRole().getName() != com.assemblyproject.cloudsystem.entity.UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User saved = authService.createAdminBySuperAdmin(
                registerRequest.name(), registerRequest.surname(), registerRequest.email(),
                passwordEncoder.encode(registerRequest.password()),
                registerRequest.getRole() != null ? registerRequest.getRole() : com.assemblyproject.cloudsystem.entity.UserRole.ADMIN
        );
        String token = tokenConfig.generateToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterUserResponse(saved.getName() + " " + saved.getSurname(), saved.getEmail(), token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> opt = userRepository.findUserByEmail(userDetails.getUsername()).map(u -> (User) u);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        return ResponseEntity.ok(Map.of(
                "userID", u.getUserID(),
                "name", u.getName(),
                "email", u.getEmail(),
                "role", u.getRole() != null ? u.getRole().getName().name() : "USER",
                "suspended", u.isSuspended()
        ));
    }
}
