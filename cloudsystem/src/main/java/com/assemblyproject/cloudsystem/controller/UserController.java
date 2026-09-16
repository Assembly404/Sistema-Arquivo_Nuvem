package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.entity.AuthRequest;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.service.JwtService;
import com.assemblyproject.cloudsystem.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService service;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserController(UserService service, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.service = service;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("welcome")
    public String welcome() {
        return "Welcome this endpoint is not secure";
    }

    @PostMapping("/addNewUser")
    public ResponseEntity<?> addNewUser(@RequestBody User user) {
        if (user == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        Optional<User> savedUser;
        try {
            savedUser = service.createUser(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Já existe esse email no sistema");
        }

        if (savedUser.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generateToken")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate((
                new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password())
                ));
        if (authentication.isAuthenticated()) {
               return jwtService.generateToken(authRequest.email());
        } else {
            throw new UsernameNotFoundException("Invalid user request");
        }
    }

}
