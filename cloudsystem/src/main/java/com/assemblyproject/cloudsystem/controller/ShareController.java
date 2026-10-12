package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.ShareCreateRequest;
import com.assemblyproject.cloudsystem.dto.request.ShareUpdateRequest;
import com.assemblyproject.cloudsystem.dto.response.FileMetadataResponse;
import com.assemblyproject.cloudsystem.dto.response.FolderResponse;
import com.assemblyproject.cloudsystem.dto.response.SharedItemResponse;
import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.Share;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.ShareService;
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
@RequestMapping("/shares")
public class ShareController {

    private final ShareService shareService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SharedItemResponse> create(@AuthenticationPrincipal JWTUserData principal,
                                                     @Valid @RequestBody ShareCreateRequest request) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        Share share = shareService.createOrUpdate(user, request.email(), request.fileId(), request.folderId(), request.permission());
        return ResponseEntity.ok(SharedItemResponse.from(share));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<SharedItemResponse>> sharedWithMe(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(shareService.sharedRootItems(user).stream()
                .map(SharedItemResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/shared/contents")
    public ResponseEntity<Map<String, Object>> sharedContents(@AuthenticationPrincipal JWTUserData principal,
                                                              @RequestParam(value = "folderId") Long folderId) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Folder folder = shareService.requireReadableFolder(user, folderId);
            List<FolderResponse> folders = shareService.childrenOfSharedFolder(user, folder).stream()
                    .map(FolderResponse::from)
                    .collect(Collectors.toList());
            List<FileMetadataResponse> files = shareService.filesOfSharedFolder(user, folder).stream()
                    .map(FileMetadataResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("folders", folders, "files", files));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/owned")
    public ResponseEntity<List<SharedItemResponse>> sharedByMe(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(shareService.listOwned(user).stream()
                .map(SharedItemResponse::from)
                .collect(Collectors.toList()));
    }

    @PatchMapping("/{shareId}")
    public ResponseEntity<SharedItemResponse> updatePermission(@AuthenticationPrincipal JWTUserData principal,
                                                               @PathVariable Long shareId,
                                                               @Valid @RequestBody ShareUpdateRequest request) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Share share = shareService.updatePermission(user, shareId, request.permission());
            return ResponseEntity.ok(SharedItemResponse.from(share));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<Map<String, Object>> revoke(@AuthenticationPrincipal JWTUserData principal,
                                                      @PathVariable Long shareId) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        try {
            shareService.revoke(user, shareId);
            return ResponseEntity.ok(Map.of("revoked", true, "shareID", shareId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}