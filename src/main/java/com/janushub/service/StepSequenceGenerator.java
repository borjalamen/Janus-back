package com.janushub.service;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.janushub.model.StepCounter;

@Service

public class StepSequenceGenerator {

     private final MongoOperations mongoOperations;

    public StepSequenceGenerator(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long getNextSequence(String counterName) {
        StepCounter counter = mongoOperations.findAndModify(
                new Query(where("_id").is(counterName)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                StepCounter.class
        );

        return counter.getSeq();
    }

    
}
