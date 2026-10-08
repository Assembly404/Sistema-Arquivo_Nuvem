package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.entity.UserRole;
import com.assemblyproject.cloudsystem.repository.RoleRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public User registerNewUser(String name, String surname, String email, String password, UserRole requestedRole) {
        UserRole effectiveRole = requestedRole != null ? requestedRole : UserRole.USER;
        Role roleEntity = roleRepository.findByName(effectiveRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found: " + effectiveRole));

        User user = new User();
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setPassword(password);
        user.setRegistrationData(java.time.LocalDateTime.now());
        user.setRole(roleEntity);
        user.setSuspended(false);
        return userRepository.save(user);
    }

    @Transactional
    public User createAdminBySuperAdmin(String name, String surname, String email, String password, UserRole adminRole) {
        if (adminRole != UserRole.ADMIN && adminRole != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ADMIN or SUPER_ADMIN can be created by SuperAdmin");
        }
        if (userRepository.findUserByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        Role role = roleRepository.findByName(adminRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found"));
        User user = new User();
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setPassword(password);
        user.setRegistrationData(java.time.LocalDateTime.now());
        user.setRole(role);
        user.setSuspended(false);
        return userRepository.save(user);
    }

    @Transactional
    public User suspendUser(Long userID, boolean suspended) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setSuspended(suspended);
        return userRepository.save(user);
    }

    @Transactional
    public User changeUserRole(Long userID, UserRole newRole) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        user.setRole(role);
        return userRepository.save(user);
    }

    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long userID) {
        return userRepository.findById(userID);
    }
}
