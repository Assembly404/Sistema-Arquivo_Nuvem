package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<UserDetails> findUserByEmail(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findFirstByRole(String role);

    long countByRole(String role);

    @Query("select count(u) from Users u where u.suspended = false")
    long countBySuspendedFalse();

    /** Pesquisa por nome, apelido ou email (termo já normalizado com % ... %). */
    @Query("""
            select u from Users u
            where lower(u.name) like :term
               or lower(u.surname) like :term
               or lower(u.email) like :term
            order by u.name asc, u.surname asc
            """)
    List<User> searchByTerm(@Param("term") String term);
}
