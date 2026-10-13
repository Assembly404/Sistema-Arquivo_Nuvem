package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.Papel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PapelRepository extends JpaRepository<Papel, Long> {

    Optional<Papel> findByCodigo(String codigo);

    Optional<Papel> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Papel> findAllByOrderByOrdemAscNomeAsc();
}
