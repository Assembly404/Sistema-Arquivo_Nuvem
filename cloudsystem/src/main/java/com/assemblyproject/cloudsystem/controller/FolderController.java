package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.FolderCreateRequest;
import com.assemblyproject.cloudsystem.dto.request.FolderUpdateRequest;
import com.assemblyproject.cloudsystem.dto.response.FolderResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@AuthenticationPrincipal JWTUserData principal,
                                                       @Valid @RequestBody FolderCreateRequest request) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FolderResponse.from(
                folderService.create(user, request.parentId(), request.name())));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> listFolders(@AuthenticationPrincipal JWTUserData principal,
                                                            @RequestParam(value = "parentId", required = false) Long parentId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(folderService.listChildren(user, parentId).stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FolderResponse>> listTrash(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(folderService.listTrash(user).stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList()));
    }

    @PatchMapping("/{folderId}")
    public ResponseEntity<FolderResponse> updateFolder(@AuthenticationPrincipal JWTUserData principal,
                                                       @PathVariable Long folderId,
                                                       @RequestBody FolderUpdateRequest request) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FolderResponse.from(
                folderService.update(user, folderId, request.name(), request.parentId())));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> deleteFolder(@AuthenticationPrincipal JWTUserData principal,
                                                            @PathVariable Long folderId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        folderService.softDelete(user, folderId);
        return ResponseEntity.ok(Map.of("trashed", true, "folderID", folderId));
    }

    @PostMapping("/{folderId}/restore")
    public ResponseEntity<FolderResponse> restoreFolder(@AuthenticationPrincipal JWTUserData principal,
                                                        @PathVariable Long folderId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FolderResponse.from(folderService.restore(user, folderId)));
    }

    @DeleteMapping("/{folderId}/purge")
    public ResponseEntity<Map<String, Object>> purgeFolder(@AuthenticationPrincipal JWTUserData principal,
                                                           @PathVariable Long folderId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        folderService.purge(user, folderId);
        return ResponseEntity.ok(Map.of("purged", true, "folderID", folderId));
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
