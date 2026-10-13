package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.dto.response.AcessoResponse;
import com.assemblyproject.cloudsystem.dto.response.PapelResponse;
import com.assemblyproject.cloudsystem.dto.response.PermissaoResponse;
import com.assemblyproject.cloudsystem.dto.response.RecursoResponse;
import com.assemblyproject.cloudsystem.entity.Papel;
import com.assemblyproject.cloudsystem.entity.Permissao;
import com.assemblyproject.cloudsystem.entity.Recurso;
import com.assemblyproject.cloudsystem.repository.PapelRepository;
import com.assemblyproject.cloudsystem.repository.PermissaoRepository;
import com.assemblyproject.cloudsystem.repository.RecursoRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fonte de verdade das permissões. A matriz papel → permissão vive na base de
 * dados (tabelas {@code recursos}, {@code permissoes}, {@code papeis} e
 * {@code papel_permissao}) em vez de estar codificada em Java.
 */
@Service
public class PermissionService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private final RecursoRepository recursoRepository;
    private final PermissaoRepository permissaoRepository;
    private final PapelRepository papelRepository;
    private final UserRepository userRepository;

    public PermissionService(RecursoRepository recursoRepository,
                             PermissaoRepository permissaoRepository,
                             PapelRepository papelRepository,
                             UserRepository userRepository) {
        this.recursoRepository = recursoRepository;
        this.permissaoRepository = permissaoRepository;
        this.papelRepository = papelRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // Seeding — cria o catálogo inicial na primeira execução
    // ------------------------------------------------------------------

    @PostConstruct
    void seed() {
        Recurso arquivos = ensureRecurso("ARQUIVOS", "Arquivos",
                "Gestão de ficheiros, pastas e partilhas.", "FiFolder", 1);
        Recurso armazenamento = ensureRecurso("ARMAZENAMENTO", "Armazenamento",
                "Nós, quotas e políticas de armazenamento.", "FiHardDrive", 2);
        Recurso administracao = ensureRecurso("ADMINISTRACAO", "Administração",
                "Gestão de utilizadores, perfis e permissões.", "FiShield", 3);

        ensurePermissao("ARQUIVO_VISUALIZAR", "Visualizar arquivos",
                "Ver a lista de ficheiros e descarregar.", arquivos, 1);
        ensurePermissao("ARQUIVO_ENVIAR", "Enviar arquivos",
                "Carregar novos ficheiros e criar pastas.", arquivos, 2);
        ensurePermissao("ARQUIVO_EDITAR", "Editar arquivos",
                "Renomear, mover e alterar o conteúdo.", arquivos, 3);
        ensurePermissao("ARQUIVO_EXCLUIR", "Excluir arquivos",
                "Mover para a lixeira, restaurar e eliminar.", arquivos, 4);
        ensurePermissao("ARQUIVO_PARTILHAR", "Partilhar arquivos",
                "Partilhar com outros utilizadores e criar links públicos.", arquivos, 5);

        ensurePermissao("ARMAZENAMENTO_VISUALIZAR", "Visualizar armazenamento",
                "Ver consumo, quota e nós de armazenamento.", armazenamento, 1);
        ensurePermissao("ARMAZENAMENTO_GERIR", "Gerir armazenamento",
                "Ligar/desligar nós e definir políticas.", armazenamento, 2);

        ensurePermissao("ADMIN_ACESSAR", "Aceder à administração",
                "Entrar na área de administração.", administracao, 1);
        ensurePermissao("ADMIN_USUARIOS", "Gerir utilizadores",
                "Criar, editar, suspender e excluir contas.", administracao, 2);
        ensurePermissao("ADMIN_PERMISSOES", "Gerir perfis e permissões",
                "Criar perfis, recursos e permissões.", administracao, 3);

        ensurePapel(ROLE_ADMIN, "Administrador", "Acesso total à plataforma.", 1, allPermissionCodes());
        ensurePapel(ROLE_USER, "Utilizador", "Acesso base a arquivos e armazenamento.", 2, List.of(
                "ARQUIVO_VISUALIZAR", "ARQUIVO_ENVIAR", "ARQUIVO_EDITAR",
                "ARQUIVO_EXCLUIR", "ARQUIVO_PARTILHAR", "ARMAZENAMENTO_VISUALIZAR"));
    }

    private Recurso ensureRecurso(String codigo, String nome, String descricao, String icone, int ordem) {
        return recursoRepository.findByCodigoIgnoreCase(codigo).orElseGet(() -> {
            Recurso recurso = new Recurso();
            recurso.setCodigo(codigo);
            recurso.setNome(nome);
            recurso.setDescricao(descricao);
            recurso.setIcone(icone);
            recurso.setOrdem(ordem);
            recurso.setSistema(true);
            return recursoRepository.save(recurso);
        });
    }

    private void ensurePermissao(String codigo, String nome, String descricao, Recurso recurso, int ordem) {
        if (permissaoRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            return;
        }
        Permissao permissao = new Permissao();
        permissao.setCodigo(codigo);
        permissao.setNome(nome);
        permissao.setDescricao(descricao);
        permissao.setRecurso(recurso);
        permissao.setOrdem(ordem);
        permissao.setSistema(true);
        permissaoRepository.save(permissao);
    }

    private void ensurePapel(String codigo, String nome, String descricao, int ordem, List<String> defaultPermissions) {
        if (papelRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            return;
        }
        Papel papel = new Papel();
        papel.setCodigo(codigo);
        papel.setNome(nome);
        papel.setDescricao(descricao);
        papel.setOrdem(ordem);
        papel.setSistema(true);
        papel.setPermissoes(permissaoRepository.findAllByCodigoIn(defaultPermissions));
        papelRepository.save(papel);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<String> allPermissionCodes() {
        return permissaoRepository.findAll().stream()
                .map(Permissao::getCodigo)
                .sorted()
                .toList();
    }

    public List<String> permissionsFor(String roleCode) {
        String code = normalizeRole(roleCode);
        return papelRepository.findByCodigoIgnoreCase(code)
                .map(this::permissionCodesOf)
                .orElseGet(() -> ROLE_ADMIN.equalsIgnoreCase(code)
                        ? allPermissionCodes()
                        : List.of("ARQUIVO_VISUALIZAR", "ARMAZENAMENTO_VISUALIZAR"));
    }

    /** Nome legível do papel (perfil), ex.: "Administrador". */
    public String roleNome(String roleCode) {
        String code = normalizeRole(roleCode);
        return papelRepository.findByCodigoIgnoreCase(code).map(Papel::getNome).orElse(code);
    }

    public Set<String> permissionSetFor(String roleCode) {
        return Set.copyOf(permissionsFor(roleCode));
    }

    public boolean hasPermission(String roleCode, String permission) {
        return permissionsFor(roleCode).contains(permission);
    }

    private List<String> permissionCodesOf(Papel papel) {
        return papel.getPermissoes().stream()
                .map(Permissao::getCodigo)
                .sorted()
                .toList();
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return ROLE_USER;
        }
        return roleCode.trim();
    }

    // ------------------------------------------------------------------
    // Mapeamento para DTOs (usado pelos controllers)
    // ------------------------------------------------------------------

    public PermissaoResponse toPermissao(Permissao p) {
        Recurso r = p.getRecurso();
        return new PermissaoResponse(
                p.getId(), p.getCodigo(), p.getNome(), p.getDescricao(), p.getOrdem(), p.isSistema(),
                r != null ? r.getId() : null,
                r != null ? r.getCodigo() : null,
                r != null ? r.getNome() : null);
    }

    public RecursoResponse toRecurso(Recurso r, List<Permissao> permissoes) {
        List<PermissaoResponse> list = permissoes.stream().map(this::toPermissao).toList();
        return new RecursoResponse(r.getId(), r.getCodigo(), r.getNome(), r.getDescricao(),
                r.getIcone(), r.getOrdem(), r.isSistema(), list);
    }

    public PapelResponse toPapel(Papel papel) {
        return new PapelResponse(
                papel.getId(), papel.getCodigo(), papel.getNome(), papel.getDescricao(),
                papel.getOrdem(), papel.isSistema(),
                userRepository.countByRole(papel.getCodigo()),
                papel.getPermissoes().size(),
                permissionCodesOf(papel));
    }

    /** Recursos do sistema agrupando todas as permissões existentes. */
    public List<RecursoResponse> recursosComPermissoes() {
        Map<Long, List<Permissao>> byRecurso = permissaoRepository
                .findAllByOrderByRecurso_OrdemAscOrdemAscNomeAsc()
                .stream()
                .collect(Collectors.groupingBy(p -> p.getRecurso().getId(), LinkedHashMap::new, Collectors.toList()));
        List<RecursoResponse> result = new ArrayList<>();
        for (Recurso r : recursoRepository.findAllByOrderByOrdemAscNomeAsc()) {
            result.add(toRecurso(r, byRecurso.getOrDefault(r.getId(), List.of())));
        }
        return result;
    }

    public RecursoResponse toRecursoComPermissoes(Recurso recurso) {
        List<Permissao> perms = permissaoRepository
                .findAllByOrderByRecurso_OrdemAscOrdemAscNomeAsc()
                .stream()
                .filter(p -> p.getRecurso() != null && p.getRecurso().getId().equals(recurso.getId()))
                .toList();
        return toRecurso(recurso, perms);
    }

    public List<PapelResponse> allPapeis() {
        return papelRepository.findAllByOrderByOrdemAscNomeAsc().stream().map(this::toPapel).toList();
    }

    /**
     * "O que o meu perfil pode fazer": recursos com as permissões concedidas ao
     * papel, omitindo recursos sem qualquer permissão.
     */
    public AcessoResponse acessoFor(String roleCode) {
        String code = normalizeRole(roleCode);
        Set<String> granted = permissionSetFor(code);
        Optional<Papel> papel = papelRepository.findByCodigoIgnoreCase(code);
        String roleNome = papel.map(Papel::getNome).orElse(code);

        Map<Long, List<Permissao>> byRecurso = permissaoRepository
                .findAllByOrderByRecurso_OrdemAscOrdemAscNomeAsc()
                .stream()
                .collect(Collectors.groupingBy(p -> p.getRecurso().getId(), LinkedHashMap::new, Collectors.toList()));

        List<AcessoResponse.RecursoAcesso> recursos = new ArrayList<>();
        for (Recurso r : recursoRepository.findAllByOrderByOrdemAscNomeAsc()) {
            List<AcessoResponse.PermissaoAcesso> perms = byRecurso.getOrDefault(r.getId(), List.of()).stream()
                    .filter(p -> granted.contains(p.getCodigo()))
                    .map(p -> new AcessoResponse.PermissaoAcesso(p.getCodigo(), p.getNome(), p.getDescricao()))
                    .toList();
            if (!perms.isEmpty()) {
                recursos.add(new AcessoResponse.RecursoAcesso(r.getCodigo(), r.getNome(), r.getDescricao(), r.getIcone(), perms));
            }
        }
        return new AcessoResponse(code, roleNome, recursos);
    }
}
