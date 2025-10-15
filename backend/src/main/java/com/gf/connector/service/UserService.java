package com.gf.connector.service;

import com.gf.connector.domain.User;
import com.gf.connector.domain.Role;
import com.gf.connector.repo.UserRepository;
import com.gf.connector.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }
        
        String roleName = user.getRole() != null ? user.getRole().getName() : "Viewer";
        // Evitar doble prefijo. Si ya viene como 'ROLE_ADMIN' usarlo tal cual; si viene 'ADMIN' o 'Viewer', prefijar correctamente
        String springRole = roleName != null && roleName.startsWith("ROLE_")
                ? roleName
                : "ROLE_" + roleName.toUpperCase();
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(springRole)
                .build();
    }
    
    @Transactional
    public User createUser(String username, String password, String email, String firstName, String lastName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El usuario ya existe: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado: " + email);
        }
        
        // Rol por defecto Viewer (crear si no existe)
        Role viewerRole = roleRepository.findByName("Viewer").orElseGet(() -> {
            Role r = Role.builder().name("Viewer").build();
            return roleRepository.save(r);
        });

        java.util.UUID tenantId = java.util.UUID.randomUUID();

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .isActive(true)
                .role(viewerRole)
                .tenantId(tenantId)
                .build();
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void updateLastLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(OffsetDateTime.now());
            userRepository.save(user);
        }
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
