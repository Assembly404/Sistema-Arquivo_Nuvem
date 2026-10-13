package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

    Optional<Permissao> findByCodigo(String codigo);

    Optional<Permissao> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    Set<Permissao> findAllByCodigoIn(Collection<String> codigos);

    List<Permissao> findAllByOrderByRecurso_OrdemAscOrdemAscNomeAsc();

    long countByRecurso_Id(Long recursoId);
}
