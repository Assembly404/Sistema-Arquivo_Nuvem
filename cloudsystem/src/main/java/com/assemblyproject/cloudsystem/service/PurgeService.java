package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.PublicLink;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import com.assemblyproject.cloudsystem.repository.PublicLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Eliminação definitiva de ficheiros e pastas com limpeza completa dos recursos
 * associados: réplicas nos nós, versões locais e links públicos. Centraliza a
 * lógica usada pelo purge manual, pela expiração da lixeira (RF11) e pela
 * remoção de pastas, evitando dados órfãos.
 */
@Service
@RequiredArgsConstructor
public class PurgeService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final PublicLinkRepository publicLinkRepository;
    private final FileVersionService versionService;
    private final ReplicationService replicationService;

    @Transactional
    public void purgeFile(FileMetadata file) {
        if (file == null) {
            return;
        }
        for (PublicLink link : publicLinkRepository.findAllByFile(file)) {
            link.setRevoked(true);
            publicLinkRepository.save(link);
        }
        try {
            versionService.deleteAllForFile(file);
        } catch (RuntimeException ignored) {
            // versões já inexistentes
        }
        try {
            replicationService.deleteReplicas(file.getFileID());
        } catch (RuntimeException ignored) {
            // réplicas já inexistentes ou nós inacessíveis
        }
        fileMetadataRepository.delete(file);
    }

    /**
     * Elimina definitivamente uma pasta e todo o seu conteúdo. Os descendentes
     * são processados antes da pasta para respeitar as chaves estrangeiras.
     */
    @Transactional
    public void purgeFolder(Folder folder) {
        if (folder == null) {
            return;
        }
        for (FileMetadata file : new ArrayList<>(fileMetadataRepository.findAllByFolder(folder))) {
            purgeFile(file);
        }
        for (Folder child : new ArrayList<>(folderRepository.findAllByOwnerAndParent(folder.getOwner(), folder))) {
            purgeFolder(child);
        }
        for (PublicLink link : publicLinkRepository.findAllByFolder(folder)) {
            link.setRevoked(true);
            publicLinkRepository.save(link);
        }
        folderRepository.delete(folder);
    }

    /**
     * Elimina apenas um ficheiro (sem lançar exceções de IO) para uso em lotes.
     */
    @Transactional
    public void purgeFileQuietly(FileMetadata file) {
        try {
            purgeFile(file);
        } catch (RuntimeException ignored) {
            // já removido
        }
    }
}
