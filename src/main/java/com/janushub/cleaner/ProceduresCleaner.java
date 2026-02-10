package com.janushub.cleaner;

import com.janushub.repository.ProceduresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProceduresCleaner implements CommandLineRunner{

      private final ProceduresRepository proceduresRepository;

    @Override
    public void run(String... args) throws Exception {
        var toDelete = proceduresRepository.findByProcedureIdIsNull();
        if (!toDelete.isEmpty()) {
            proceduresRepository.deleteAll(toDelete);
            System.out.println("Eliminats " + toDelete.size() + " procediments amb procedureId null");
        }
    }
    
}
