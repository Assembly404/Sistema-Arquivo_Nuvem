package com.assemblyproject.cloudsystem.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido",
                        (a, b) -> a));
        return ResponseEntity.badRequest().body(Map.of("message", "Dados inválidos.", "errors", fields));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Pedido inválido."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Estado inválido."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Credenciais inválidas."));
    }

    @ExceptionHandler(com.assemblyproject.cloudsystem.service.PublicLinkService.InvalidLinkPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidLinkPassword(
            com.assemblyproject.cloudsystem.service.PublicLinkService.InvalidLinkPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage(), "passwordRequired", "true"));
    }

    @ExceptionHandler(com.assemblyproject.cloudsystem.service.NoStorageAvailableException.class)
    public ResponseEntity<Map<String, String>> handleNoStorage(
            com.assemblyproject.cloudsystem.service.NoStorageAvailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage(), "storageUnavailable", "true"));
    }

    @ExceptionHandler(com.assemblyproject.cloudsystem.service.DeviceOfflineException.class)
    public ResponseEntity<Map<String, String>> handleDeviceOffline(
            com.assemblyproject.cloudsystem.service.DeviceOfflineException ex) {
        Map<String, String> body = new java.util.HashMap<>();
        body.put("message", ex.getMessage());
        body.put("deviceOffline", "true");
        if (ex.getNodeID() != null) {
            body.put("nodeID", ex.getNodeID().toString());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(com.assemblyproject.cloudsystem.service.InvalidPairingException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPairing(
            com.assemblyproject.cloudsystem.service.InvalidPairingException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Sem permissão."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erro interno do servidor."));
    }
}
