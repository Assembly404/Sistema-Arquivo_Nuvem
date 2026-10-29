package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    private static final String EMAIL = "admin@test.local";
    private static final String PASSWORD = "Admin@1234";

    @Mock
    private UserRepository userRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private FolderService folderService;

    @Mock
    private SystemNodeService systemNodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seeder, "enabled", true);
        ReflectionTestUtils.setField(seeder, "adminEmail", EMAIL);
        ReflectionTestUtils.setField(seeder, "adminPassword", PASSWORD);
        ReflectionTestUtils.setField(seeder, "adminName", "Administrador");
        ReflectionTestUtils.setField(seeder, "adminSurname", "Sistema");
        ReflectionTestUtils.setField(seeder, "demoData", false);
    }

    @Test
    void createsAdminWhenNoneExists() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.countByRole(PermissionService.ROLE_ADMIN)).thenReturn(0L);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("bcrypt-hash");

        seeder.seed();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals(EMAIL, saved.getEmail());
        assertEquals(PermissionService.ROLE_ADMIN, saved.getRole());
        assertEquals("bcrypt-hash", saved.getPassword());
        assertNotNull(saved.getRegistrationData());
        assertEquals("SYSTEM_FIRST", saved.getStoragePolicy());
        verify(systemNodeService).getOrCreateSystemNode();
    }

    @Test
    void doesNotDuplicateExistingAdmin() {
        User existing = adminUser();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));

        seeder.seed();

        verify(userRepository, never()).save(any());
        verify(systemNodeService).getOrCreateSystemNode();
    }

    @Test
    void doesNotCreateDefaultAdminWhenAnotherAdminExists() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.countByRole(PermissionService.ROLE_ADMIN)).thenReturn(1L);
        when(userRepository.findFirstByRole(PermissionService.ROLE_ADMIN))
                .thenReturn(Optional.of(adminUser()));

        seeder.seed();

        verify(userRepository, never()).save(any());
    }

    @Test
    void createsDemoFoldersWhenEnabled() {
        User existing = adminUser();
        ReflectionTestUtils.setField(seeder, "demoData", true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));
        when(folderRepository.existsByOwnerAndParentIsNullAndNameAndDeletedAtIsNull(eq(existing), anyString()))
                .thenReturn(false);

        seeder.seed();

        verify(folderService).create(existing, null, "Documentos");
        verify(folderService).create(existing, null, "Imagens");
        verify(folderService).create(existing, null, "Projetos");
    }

    @Test
    void doesNotCreateDemoFoldersForExistingNonAdmin() {
        User existing = adminUser();
        existing.setRole(PermissionService.ROLE_USER);
        ReflectionTestUtils.setField(seeder, "demoData", true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));

        seeder.seed();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(folderRepository, folderService);
    }

    @Test
    void doesNothingWhenDisabled() {
        ReflectionTestUtils.setField(seeder, "enabled", false);

        seeder.seed();

        verifyNoInteractions(userRepository, folderRepository, folderService, systemNodeService);
    }

    private User adminUser() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setRole(PermissionService.ROLE_ADMIN);
        return user;
    }
}
