package com.assemblyproject.cloudsystem.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Pedido de registo de um dispositivo pessoal (RF17). Pode ser feito por um
 * utilizador autenticado ou por um agente local que apresente um código de
 * pareamento válido (RF17.4).
 */
public record RegisterNodeRequest(
        @NotBlank(message = "O nome do nó é obrigatório")
        String name,
        Long totalSpaceBytes,
        String visibility,
        String address,
        String localPath,
        String operatingSystem,
        String agentVersion,
        Long reportedFreeBytes,
        Double reportedFreeGb,
        String pairingCode
) {
}
