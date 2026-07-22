package com.kelompok5.orchestrator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document
public class ServiceLog {

    @Id
    private String id;
    private String serviceName;
    private String orderId;
    private Map<String, Object> request;
    private Map<String, Object> response;
    private String status;
    private LocalDateTime timestamp;

    public ServiceLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ServiceLog(String serviceName, String orderId, Map<String, Object> request,
                      Map<String, Object> response, String status) {
        this();
        this.serviceName = serviceName;
        this.orderId = orderId;
        this.request = request;
        this.response = response;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Map<String, Object> getRequest() { return request; }
    public void setRequest(Map<String, Object> request) { this.request = request; }
    public Map<String, Object> getResponse() { return response; }
    public void setResponse(Map<String, Object> response) { this.response = response; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
