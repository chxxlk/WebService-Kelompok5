package com.kelompok5.orchestrator.repository;

import com.kelompok5.orchestrator.model.ServiceLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServiceLogRepository extends MongoRepository<ServiceLog, String> {
}
