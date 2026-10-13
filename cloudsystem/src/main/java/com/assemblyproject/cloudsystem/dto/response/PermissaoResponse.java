package com.assemblyproject.cloudsystem.dto.response;

import java.util.List;

public record PermissaoResponse(
        Long id,
        String codigo,
        String nome,
        String descricao,
        Integer ordem,
        boolean sistema,
        Long recursoId,
        String recursoCodigo,
        String recursoNome
) {
}
