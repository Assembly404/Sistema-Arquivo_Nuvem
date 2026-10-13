package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.repository.PapelRepository;
import com.assemblyproject.cloudsystem.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/permissions")
public class AdminPermissionsController {

    private final PapelRepository papelRepository;
    private final PermissionService permissionService;

    /** Matriz papel → permissões, derivada da base de dados (compatibilidade). */
    @GetMapping
    public ResponseEntity<Map<String, List<String>>> matrix() {
        Map<String, List<String>> matrix = new LinkedHashMap<>();
        papelRepository.findAllByOrderByOrdemAscNomeAsc()
                .forEach(papel -> matrix.put(papel.getCodigo(), permissionService.permissionsFor(papel.getCodigo())));
        return ResponseEntity.ok(matrix);
    }
}