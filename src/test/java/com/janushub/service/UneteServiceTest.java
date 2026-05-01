package com.janushub.service;

import com.janushub.model.Unete;
import com.janushub.repository.UneteRepository;
import com.janushub.repository.UserRepository;
import dto.UneteDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UneteServiceTest {

    @Mock UneteRepository uneteRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock EmailNotificationService emailNotificationService;
    @Mock NotificationService notificationService;

    @InjectMocks
    UneteService uneteService;

    // ─────────────────────────────────────────────────────────────────
    // createRequest
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRequest: estado inicial debe ser INICIADA")
    void createRequest_estadoIniciada() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(uneteRepository.findByEmailIgnoreCase(anyString())).thenReturn(List.of());
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Unete result = uneteService.createRequest(dto);

        assertThat(result.getEstado()).isEqualTo("INICIADA");
    }

    @Test
    @DisplayName("createRequest: NO debe enviar email pero SÍ notificar a admins")
    void createRequest_noEnviaEmailPeroNotificaAdmins() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(uneteRepository.findByEmailIgnoreCase(anyString())).thenReturn(List.of());
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        uneteService.createRequest(dto);

        verifyNoInteractions(emailNotificationService);
        verify(notificationService).broadcastToRoles(
                argThat(roles -> roles.contains("ADMIN") && roles.contains("DEVOPS")),
                eq("JOIN_NUEVA"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("createRequest: rechaza email vacío")
    void createRequest_emailVacioLanzaExcepcion() {
        UneteDTO dto = buildDto("Ana García", "");

        assertThatThrownBy(() -> uneteService.createRequest(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("createRequest: rechaza email ya registrado como usuario")
    void createRequest_emailYaExisteComoUsuario() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(new com.janushub.model.Users()));

        assertThatThrownBy(() -> uneteService.createRequest(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createRequest: rechaza solicitud duplicada en estado PENDIENTE")
    void createRequest_solicitudDuplicadaPendiente() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Unete existente = new Unete();
        existente.setEstado("PENDIENTE");
        when(uneteRepository.findByEmailIgnoreCase("ana@test.com")).thenReturn(List.of(existente));

        assertThatThrownBy(() -> uneteService.createRequest(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────

    private UneteDTO buildDto(String fullName, String email) {
        UneteDTO dto = new UneteDTO();
        dto.setFullName(fullName);
        dto.setEmail(email);
        dto.setRole("CONSULTOR");
        dto.setProjectCode("PRJ-001");
        dto.setProjectName("Proyecto Test");
        dto.setComments("Test");
        return dto;
    }
}
