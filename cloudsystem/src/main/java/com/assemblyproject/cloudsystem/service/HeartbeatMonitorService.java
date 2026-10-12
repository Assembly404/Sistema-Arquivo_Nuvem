package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NotificationType;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HeartbeatMonitorService {

    private final StorageNodeRepository nodeRepository;
    private final ContainerClient containerClient;
    private final NotificationService notificationService;

    @Value("${cloud.node.heartbeat-timeout-seconds:90}")
    private int timeoutSeconds;

    @Scheduled(fixedRateString = "${cloud.node.heartbeat-interval-ms:30000}")
    @Transactional
    public void checkAllNodes() {
        List<StorageNode> nodes = nodeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (StorageNode node : nodes) {
            boolean online = containerClient.isNodeOnline(node);
            NodeStatus previous = node.getStatus();

            if (online) {
                node.setStatus(NodeStatus.ONLINE);
                node.setLastHeartbeat(now);
            } else {
                LocalDateTime lastHB = node.getLastHeartbeat();
                if (lastHB != null && lastHB.plusSeconds(timeoutSeconds).isBefore(now)) {
                    node.setStatus(NodeStatus.OFFLINE);
                } else if (node.getStatus() == NodeStatus.ONLINE) {
                    node.setStatus(NodeStatus.DEGRADED);
                }
            }

            if (node.getStatus() == NodeStatus.OFFLINE && previous != NodeStatus.OFFLINE) {
                notificationService.notify(node.getOwner(), NotificationType.NODE_OFFLINE,
                        "Dispositivo offline",
                        "O dispositivo \"" + node.getName() + "\" ficou offline. Os ficheiros nele guardados podem ficar indisponíveis.",
                        "/dashboard/armazenamento");
            }

            nodeRepository.save(node);
        }
    }

    public List<StorageNode> getOfflineNodes() {
        return nodeRepository.findAllByStatus(NodeStatus.OFFLINE);
    }

    public List<StorageNode> getDegradedNodes() {
        return nodeRepository.findAllByStatus(NodeStatus.DEGRADED);
    }
}
