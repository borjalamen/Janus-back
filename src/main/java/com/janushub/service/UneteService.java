package com.janushub.service;

import com.janushub.model.Unete;
import com.janushub.repository.UneteRepository;
import dto.UneteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UneteService {

    private final UneteRepository uneteRepository;

    /**
     * Registra una nueva peticion de "unete" con estado PENDIENTE.
     */
    public Unete createRequest(UneteDTO dto) {
        Unete request = new Unete();
        request.setFullName(dto.getFullName());
        request.setEmail(dto.getEmail());
        request.setRole(dto.getRole());
        request.setProjectCode(dto.getProjectCode());
        request.setProjectName(dto.getProjectName());
        request.setComments(dto.getComments());
        request.setEstado("PENDIENTE");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return uneteRepository.save(request);
    }

    /**
     * Devuelve todas las peticiones (para el panel de administracion).
     */
    public List<Unete> getAllRequests() {
        return uneteRepository.findAll();
    }

    /**
     * Filtra peticiones por estado.
     */
    public List<Unete> getRequestsByEstado(String estado) {
        return uneteRepository.findByEstado(estado);
    }

    /**
     * Obtiene una peticion por su ID.
     */
    public Unete getRequestById(String id) {
        return uneteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peticion no encontrada: " + id));
    }

    /**
     * Aprueba una peticion.
     */
    public Unete approveRequest(String id, String adminComment) {
        return updateEstado(id, "APROBADA", adminComment);
    }

    /**
     * Rechaza una peticion.
     */
    public Unete rejectRequest(String id, String adminComment) {
        return updateEstado(id, "RECHAZADA", adminComment);
    }

    /**
     * Actualiza el estado de una peticion existente.
     */
    private Unete updateEstado(String id, String nuevoEstado, String adminComment) {
        Unete request = getRequestById(id);
        request.setEstado(nuevoEstado);
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        return uneteRepository.save(request);
    }

    /**
     * Elimina una peticion por su ID.
     */
    public void deleteRequest(String id) {
        if (!uneteRepository.existsById(id)) {
            throw new IllegalArgumentException("Peticion no encontrada: " + id);
        }
        uneteRepository.deleteById(id);
    }
}
