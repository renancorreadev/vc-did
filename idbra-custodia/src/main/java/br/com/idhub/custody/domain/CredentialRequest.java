package br.com.idhub.custody.domain;

import java.time.LocalDateTime;
import java.util.Map;

public class CredentialRequest {

    private String issuerDid;
    private String holderDid;
    private String credentialType;
    private Map<String, Object> credentialSubject;
    private LocalDateTime expiresAt;
    private String statusListId;
    private String issuerWalletAddress;
    // NOVO CAMPO
    private String holderWalletAddress;

    // Construtores
    public CredentialRequest() {}

    public CredentialRequest(String issuerDid, String holderDid, String credentialType,
                           Map<String, Object> credentialSubject, String statusListId) {
        this.issuerDid = issuerDid;
        this.holderDid = holderDid;
        this.credentialType = credentialType;
        this.credentialSubject = credentialSubject;
        this.statusListId = statusListId;
    }

    // Getters e Setters
    public String getIssuerDid() { return issuerDid; }
    public void setIssuerDid(String issuerDid) { this.issuerDid = issuerDid; }

    public String getHolderDid() { return holderDid; }
    public void setHolderDid(String holderDid) { this.holderDid = holderDid; }

    public String getCredentialType() { return credentialType; }
    public void setCredentialType(String credentialType) { this.credentialType = credentialType; }

    public Map<String, Object> getCredentialSubject() { return credentialSubject; }
    public void setCredentialSubject(Map<String, Object> credentialSubject) { this.credentialSubject = credentialSubject; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getStatusListId() { return statusListId; }
    public void setStatusListId(String statusListId) { this.statusListId = statusListId; }

    public String getIssuerWalletAddress() { return issuerWalletAddress; }
    public void setIssuerWalletAddress(String issuerWalletAddress) { this.issuerWalletAddress = issuerWalletAddress; }
    public String getHolderWalletAddress() { return holderWalletAddress; }
    public void setHolderWalletAddress(String holderWalletAddress) { this.holderWalletAddress = holderWalletAddress; }
}
