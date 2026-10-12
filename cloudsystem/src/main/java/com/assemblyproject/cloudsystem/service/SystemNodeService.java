package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeType;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Garante a existência de um nó de sistema único que representa a nuvem oficial
 * (RF18). Este nó é interno, não pertence a nenhum utilizador e está sempre
 * disponível: os ficheiros ficam guardados no disco do próprio servidor, o que
 * elimina o cenário de upload "com sucesso" mas sem qualquer réplica.
 */
@Service
@RequiredArgsConstructor
public class SystemNodeService {

    private static final Logger log = LoggerFactory.getLogger(SystemNodeService.class);

    private final StorageNodeRepository nodeRepository;
    private final StorageNodeService nodeStorageService;

    @Value("${cloud.node.system-node-name:Nuvem do sistema}")
    private String systemNodeName;

    @Value("${cloud.node.system-quota-bytes:0}")
    private long systemQuotaBytes;

    @Value("${cloud.node.server-port:8081}")
    private int serverPort;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureSystemNodeOnStartup() {
        backfillLegacyNodes();
        getOrCreateSystemNode();
    }

    /**
     * Preenche valores por omissão em nós criados antes de existir a noção de
     * tipo/estado ativo, para evitar colunas nulas após a migração automática.
     */
    private void backfillLegacyNodes() {
        for (StorageNode node : nodeRepository.findAll()) {
            boolean changed = false;
            if (node.getType() == null) {
                node.setType(NodeType.DEVICE);
                changed = true;
            }
            if (node.getEnabled() == null) {
                node.setEnabled(true);
                changed = true;
            }
            if (changed) {
                nodeRepository.save(node);
            }
        }
    }

    @Transactional
    public StorageNode getOrCreateSystemNode() {
        StorageNode node = nodeRepository.findFirstByType(NodeType.SYSTEM).orElse(null);
        if (node == null) {
            node = StorageNode.builder()
                    .owner(null)
                    .name(systemNodeName)
                    .type(NodeType.SYSTEM)
                    .status(NodeStatus.ONLINE)
                    .totalSpaceBytes(systemQuotaBytes)
                    .usedSpaceBytes(0L)
                    .visibility(NodeVisibility.PUBLIC)
                    .sessionId("127.0.0.1:" + serverPort)
                    .enabled(true)
                    .lastHeartbeat(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            node = nodeRepository.save(node);
            log.info("Nó de sistema criado com id {} (nuvem oficial).", node.getNodeID());
        } else {
            node.setStatus(NodeStatus.ONLINE);
            node.setEnabled(true);
            node.setType(NodeType.SYSTEM);
            node.setLastHeartbeat(LocalDateTime.now());
            node = nodeRepository.save(node);
        }
        return node;
    }

    public boolean isSystemQuotaUnlimited() {
        return systemQuotaBytes <= 0;
    }

    /**
     * Recalcula o espaço usado pelo nó de sistema a partir do disco.
     */
    @Transactional
    public StorageNode refreshSystemUsage() {
        StorageNode node = getOrCreateSystemNode();
        try {
            node.setUsedSpaceBytes(nodeStorageService.getStoredBytes(node));
        } catch (Exception e) {
            log.warn("Falha ao recalcular o uso do nó de sistema: {}", e.getMessage());
        }
        return nodeRepository.save(node);
    }
}
