package com.assemblyproject.cloudsystem.configuration;

import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.UserRole;
import com.assemblyproject.cloudsystem.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataLoader {

    private final RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void init() {
        for (UserRole r : UserRole.values()) {
            roleRepository.findByName(r).orElseGet(() -> {
                Role role = new Role();
                role.setName(r);
                return roleRepository.save(role);
            });
        }
    }
}
