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
import java.util.ArrayList;
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

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @NotNull
    private String password;

    @NotNull
    private LocalDateTime registrationData;

    private LocalDateTime lastAccess;

    /**
     * Código do {@link Papel}/perfil (ex.: USER, ADMIN). Guardado como texto
     * para permitir perfis dinâmicos definidos em base de dados.
     */
    @Column(nullable = false, length = 40)
    private String role = "USER";

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

    // Relações com cascade para limpeza ao apagar conta (RF06)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileMetadata> files = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Folder> folders = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Share> shares = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String effective = role != null && !role.isBlank() ? role : "USER";
        return List.of(new SimpleGrantedAuthority("ROLE_" + effective));
    }

    @Override
    public String getUsername() {
        return email != null ? email : "";
    }
}
