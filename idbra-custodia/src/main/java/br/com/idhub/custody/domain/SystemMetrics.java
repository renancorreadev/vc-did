package br.com.idhub.custody.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;

public class SystemMetrics {
    private BigInteger totalDIDs;
    private BigInteger verifiedDIDs;
    private BigInteger totalCredentials;
    private BigInteger revokedCredentials;
    private BigInteger totalOperations;
    private LocalDateTime timestamp;

    public SystemMetrics() {
        this.timestamp = LocalDateTime.now();
    }

    public SystemMetrics(BigInteger totalDIDs, BigInteger verifiedDIDs,
                        BigInteger totalCredentials, BigInteger revokedCredentials,
                        BigInteger totalOperations) {
        this.totalDIDs = totalDIDs;
        this.verifiedDIDs = verifiedDIDs;
        this.totalCredentials = totalCredentials;
        this.revokedCredentials = revokedCredentials;
        this.totalOperations = totalOperations;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public BigInteger getTotalDIDs() {
        return totalDIDs;
    }

    public void setTotalDIDs(BigInteger totalDIDs) {
        this.totalDIDs = totalDIDs;
    }

    public BigInteger getVerifiedDIDs() {
        return verifiedDIDs;
    }

    public void setVerifiedDIDs(BigInteger verifiedDIDs) {
        this.verifiedDIDs = verifiedDIDs;
    }

    public BigInteger getTotalCredentials() {
        return totalCredentials;
    }

    public void setTotalCredentials(BigInteger totalCredentials) {
        this.totalCredentials = totalCredentials;
    }

    public BigInteger getRevokedCredentials() {
        return revokedCredentials;
    }

    public void setRevokedCredentials(BigInteger revokedCredentials) {
        this.revokedCredentials = revokedCredentials;
    }

    public BigInteger getTotalOperations() {
        return totalOperations;
    }

    public void setTotalOperations(BigInteger totalOperations) {
        this.totalOperations = totalOperations;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SystemMetrics{" +
                "totalDIDs=" + totalDIDs +
                ", verifiedDIDs=" + verifiedDIDs +
                ", totalCredentials=" + totalCredentials +
                ", revokedCredentials=" + revokedCredentials +
                ", totalOperations=" + totalOperations +
                ", timestamp=" + timestamp +
                '}';
    }
}
