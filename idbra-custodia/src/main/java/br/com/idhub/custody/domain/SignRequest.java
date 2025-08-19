package br.com.idhub.custody.domain;

import java.math.BigInteger;

public class SignRequest {
    private String messageHash;
    private String address;
    private String signature;
    private SignStatus status;
    private String errorMessage;

    public enum SignStatus {
        PENDING, SIGNED, FAILED
    }

    public SignRequest() {}

    public SignRequest(String messageHash, String address) {
        this.messageHash = messageHash;
        this.address = address;
        this.status = SignStatus.PENDING;
    }

    // Getters e Setters
    public String getMessageHash() {
        return messageHash;
    }

    public void setMessageHash(String messageHash) {
        this.messageHash = messageHash;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public SignStatus getStatus() {
        return status;
    }

    public void setStatus(SignStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
