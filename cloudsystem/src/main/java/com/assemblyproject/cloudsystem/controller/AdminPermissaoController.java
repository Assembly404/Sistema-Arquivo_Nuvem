package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.request.PermissaoRequest;
import com.assemblyproject.cloudsystem.dto.response.PermissaoResponse;
import com.assemblyproject.cloudsystem.entity.Papel;
import com.assemblyproject.cloudsystem.entity.Permissao;
import com.assemblyproject.cloudsystem.entity.Recurso;
import com.assemblyproject.cloudsystem.repository.PapelRepository;
import com.assemblyproject.cloudsystem.repository.PermissaoRepository;
import com.assemblyproject.cloudsystem.repository.RecursoRepository;
import com.assemblyproject.cloudsystem.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Catálogo de permissões do sistema. Cada permissão pertence a um {@link Recurso}.
 * Restrito a perfis com ADMIN_PERMISSOES pelo SecurityConfig.
 */
@RestController
@RequestMapping("/admin/permissoes")
public class AdminPermissaoController {

    private final PermissaoRepository permissaoRepository;
    private final RecursoRepository recursoRepository;
    private final PapelRepository papelRepository;
    private final PermissionService permissionService;

    public AdminPermissaoController(PermissaoRepository permissaoRepository,
                                    RecursoRepository recursoRepository,
                                    PapelRepository papelRepository,
                                    PermissionService permissionService) {
        this.permissaoRepository = permissaoRepository;
        this.recursoRepository = recursoRepository;
        this.papelRepository = papelRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<PermissaoResponse>> list() {
        return ResponseEntity.ok(permissaoRepository
                .findAllByOrderByRecurso_OrdemAscOrdemAscNomeAsc()
                .stream()
                .map(permissionService::toPermissao)
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PermissaoRequest request) {
        if (permissaoRepository.existsByCodigoIgnoreCase(request.codigo())) {
            return ResponseEntity.status(409).body(Map.of("message", "Já existe uma permissão com esse código."));
        }
        Recurso recurso = recursoRepository.findById(request.recursoId()).orElse(null);
        if (recurso == null) {
            return ResponseEntity.status(400).body(Map.of("message", "O recurso indicado não existe."));
        }
        Permissao permissao = new Permissao();
        permissao.setCodigo(request.codigo().trim().toUpperCase());
        permissao.setNome(request.nome());
        permissao.setDescricao(request.descricao());
        permissao.setRecurso(recurso);
        permissao.setOrdem(request.ordem() != null ? request.ordem() : 99);
        permissao.setSistema(false);
        permissaoRepository.save(permissao);
        return ResponseEntity.status(201).body(permissionService.toPermissao(permissao));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PermissaoRequest request) {
        Permissao permissao = permissaoRepository.findById(id).orElse(null);
        if (permissao == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Permissão não encontrada."));
        }
        String newCode = request.codigo().trim().toUpperCase();
        if (!newCode.equalsIgnoreCase(permissao.getCodigo()) && permissaoRepository.existsByCodigoIgnoreCase(newCode)) {
            return ResponseEntity.status(409).body(Map.of("message", "Já existe uma permissão com esse código."));
        }
        Recurso recurso = recursoRepository.findById(request.recursoId()).orElse(null);
        if (recurso == null) {
            return ResponseEntity.status(400).body(Map.of("message", "O recurso indicado não existe."));
        }
        permissao.setCodigo(newCode);
        permissao.setNome(request.nome());
        permissao.setDescricao(request.descricao());
        permissao.setRecurso(recurso);
        if (request.ordem() != null) {
            permissao.setOrdem(request.ordem());
        }
        permissaoRepository.save(permissao);
        return ResponseEntity.ok(permissionService.toPermissao(permissao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Permissao permissao = permissaoRepository.findById(id).orElse(null);
        if (permissao == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Permissão não encontrada."));
        }
        if (permissao.isSistema()) {
            return ResponseEntity.status(409).body(Map.of("message", "Esta permissão é de sistema e não pode ser eliminada."));
        }
        /* remover da tabela associativa de todos os perfis antes de apagar */
        for (Papel papel : papelRepository.findAll()) {
            if (papel.getPermissoes().removeIf(p -> p.getId().equals(id))) {
                papelRepository.save(papel);
            }
        }
        permissaoRepository.delete(permissao);
        return ResponseEntity.noContent().build();
    }
}