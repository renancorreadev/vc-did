package br.com.idhub.custody.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;

public class RevocationRecord {
    private String credentialId;
    private boolean isRevoked;
    private String revoker;
    private BigInteger revokedAt;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RevocationRecord() {}

    public RevocationRecord(String credentialId, boolean isRevoked, String revoker,
                           BigInteger revokedAt, String reason) {
        this.credentialId = credentialId;
        this.isRevoked = isRevoked;
        this.revoker = revoker;
        this.revokedAt = revokedAt;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean revoked) {
        isRevoked = revoked;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRevoker() {
        return revoker;
    }

    public void setRevoker(String revoker) {
        this.revoker = revoker;
        this.updatedAt = LocalDateTime.now();
    }

    public BigInteger getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(BigInteger revokedAt) {
        this.revokedAt = revokedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
        return "RevocationRecord{" +
                "credentialId='" + credentialId + '\'' +
                ", isRevoked=" + isRevoked +
                ", revoker='" + revoker + '\'' +
                ", revokedAt=" + revokedAt +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
