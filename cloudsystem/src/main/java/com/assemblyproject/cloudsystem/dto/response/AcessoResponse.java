package com.assemblyproject.cloudsystem.dto.response;

import java.util.List;

/**
 * Vista de "o que o meu perfil pode fazer": recursos do sistema com as
 * permissões que o papel do utilizador autenticado possui.
 */
public record AcessoResponse(
        String role,
        String roleNome,
        List<RecursoAcesso> recursos
) {
    public record RecursoAcesso(
            String codigo,
            String nome,
            String descricao,
            String icone,
            List<PermissaoAcesso> permissoes
    ) {
    }

    public record PermissaoAcesso(
            String codigo,
            String nome,
            String descricao
    ) {
    }
}
