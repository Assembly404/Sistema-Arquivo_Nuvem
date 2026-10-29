package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Seed inicial do sistema, executado uma vez em cada arranque de forma
 * <strong>idempotente</strong>.
 *
 * <p>Garante, por esta ordem:</p>
 * <ol>
 *   <li>o nó de armazenamento {@code SYSTEM} (nuvem oficial);</li>
 *   <li>uma conta {@code ADMIN} inicial, para que seja possível entrar na
 *       área de administração (o registo público cria sempre {@code USER});</li>
 *   <li>opcionalmente, pastas de demonstração para essa conta.</li>
 * </ol>
 *
 * <p>O catálogo de recursos/permissões/papéis é criado separadamente por
 * {@link PermissionService#seed()} e não é duplicado aqui.</p>
 *
 * <p>Configuração em {@code application.properties} / variáveis de ambiente.
 * Ver {@code docs/SEED.md}.</p>
 */
@Service
@RequiredArgsConstructor
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Mesma regra usada pelo registo e pela reposição de password. */
    private static final String PASSWORD_RULE =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$";

    /** Valores por omissão alinhados com {@code AuthController}. */
    private static final long DEFAULT_QUOTA_BYTES = 5L * 1024L * 1024L * 1024L;
    private static final String DEFAULT_STORAGE_POLICY = "SYSTEM_FIRST";

    private static final List<String> DEMO_FOLDERS = List.of("Documentos", "Imagens", "Projetos");

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FolderService folderService;
    private final SystemNodeService systemNodeService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.admin.email:admin@arquivonuvem.local}")
    private String adminEmail;

    @Value("${app.seed.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${app.seed.admin.name:Administrador}")
    private String adminName;

    @Value("${app.seed.admin.surname:Sistema}")
    private String adminSurname;

    @Value("${app.seed.demo-data:false}")
    private boolean demoData;

    @EventListener(ApplicationReadyEvent.class)
    @Order
    public void seed() {
        if (!enabled) {
            log.info("Seed desativado (app.seed.enabled=false).");
            return;
        }

        systemNodeService.getOrCreateSystemNode();

        User admin = ensureAdmin();
        if (demoData && admin != null) {
            ensureDemoFolders(admin);
        }
    }

    private User ensureAdmin() {
        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        if (email.isBlank()) {
            log.warn("Seed: app.seed.admin.email vazio; conta ADMIN não criada.");
            return null;
        }

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!PermissionService.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                log.warn("Seed: já existe uma conta com o email {} mas o papel é {} (esperado ADMIN). "
                        + "A conta não foi alterada.", email, user.getRole());
                return null;
            }
            log.info("Seed: conta ADMIN {} já existe.", email);
            return user;
        }

        // Se já existe outro administrador, não criar a conta padrão.
        if (userRepository.countByRole(PermissionService.ROLE_ADMIN) > 0) {
            log.info("Seed: já existe pelo menos um ADMIN; conta padrão não criada.");
            return userRepository.findFirstByRole(PermissionService.ROLE_ADMIN).orElse(null);
        }

        if (adminPassword == null || !adminPassword.matches(PASSWORD_RULE)) {
            throw new IllegalStateException(
                    "Seed: SEED_ADMIN_PASSWORD não cumpre a regra (8-24 caracteres, "
                            + "maiúscula, minúscula, número e símbolo).");
        }

        User admin = new User();
        admin.setName(adminName);
        admin.setSurname(adminSurname);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRegistrationData(LocalDateTime.now());
        admin.setRole(PermissionService.ROLE_ADMIN);
        admin.setQuotaBytes(DEFAULT_QUOTA_BYTES);
        admin.setStoragePolicy(DEFAULT_STORAGE_POLICY);
        userRepository.save(admin);

        log.info("Seed: conta ADMIN criada ({}). Password definida por SEED_ADMIN_PASSWORD "
                + "ou pelo valor padrão de desenvolvimento — ver docs/SEED.md.", email);
        return admin;
    }

    private void ensureDemoFolders(User owner) {
        for (String name : DEMO_FOLDERS) {
            if (folderRepository.existsByOwnerAndParentIsNullAndNameAndDeletedAtIsNull(owner, name)) {
                continue;
            }
            try {
                folderService.create(owner, null, name);
                log.info("Seed: pasta demo '{}' criada para {}.", name, owner.getEmail());
            } catch (IllegalStateException e) {
                log.debug("Seed: pasta demo '{}' já existe para {}.", name, owner.getEmail());
            }
        }
    }
}
