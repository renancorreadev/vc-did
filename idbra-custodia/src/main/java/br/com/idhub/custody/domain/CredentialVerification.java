package br.com.idhub.custody.domain;

import java.time.LocalDateTime;
import java.util.List;

public class CredentialVerification {
    
    private boolean valid;
    private String credentialId;
    private String issuerDid;
    private String holderDid;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private List<String> errors;
    private List<String> warnings;
    
    // Construtores
    public CredentialVerification() {}
    
    public CredentialVerification(boolean valid, String credentialId) {
        this.valid = valid;
        this.credentialId = credentialId;
    }
    
    // Getters e Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
    
    public String getIssuerDid() { return issuerDid; }
    public void setIssuerDid(String issuerDid) { this.issuerDid = issuerDid; }
    
    public String getHolderDid() { return holderDid; }
    public void setHolderDid(String holderDid) { this.holderDid = holderDid; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    // Métodos utilitários
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new java.util.ArrayList<>();
        }
        this.errors.add(error);
    }
    
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new java.util.ArrayList<>();
        }
        this.warnings.add(warning);
    }
}
