package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.response.FileMetadataResponse;
import com.assemblyproject.cloudsystem.dto.response.UploadResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.service.ContainerClient;
import com.assemblyproject.cloudsystem.service.ContainerService;
import com.assemblyproject.cloudsystem.service.ReplicationService;
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
    private final ReplicationService replicationService;
    private final StorageNodeRepository nodeRepository;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@AuthenticationPrincipal User user,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        byte[] containerData = containerService.buildContainer(file.getBytes(), file.getOriginalFilename(), file.getContentType(), file.getSize());
        Long fileID = containerService.uploadFile(user, file);
        replicationService.replicateFile(fileID, user, containerData);

        UploadResponse response = new UploadResponse(
                fileID,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listUserFiles(@AuthenticationPrincipal User user) {
        List<FileMetadataResponse> files = fileMetadataRepository.findAllByOwner(user).stream()
                .map(FileMetadataResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@AuthenticationPrincipal User user,
                                               @PathVariable Long fileId) throws IOException {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (!metadata.getOwner().getUserID().equals(user.getUserID())) {
            return ResponseEntity.status(403).build();
        }

        List<StorageNode> nodes = nodeRepository.findAllByStatus(com.assemblyproject.cloudsystem.entity.NodeStatus.ONLINE);
        if (nodes.isEmpty()) {
            return ResponseEntity.status(503).body(new byte[0]);
        }

        StorageNode node = nodes.get(0);
        byte[] containerData = containerClient.downloadReplica(node, fileId);

        String encryptedKey = metadata.getEncryptedDataKey();
        byte[] masterKey = java.util.Base64.getDecoder().decode("EvguenyEwenAllenEdmundoMalik".getBytes());
        byte[] fileKey = containerService.decryptKey(encryptedKey, masterKey);

        byte[] fileData = containerService.downloadFromNode(containerData, fileKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .body(fileData);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@AuthenticationPrincipal User user,
                                                          @PathVariable Long fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (!metadata.getOwner().getUserID().equals(user.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        replicationService.deleteReplicas(fileId);
        fileMetadataRepository.delete(metadata);
        return ResponseEntity.ok(Map.of("deleted", true, "fileID", fileId));
    }
}
