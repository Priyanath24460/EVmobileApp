package com.evcharging.mobile.api;

public class ReactivationRequest {
    private String officerCode;
    private String reason;

    public ReactivationRequest(String officerCode, String reason) {
        this.officerCode = officerCode;
        this.reason = reason;
    }

    public String getOfficerCode() { return officerCode; }
    public void setOfficerCode(String officerCode) { this.officerCode = officerCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}