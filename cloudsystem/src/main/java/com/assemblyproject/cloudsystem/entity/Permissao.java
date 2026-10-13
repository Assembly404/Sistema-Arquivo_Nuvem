package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permissão concreta do sistema (ex.: ARQUIVO_EXCLUIR) que pertence a um {@link Recurso}.
 * É a unidade atribuível a um {@link Papel}.
 */
@Entity
@Table(name = "permissoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recurso_id", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private Integer ordem = 0;

    /** Permissões de sistema não podem ser eliminadas pela UI. */
    @Column(nullable = false)
    private boolean sistema;
}
