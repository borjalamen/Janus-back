package com.janushub.service;

import com.janushub.model.Parametritzacio;
import com.janushub.repository.ParametritzacioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ParametritzacioService {
    private final ParametritzacioRepository parametrizationRepository;

    @Autowired
    public ParametritzacioService(ParametritzacioRepository parametrizationRepository) {
        this.parametrizationRepository = parametrizationRepository;
    }

    public Optional<String> getVersion() {
        return parametrizationRepository.findAll()
            .stream()
            .findFirst()
            .map(Parametritzacio::getVersion);
    }
}