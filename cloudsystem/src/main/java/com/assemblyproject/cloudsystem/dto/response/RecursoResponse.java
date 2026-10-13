package com.assemblyproject.cloudsystem.dto.response;

import java.util.List;

public record RecursoResponse(
        Long id,
        String codigo,
        String nome,
        String descricao,
        String icone,
        Integer ordem,
        boolean sistema,
        List<PermissaoResponse> permissoes
) {
}
