package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recurso do sistema (ex.: Arquivos, Armazenamento, Administração).
 * Representa "o que o sistema oferece" e agrupa permissões dentro de cada área.
 */
@Entity
@Table(name = "recursos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    /** Nome do ícone (react-icons/feather) sugerido para a UI. */
    @Column(length = 60)
    private String icone;

    @Column(nullable = false)
    private Integer ordem = 0;

    /** Recursos de sistema não podem ser eliminados pela UI. */
    @Column(nullable = false)
    private boolean sistema;
}
