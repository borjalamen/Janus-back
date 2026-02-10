package com.janushub.config;

import com.janushub.repository.ProceduresRepository;
import com.janushub.model.Procedure;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Sort;

@Configuration
@RequiredArgsConstructor
public class ProceduresMongoConfig {

     private final MongoTemplate mongoTemplate;
    private final ProceduresRepository proceduresRepository;

    @PostConstruct
    public void cleanAndSetupIndex() {
        // 1️⃣ Esborra els documents amb procedureId null
        var toDelete = proceduresRepository.findAll()
                .stream()
                .filter(p -> p.getProcedureId() == null)
                .toList();
        if (!toDelete.isEmpty()) {
            proceduresRepository.deleteAll(toDelete);
            System.out.println("Eliminats " + toDelete.size() + " procediments amb procedureId null");
        } else {
            System.out.println("No s'han trobat procediments amb procedureId null");
        }

        // 2️⃣ Assegura l'índex únic amb partialFilterExpression
    Index index = new Index()
                .on("procedureId", Sort.Direction.ASC)
                .unique();

        mongoTemplate.indexOps(Procedure.class).ensureIndex(index);

        System.out.println("Índex procedureId creat correctament");
    }
}
    

