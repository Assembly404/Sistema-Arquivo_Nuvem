package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/permissions")
public class AdminPermissionsController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<Map<String, List<String>>> matrix() {
        Map<String, List<String>> matrix = new LinkedHashMap<>();
        Arrays.asList(Role.values()).forEach(role -> matrix.put(role.name(), permissionService.permissionsFor(role)));
        return ResponseEntity.ok(matrix);
    }
}