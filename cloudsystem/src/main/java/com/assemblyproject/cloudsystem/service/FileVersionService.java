package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileVersion;
import com.assemblyproject.cloudsystem.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileVersionService {

    private final FileVersionRepository versionRepository;
    private final ContainerConfig config;

    /**
     * Guarda o container atual do ficheiro como uma versao historica.
     */
    @Transactional
    public FileVersion snapshot(FileMetadata file, byte[] currentContainerData) throws IOException {
        int next = (int) versionRepository.countByFile(file) + 1;
        Path dir = Paths.get(config.getStorageDir(), "versions", String.valueOf(file.getFileID()));
        Files.createDirectories(dir);
        Path target = dir.resolve("v" + next + config.getExtension());
        Files.write(target, currentContainerData);

        FileVersion version = FileVersion.builder()
                .file(file)
                .versionNumber(next)
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .originalSizeBytes(file.getOriginalSizeBytes())
                .storedSizeBytes(file.getStoredSizeBytes())
                .checksumSha256(file.getChecksumSha256())
                .encryptedDataKey(file.getEncryptedDataKey())
                .keyEncryptionIv(file.getKeyEncryptionIv())
                .localPath(target.toString())
                .createdAt(LocalDateTime.now())
                .build();
        return versionRepository.save(version);
    }

    public List<FileVersion> list(FileMetadata file) {
        return versionRepository.findAllByFileOrderByVersionNumberDesc(file);
    }

    public Optional<FileVersion> find(FileMetadata file, Integer versionNumber) {
        return versionRepository.findByFileAndVersionNumber(file, versionNumber);
    }

    public byte[] readContainer(FileVersion version) throws IOException {
        if (version.getLocalPath() == null) {
            throw new IOException("Versao sem copia local disponivel");
        }
        return Files.readAllBytes(Paths.get(version.getLocalPath()));
    }

    @Transactional
    public void deleteAllForFile(FileMetadata file) {
        List<FileVersion> versions = versionRepository.findAllByFileOrderByVersionNumberDesc(file);
        for (FileVersion version : versions) {
            if (version.getLocalPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(version.getLocalPath()));
                } catch (IOException ignored) {
                    // copia local ja inexistente
                }
            }
        }
        versionRepository.deleteAll(versions);
    }
}
