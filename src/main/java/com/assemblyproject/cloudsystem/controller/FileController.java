package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.response.FileMetadataResponse;
import com.assemblyproject.cloudsystem.dto.response.UploadResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
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
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final ContainerService containerService;
    private final FileMetadataRepository fileMetadataRepository;
    private final ReplicationService replicationService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@AuthenticationPrincipal User user,
                                                     @RequestParam("file") MultipartFile file) throws IOException {
        Long fileID = containerService.uploadFile(user, file);
        replicationService.replicateFile(fileID);

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
                                               @PathVariable Long fileId) {
        // MVP simplificado - retorna bytes vazios; produção real leria do nó
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file\"")
                .body(new byte[0]);
    }
}