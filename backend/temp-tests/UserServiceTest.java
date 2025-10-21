package com.gf.connector.service;

import com.gf.connector.domain.Role;
import com.gf.connector.domain.User;
import com.gf.connector.repo.RoleRepository;
import com.gf.connector.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private UserService service;

    private User user;
    private Role viewerRole;
    private Role adminRole;

    @BeforeEach
    void setup() {
        viewerRole = Role.builder()
                .id(UUID.randomUUID())
                .name("Viewer")
                .build();

        adminRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_ADMIN")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .password("encoded-password")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .role(viewerRole)
                .tenantId(UUID.randomUUID())
                .build();
    }

    @Test
    void loadUserByUsername_activeUser_returnsUserDetails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado: nonexistent");
    }

    @Test
    void loadUserByUsername_inactiveUser_throwsException() {
        user.setIsActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("testuser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuario inactivo: testuser");
    }

    @Test
    void loadUserByUsername_userWithAdminRole_returnsCorrectAuthority() {
        user.setRole(adminRole);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("testuser");

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_userWithNullRole_returnsViewerAuthority() {
        user.setRole(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("testuser");

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
    }

    @Test
    void createUser_validData_createsUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("Viewer")).thenReturn(Optional.of(viewerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = service.createUser("newuser", "password123", "new@example.com", "New", "User");

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getRole()).isEqualTo(viewerRole);
        assertThat(result.getTenantId()).isNotNull();
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_usernameExists_throwsException() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("existinguser", "password", "email@example.com", "First", "Last"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El usuario ya existe: existinguser");
    }

    @Test
    void createUser_emailExists_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("newuser", "password", "existing@example.com", "First", "Last"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado: existing@example.com");
    }

    @Test
    void createUser_viewerRoleNotExists_createsRole() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("Viewer")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = service.createUser("newuser", "password123", "new@example.com", "New", "User");

        assertThat(result.getRole().getName()).isEqualTo("Viewer");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateLastLogin_userExists_updatesLastLogin() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.updateLastLogin("testuser");

        assertThat(user.getLastLogin()).isNotNull();
        assertThat(user.getLastLogin()).isAfter(OffsetDateTime.now().minusMinutes(1));
        verify(userRepository).save(user);
    }

    @Test
    void updateLastLogin_userNotExists_doesNothing() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        service.updateLastLogin("nonexistent");

        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_returnsUserFromRepository() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<User> result = service.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByUsername_userNotExists_returnsEmpty() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = service.findByUsername("nonexistent");

        assertThat(result).isEmpty();
        verify(userRepository).findByUsername("nonexistent");
    }
}
