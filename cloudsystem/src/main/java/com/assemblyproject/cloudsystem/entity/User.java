package com.assemblyproject.cloudsystem.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userID;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(nullable = false, length = 100)
    private String email;

    @NotNull
    private String password;

    @NotNull
    private LocalDateTime registrationData;

    private LocalDateTime lastAccess;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    private boolean suspended;

    /**
     * Cota de armazenamento na nuvem do sistema, em bytes. {@code null} = ilimitado.
     */
    private Long quotaBytes;

    /**
     * Política padrão de armazenamento: SYSTEM_FIRST, DEVICE_FIRST ou MANUAL.
     */
    private String storagePolicy;

    /**
     * Comportamento quando um dispositivo pessoal que guarda o ficheiro está
     * offline (RF24): DENY (negar) ou CACHE (servir cópia em cache).
     */
    @Column(length = 20)
    private String offlineBehavior;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role effective = role != null ? role : Role.USER;
        return List.of(new SimpleGrantedAuthority("ROLE_" + effective.name()));
    }

    @Override
    public String getUsername() {
        return email != null ? email : "";
    }
}
