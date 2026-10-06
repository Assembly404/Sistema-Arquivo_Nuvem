package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.TokenConfig;
import com.assemblyproject.cloudsystem.dto.request.LoginRequest;
import com.assemblyproject.cloudsystem.dto.request.RegisterUserRequest;
import com.assemblyproject.cloudsystem.dto.response.LoginResponse;
import com.assemblyproject.cloudsystem.dto.response.RegisterUserResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.RoleRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;

    public AuthController(UserRepository repository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenConfig tokenConfig, RoleRepository roleRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userAndPass);

        User user = (User) authentication.getPrincipal();
        String token = tokenConfig.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest registerRequest) {
        User newUser = new User();
        newUser.setPassword(passwordEncoder.encode(registerRequest.password()));
        newUser.setEmail(registerRequest.email());
        newUser.setName(registerRequest.name());
        newUser.setSurname(registerRequest.surname());

        LocalDateTime dateTime = LocalDateTime.now();
        newUser.setRegistrationData(dateTime);

        newUser.setRole(roleRepository.getReferenceById(3L));

        if (isEmailUnique(registerRequest.email())) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        repository.save(newUser);
        String token = tokenConfig.generateToken(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterUserResponse(newUser.getName() + " " + newUser.getSurname(), newUser.getEmail(), token));
    }

    public boolean isEmailUnique(String email) {
        if (email.isEmpty()) return false;

        Optional<UserDetails> userVerify = repository.findUserByEmail(email);

        if (userVerify.isPresent()) {
            return true;
        }
        return false;
    }
}
