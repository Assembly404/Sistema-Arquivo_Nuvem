package com.assemblyproject.cloudsystem.dto.response;

import java.util.List;

public record PapelResponse(
        Long id,
        String codigo,
        String nome,
        String descricao,
        Integer ordem,
        boolean sistema,
        long utilizadores,
        int totalPermissoes,
        List<String> permissoes
) {
}
