package com.kelompok5.saga_orchestrator.repository;

import java.util.List;

import com.kelompok5.saga_orchestrator.model.SagaLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SagaLogRepository extends MongoRepository<SagaLog, String> {
    List<SagaLog> findBySagaId(String sagaId);
    List<SagaLog> findBySagaIdAndType(String sagaId, String type);
    List<SagaLog> findBySagaIdAndStep(String sagaId, int step);
}
