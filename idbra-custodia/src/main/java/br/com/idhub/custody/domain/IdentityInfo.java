package br.com.idhub.custody.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;

public class IdentityInfo {
    private String owner;
    private String didDocument;
    private boolean kycVerified;
    private BigInteger lastActivity;
    private BigInteger lastChange;
    private BigInteger credentialCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IdentityInfo() {}

    public IdentityInfo(String owner, String didDocument, boolean kycVerified,
                       BigInteger lastActivity, BigInteger lastChange, BigInteger credentialCount) {
        this.owner = owner;
        this.didDocument = didDocument;
        this.kycVerified = kycVerified;
        this.lastActivity = lastActivity;
        this.lastChange = lastChange;
        this.credentialCount = credentialCount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDidDocument() {
        return didDocument;
    }

    public void setDidDocument(String didDocument) {
        this.didDocument = didDocument;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(boolean kycVerified) {
        this.kycVerified = kycVerified;
        this.updatedAt = LocalDateTime.now();
    }

    public BigInteger getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(BigInteger lastActivity) {
        this.lastActivity = lastActivity;
        this.updatedAt = LocalDateTime.now();
    }

    public BigInteger getLastChange() {
        return lastChange;
    }

    public void setLastChange(BigInteger lastChange) {
        this.lastChange = lastChange;
        this.updatedAt = LocalDateTime.now();
    }

    public BigInteger getCredentialCount() {
        return credentialCount;
    }

    public void setCredentialCount(BigInteger credentialCount) {
        this.credentialCount = credentialCount;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "IdentityInfo{" +
                "owner='" + owner + '\'' +
                ", didDocument='" + didDocument + '\'' +
                ", kycVerified=" + kycVerified +
                ", lastActivity=" + lastActivity +
                ", lastChange=" + lastChange +
                ", credentialCount=" + credentialCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
