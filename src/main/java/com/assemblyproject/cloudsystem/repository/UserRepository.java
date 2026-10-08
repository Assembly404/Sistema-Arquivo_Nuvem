package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByEmail(String username);

    @Query("select count(u) from Users u where u.suspended = false")
    long countBySuspendedFalse();

    @Query("select coalesce(sum(f.storedSizeBytes), 0) from FileMetadata f")
    long sumStoredSizeBytes();
}
