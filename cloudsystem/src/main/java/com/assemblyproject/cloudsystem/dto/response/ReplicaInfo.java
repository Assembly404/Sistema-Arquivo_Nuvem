package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import lombok.Data;

@Data
public class ReplicaInfo {
    private Long nodeID;
    private String nodeName;
    private String nodeType;
    private Boolean online;
    private ReplicaStatus status;

    public ReplicaInfo(Long nodeID, String nodeName, ReplicaStatus status) {
        this(nodeID, nodeName, null, null, status);
    }

    public ReplicaInfo(Long nodeID, String nodeName, String nodeType, Boolean online, ReplicaStatus status) {
        this.nodeID = nodeID;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.online = online;
        this.status = status;
    }
}
