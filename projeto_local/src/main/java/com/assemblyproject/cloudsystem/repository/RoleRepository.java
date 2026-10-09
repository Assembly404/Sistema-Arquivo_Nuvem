package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.UserRole;
import com.assemblyproject.cloudsystem.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
}
