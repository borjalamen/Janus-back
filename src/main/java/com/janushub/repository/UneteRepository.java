package com.janushub.repository;

import com.janushub.model.Unete;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UneteRepository extends MongoRepository<Unete, String> {

    List<Unete> findByEstado(String estado);

    List<Unete> findByEmailIgnoreCase(String email);

    List<Unete> findByFullNameContainingIgnoreCase(String fullName);

    Optional<Unete> findByEmailToken(String emailToken);
}
