package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.entity.UserRole;
import com.assemblyproject.cloudsystem.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal JWTUserData user) {
        if (user == null || user.role() == null || !user.role().equals(UserRole.SUPER_ADMIN.name())) {
            return ResponseEntity.status(403).body("Only SUPER_ADMIN can access system dashboard");
        }
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/storage-by-node")
    public ResponseEntity<?> storageByNode(@AuthenticationPrincipal JWTUserData user) {
        if (user == null || user.role() == null || !user.role().equals(UserRole.SUPER_ADMIN.name())) {
            return ResponseEntity.status(403).body("Only SUPER_ADMIN");
        }
        return ResponseEntity.ok(dashboardService.getStorageByNode());
    }

    @GetMapping("/replicas-by-node")
    public ResponseEntity<?> replicasByNode(@AuthenticationPrincipal JWTUserData user) {
        if (user == null || user.role() == null || !user.role().equals(UserRole.SUPER_ADMIN.name())) {
            return ResponseEntity.status(403).body("Only SUPER_ADMIN");
        }
        return ResponseEntity.ok(dashboardService.getReplicasByNode());
    }
}
