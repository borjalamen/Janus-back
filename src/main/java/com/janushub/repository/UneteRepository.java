package com.janushub.repository;

import com.janushub.model.Unete;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UneteRepository extends MongoRepository<Unete, String> {

    /**
     * Busca peticiones por estado (PENDIENTE, APROBADA, RECHAZADA).
     */
    List<Unete> findByEstado(String estado);

    /**
     * Busca peticiones por email del solicitante.
     */
    List<Unete> findByEmailIgnoreCase(String email);

    /**
     * Busca peticiones por nombre completo (parcial, case-insensitive).
     */
    List<Unete> findByFullNameContainingIgnoreCase(String fullName);
}
