package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    Optional<Recurso> findByCodigo(String codigo);

    Optional<Recurso> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Recurso> findAllByOrderByOrdemAscNomeAsc();
}
