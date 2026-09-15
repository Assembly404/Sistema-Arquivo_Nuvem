package com.assemblyproject.cloudsystem.controller;

//import com.assemblyproject.cloudsystem.dto.UserMapper;
import com.assemblyproject.cloudsystem.dto.UserMapper;
import com.assemblyproject.cloudsystem.dto.UserRegisterDto;
import com.assemblyproject.cloudsystem.entity.AuthRequest;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class Auth {

    private final UserService userService;
    private final UserMapper userMapper;

    public Auth(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

//    @PostMapping("/login")
//    @CrossOrigin(origins = "http://127.0.0.1:5500")
//    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest)  {
//        if (authRequest.email() == null || authRequest.password() == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//
//        Optional<User> user = userService.;
//
//        if (user.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(auth);
//        }
//
//        return ResponseEntity.status(HttpStatus.OK).body(userMapper.mapToDto(user.get()));
//    }

    @PostMapping("/addNewUser")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> cadastro(@RequestBody UserRegisterDto userRegisterDto) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (userRegisterDto.name() == null || userRegisterDto.surname() == null|| userRegisterDto.email() == null || userRegisterDto.password() == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        Optional<User> user;
        try {

            user = userService.createUser(userMapper.mapRegisterDtoToUser(userRegisterDto));

            if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(userRegisterDto);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Já existe um usuário com esse email");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.mapToDto(user.get()));
    }
}
