package com.evcharging.mobile.api;

public class StatusUpdateRequest {
    private String status;
    private String operatorUsername;
    private String notes;

    // Constructors
    public StatusUpdateRequest() {}

    public StatusUpdateRequest(String status, String operatorUsername) {
        this.status = status;
        this.operatorUsername = operatorUsername;
    }

    public StatusUpdateRequest(String status, String operatorUsername, String notes) {
        this.status = status;
        this.operatorUsername = operatorUsername;
        this.notes = notes;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}