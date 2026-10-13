package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Papel/perfil de acesso (ex.: ADMIN, USER). Agrega um conjunto de
 * {@link Permissao} através da tabela associativa {@code papel_permissao}.
 */
@Entity
@Table(name = "papeis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Papel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código estável gravado em {@code users.role} e no JWT. */
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    private Integer ordem = 0;

    /** Papéis de sistema não podem ser eliminados nem ter o código alterado. */
    @Column(nullable = false)
    private boolean sistema;

    /** Tabela associativa perfil ↔ permissão. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "papel_permissao",
            joinColumns = @JoinColumn(name = "papel_id"),
            inverseJoinColumns = @JoinColumn(name = "permissao_id")
    )
    private Set<Permissao> permissoes = new HashSet<>();
}
