package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.request.RecursoRequest;
import com.assemblyproject.cloudsystem.dto.response.RecursoResponse;
import com.assemblyproject.cloudsystem.entity.Recurso;
import com.assemblyproject.cloudsystem.repository.PermissaoRepository;
import com.assemblyproject.cloudsystem.repository.RecursoRepository;
import com.assemblyproject.cloudsystem.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Catálogo de recursos do sistema ("o que o sistema oferece").
 * Restrito a perfis com ADMIN_PERMISSOES pelo SecurityConfig.
 */
@RestController
@RequestMapping("/admin/recursos")
public class AdminRecursoController {

    private final RecursoRepository recursoRepository;
    private final PermissaoRepository permissaoRepository;
    private final PermissionService permissionService;

    public AdminRecursoController(RecursoRepository recursoRepository,
                                  PermissaoRepository permissaoRepository,
                                  PermissionService permissionService) {
        this.recursoRepository = recursoRepository;
        this.permissaoRepository = permissaoRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<RecursoResponse>> list() {
        return ResponseEntity.ok(permissionService.recursosComPermissoes());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RecursoRequest request) {
        if (recursoRepository.existsByCodigoIgnoreCase(request.codigo())) {
            return ResponseEntity.status(409).body(Map.of("message", "Já existe um recurso com esse código."));
        }
        Recurso recurso = new Recurso();
        recurso.setCodigo(request.codigo().trim().toUpperCase());
        recurso.setNome(request.nome());
        recurso.setDescricao(request.descricao());
        recurso.setIcone(request.icone());
        recurso.setOrdem(request.ordem() != null ? request.ordem() : 99);
        recurso.setSistema(false);
        recursoRepository.save(recurso);
        return ResponseEntity.status(201).body(permissionService.toRecursoComPermissoes(recurso));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody RecursoRequest request) {
        Recurso recurso = recursoRepository.findById(id).orElse(null);
        if (recurso == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Recurso não encontrado."));
        }
        String newCode = request.codigo().trim().toUpperCase();
        if (!newCode.equalsIgnoreCase(recurso.getCodigo()) && recursoRepository.existsByCodigoIgnoreCase(newCode)) {
            return ResponseEntity.status(409).body(Map.of("message", "Já existe um recurso com esse código."));
        }
        recurso.setCodigo(newCode);
        recurso.setNome(request.nome());
        recurso.setDescricao(request.descricao());
        recurso.setIcone(request.icone());
        if (request.ordem() != null) {
            recurso.setOrdem(request.ordem());
        }
        recursoRepository.save(recurso);
        return ResponseEntity.ok(permissionService.toRecursoComPermissoes(recurso));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Recurso recurso = recursoRepository.findById(id).orElse(null);
        if (recurso == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Recurso não encontrado."));
        }
        if (recurso.isSistema()) {
            return ResponseEntity.status(409).body(Map.of("message", "Este recurso é de sistema e não pode ser eliminado."));
        }
        if (permissaoRepository.countByRecurso_Id(id) > 0) {
            return ResponseEntity.status(409).body(Map.of("message", "Elimine primeiro as permissões deste recurso."));
        }
        recursoRepository.delete(recurso);
        return ResponseEntity.noContent().build();
    }
}