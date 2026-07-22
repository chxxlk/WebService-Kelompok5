package com.kelompok5.saga_orchestrator.dto;

public class CompensationResult {

    private int step;
    private String service;
    private String action;
    private String status;

    public CompensationResult() {
    }

    public CompensationResult(int step, String service, String action, String status) {
        this.step = step;
        this.service = service;
        this.action = action;
        this.status = status;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
