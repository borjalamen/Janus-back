package com.janushub.service;

import com.janushub.model.Unete;
import com.janushub.repository.UneteRepository;
import com.janushub.repository.UserRepository;
import dto.UneteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uneteService, "baseUrl", "http://localhost:4200");
    }

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
    @DisplayName("createRequest: debe generar un emailToken no nulo")
    void createRequest_generaToken() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(uneteRepository.findByEmailIgnoreCase(anyString())).thenReturn(List.of());
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Unete result = uneteService.createRequest(dto);

        assertThat(result.getEmailToken()).isNotBlank();
    }

    @Test
    @DisplayName("createRequest: debe llamar a sendEmailVerification con la URL correcta")
    void createRequest_enviaEmailVerificacion() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(uneteRepository.findByEmailIgnoreCase(anyString())).thenReturn(List.of());
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        uneteService.createRequest(dto);

        ArgumentCaptor<Unete> capUnete = ArgumentCaptor.forClass(Unete.class);
        ArgumentCaptor<String> capUrl = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendEmailVerification(capUnete.capture(), capUrl.capture());

        assertThat(capUrl.getValue()).startsWith("http://localhost:4200/verificar?token=");
        assertThat(capUrl.getValue()).contains(capUnete.getValue().getEmailToken());
    }

    @Test
    @DisplayName("createRequest: NO debe notificar a admins todavía")
    void createRequest_noNotificaAdmins() {
        UneteDTO dto = buildDto("Ana García", "ana@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(uneteRepository.findByEmailIgnoreCase(anyString())).thenReturn(List.of());
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        uneteService.createRequest(dto);

        verifyNoInteractions(notificationService);
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
    // verifyEmail
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyEmail: cambia estado de INICIADA a PENDIENTE")
    void verifyEmail_cambiaEstadoAPendiente() {
        Unete solicitud = uneteConEstado("INICIADA", "token-abc");
        when(uneteRepository.findByEmailToken("token-abc")).thenReturn(Optional.of(solicitud));
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Unete result = uneteService.verifyEmail("token-abc");

        assertThat(result.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("verifyEmail: registra emailVerifiedAt")
    void verifyEmail_registraFechaVerificacion() {
        Unete solicitud = uneteConEstado("INICIADA", "token-abc");
        when(uneteRepository.findByEmailToken("token-abc")).thenReturn(Optional.of(solicitud));
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Unete result = uneteService.verifyEmail("token-abc");

        assertThat(result.getEmailVerifiedAt()).isNotNull();
        assertThat(result.getEmailVerifiedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("verifyEmail: notifica a ADMIN y DEVOPS tras verificar")
    void verifyEmail_notificaAdmins() {
        Unete solicitud = uneteConEstado("INICIADA", "token-abc");
        when(uneteRepository.findByEmailToken("token-abc")).thenReturn(Optional.of(solicitud));
        when(uneteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        uneteService.verifyEmail("token-abc");

        verify(notificationService).broadcastToRoles(
                argThat(roles -> roles.contains("ADMIN") && roles.contains("DEVOPS")),
                eq("JOIN_NUEVA"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("verifyEmail: token inválido lanza excepción")
    void verifyEmail_tokenInvalidoLanzaExcepcion() {
        when(uneteRepository.findByEmailToken("token-malo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> uneteService.verifyEmail("token-malo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("válido");
    }

    @Test
    @DisplayName("verifyEmail: token vacío lanza excepción")
    void verifyEmail_tokenVacioLanzaExcepcion() {
        assertThatThrownBy(() -> uneteService.verifyEmail(""))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(uneteRepository);
    }

    @Test
    @DisplayName("verifyEmail: solicitud ya PENDIENTE devuelve la solicitud sin error")
    void verifyEmail_yaPendienteDevuelveSinError() {
        Unete solicitud = uneteConEstado("PENDIENTE", "token-abc");
        when(uneteRepository.findByEmailToken("token-abc")).thenReturn(Optional.of(solicitud));

        Unete result = uneteService.verifyEmail("token-abc");

        assertThat(result.getEstado()).isEqualTo("PENDIENTE");
        verify(uneteRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("verifyEmail: solicitud ya APROBADA devuelve sin re-notificar")
    void verifyEmail_yaAprobadaDevuelveSinError() {
        Unete solicitud = uneteConEstado("APROBADA", "token-abc");
        when(uneteRepository.findByEmailToken("token-abc")).thenReturn(Optional.of(solicitud));

        Unete result = uneteService.verifyEmail("token-abc");

        assertThat(result.getEstado()).isEqualTo("APROBADA");
        verifyNoInteractions(notificationService);
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

    private Unete uneteConEstado(String estado, String token) {
        Unete u = new Unete();
        u.setId("id-123");
        u.setFullName("Ana García");
        u.setEmail("ana@test.com");
        u.setEstado(estado);
        u.setEmailToken(token);
        u.setCreatedAt(LocalDateTime.now().minusHours(1));
        return u;
    }
}
