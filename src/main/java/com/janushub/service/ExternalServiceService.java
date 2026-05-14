package com.janushub.service;

import com.janushub.model.ExternalService;
import com.janushub.repository.ExternalServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExternalServiceService {

    private final ExternalServiceRepository repository;

    public ExternalServiceService(ExternalServiceRepository repository) {
        this.repository = repository;
    }

    public List<ExternalService> getAll() {
        return repository.findAllByOrderByNameAsc();
    }

    public Optional<ExternalService> getById(String id) {
        return repository.findById(id);
    }

    public ExternalService create(ExternalService service) {
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        return repository.save(service);
    }

    public Optional<ExternalService> update(String id, ExternalService details) {
        return repository.findById(id).map(existing -> {
            existing.setName(details.getName());
            existing.setCode(details.getCode());
            existing.setDescription(details.getDescription());
            existing.setUrl(details.getUrl());
            existing.setUpdatedAt(LocalDateTime.now());
            return repository.save(existing);
        });
    }

    public boolean delete(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
