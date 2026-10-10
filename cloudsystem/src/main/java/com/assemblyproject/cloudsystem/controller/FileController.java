package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.response.FileMetadataResponse;
import com.assemblyproject.cloudsystem.dto.response.UploadResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileReplica;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.ContainerClient;
import com.assemblyproject.cloudsystem.service.ContainerService;
import com.assemblyproject.cloudsystem.service.ReplicationService;
import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final ContainerService containerService;
    private final ContainerClient containerClient;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final ReplicationService replicationService;
    private final StorageNodeRepository nodeRepository;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@AuthenticationPrincipal JWTUserData principal,
                                                     @RequestParam("file") MultipartFile file) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ContainerService.StoredContainer stored = containerService.uploadFile(user, file);
        replicationService.replicateFile(stored.fileID(), user, stored.containerData());

        UploadResponse response = new UploadResponse(
                stored.fileID(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listUserFiles(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<FileMetadataResponse> files = fileMetadataRepository.findAllByOwner(user).stream()
                .map(FileMetadataResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@AuthenticationPrincipal JWTUserData principal,
                                               @PathVariable Long fileId) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
        if (metadata == null) {
            return ResponseEntity.status(404).build();
        }
        if (!metadata.getOwner().getUserID().equals(user.getUserID())) {
            return ResponseEntity.status(403).build();
        }

        StorageNode node = pickOnlineNodeWithReplica(metadata);
        if (node == null) {
            return ResponseEntity.status(503).build();
        }

        byte[] containerData = containerClient.downloadReplica(node, fileId);
        byte[] fileKey = containerService.resolveFileKey(metadata);
        byte[] fileData = containerService.downloadFromNode(containerData, fileKey);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .body(fileData);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@AuthenticationPrincipal JWTUserData principal,
                                                          @PathVariable Long fileId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
        if (metadata == null) {
            return ResponseEntity.status(404).build();
        }
        if (!metadata.getOwner().getUserID().equals(user.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        replicationService.deleteReplicas(fileId);
        fileMetadataRepository.delete(metadata);
        return ResponseEntity.ok(Map.of("deleted", true, "fileID", fileId));
    }

    private StorageNode pickOnlineNodeWithReplica(FileMetadata metadata) {
        List<FileReplica> replicas = fileReplicaRepository.findAllByFileAndStatus(metadata, ReplicaStatus.SYNCED);
        for (FileReplica replica : replicas) {
            StorageNode node = replica.getNode();
            if (node != null && node.getStatus() == NodeStatus.ONLINE) {
                return node;
            }
        }
        return null;
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
