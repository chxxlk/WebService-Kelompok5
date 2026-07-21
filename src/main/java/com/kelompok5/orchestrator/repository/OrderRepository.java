package com.kelompok5.orchestrator.repository;

import com.kelompok5.orchestrator.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {
}
