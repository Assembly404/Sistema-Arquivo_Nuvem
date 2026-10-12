package com.assemblyproject.cloudsystem.entity;

/**
 * Tipos de evento notificável (RF35).
 */
public enum NotificationType {
    SHARE_RECEIVED,
    SHARE_REVOKED,
    NODE_OFFLINE,
    NODE_ONLINE,
    REPLICA_FAILED,
    QUOTA_WARNING,
    SYSTEM
}
