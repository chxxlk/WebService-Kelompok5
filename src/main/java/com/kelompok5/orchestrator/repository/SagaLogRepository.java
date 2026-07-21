package com.kelompok5.orchestrator.repository;

import com.kelompok5.orchestrator.model.SagaLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SagaLogRepository extends MongoRepository<SagaLog, String> {
    List<SagaLog> findBySagaIdOrderByTimestampAsc(String sagaId);
}
