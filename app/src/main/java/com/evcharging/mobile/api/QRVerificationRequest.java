package com.evcharging.mobile.api;

public class QRVerificationRequest {
    private String qrCodeData;
    private String operatorId;

    public QRVerificationRequest(String qrCodeData, String operatorId) {
        this.qrCodeData = qrCodeData;
        this.operatorId = operatorId;
    }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
}