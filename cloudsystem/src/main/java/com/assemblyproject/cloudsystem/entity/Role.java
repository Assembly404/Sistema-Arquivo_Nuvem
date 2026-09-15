package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity(name = "role_system")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRole;

    @Column(nullable = false, unique = true, length = 50)
    private String role;

    @Column(nullable = false)
    private String description;

    @OneToMany(mappedBy = "role")
    private List<User> users;
}
