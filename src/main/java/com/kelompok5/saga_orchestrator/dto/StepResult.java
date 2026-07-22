package com.kelompok5.saga_orchestrator.dto;

import java.util.Map;

public class StepResult {

    private String service;
    private String status;
    private Map<String, Object> data;

    public StepResult() {
    }

    public StepResult(String service, String status, Map<String, Object> data) {
        this.service = service;
        this.status = status;
        this.data = data;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
