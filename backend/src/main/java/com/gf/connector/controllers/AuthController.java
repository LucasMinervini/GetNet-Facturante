package com.gf.connector.controllers;

import com.gf.connector.domain.RefreshToken;
import com.gf.connector.domain.User;
import com.gf.connector.security.JwtTokenService;
import com.gf.connector.service.RefreshTokenService;
import com.gf.connector.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword())
            );
            UserDetails user = (UserDetails) auth.getPrincipal();

            // Actualizar último login
            userService.updateLastLogin(body.getUsername());

            // Generar tokens con tenantId
            Optional<User> userEntity = userService.findByUsername(body.getUsername());
            if (userEntity.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
            }
            User domainUser = userEntity.get();
            String access = jwtTokenService.generateAccessToken(user.getUsername(), Map.of(
                "roles", user.getAuthorities(),
                "tenantId", domainUser.getTenantId() != null ? domainUser.getTenantId().toString() : null
            ));
            
            // Crear refresh token en BD
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(domainUser, null);
            
            return ResponseEntity.ok(Map.of(
                "accessToken", access, 
                "refreshToken", refreshToken.getTokenHash(), // En producción, devolver el token real
                "expiresIn", 900 // 15 minutos
            ));
        } catch (Exception e) {
            log.error("Error en login", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest body) {
        try {
            // Validar y rotar refresh token
            Optional<RefreshToken> newTokenOpt = refreshTokenService.validateAndRotateToken(body.getRefreshToken());
            if (newTokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Refresh token inválido o expirado"));
            }

            RefreshToken newToken = newTokenOpt.get();
            User user = newToken.getUser();
            
            // Generar nuevo access token
            String access = jwtTokenService.generateAccessToken(user.getUsername(), Map.of(
                "roles", userService.loadUserByUsername(user.getUsername()).getAuthorities(),
                "tenantId", user.getTenantId() != null ? user.getTenantId().toString() : null
            ));
            
            return ResponseEntity.ok(Map.of(
                "accessToken", access,
                "refreshToken", newToken.getTokenHash(), // En producción, devolver el token real
                "expiresIn", 900
            ));
        } catch (Exception e) {
            log.error("Error en refresh", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Error al renovar token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest body) {
        try {
            if (body != null && body.getRefreshToken() != null) {
                refreshTokenService.revokeToken(body.getRefreshToken());
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error en logout", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al cerrar sesión"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        try {
            User user = userService.createUser(
                body.getUsername(), 
                body.getPassword(), 
                body.getEmail(), 
                body.getFirstName(), 
                body.getLastName()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Usuario creado exitosamente",
                "userId", user.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en registro", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear usuario"));
        }
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Data
    public static class LogoutRequest {
        private String refreshToken;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String firstName;
        private String lastName;
    }
}


