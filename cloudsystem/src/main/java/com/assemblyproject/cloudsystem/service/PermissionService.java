package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.Role;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionService {

    public List<String> permissionsFor(Role role) {
        if (role == Role.ADMIN) {
            return List.of(
                    "ADMIN_ACESSAR",
                    "ADMIN_USUARIOS",
                    "ADMIN_PERMISSOES",
                    "ARQUIVO_VISUALIZAR",
                    "ARQUIVO_EXCLUIR",
                    "ARMAZENAMENTO_VISUALIZAR");
        }
        return List.of("ARQUIVO_VISUALIZAR", "ARMAZENAMENTO_VISUALIZAR");
    }

    public boolean hasPermission(Role role, String permission) {
        return permissionsFor(role).contains(permission);
    }
}
