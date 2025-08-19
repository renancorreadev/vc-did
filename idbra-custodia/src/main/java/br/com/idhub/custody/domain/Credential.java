package br.com.idhub.custody.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "credentials",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"statusListId", "statusListIndex"})
       })
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String credentialId;

    @Column(nullable = false)
    private String issuerDid;

    @Column(nullable = false)
    private String holderDid;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String credentialData;

    @Column(nullable = false)
    private String statusListId;

    @Column(nullable = false)
    private Integer statusListIndex;

    @Column(nullable = false)
    private String status; // VALID, REVOKED, SUSPENDED

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String jwsToken;

    @Column
    private String issuerWalletAddress;

    // Construtores
    public Credential() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Credential(String credentialId, String issuerDid, String holderDid,
                     String credentialData, String statusListId, Integer statusListIndex) {
        this();
        this.credentialId = credentialId;
        this.issuerDid = issuerDid;
        this.holderDid = holderDid;
        this.credentialData = credentialData;
        this.statusListId = statusListId;
        this.statusListIndex = statusListIndex;
        this.status = "VALID";
        this.issuedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getIssuerDid() { return issuerDid; }
    public void setIssuerDid(String issuerDid) { this.issuerDid = issuerDid; }

    public String getHolderDid() { return holderDid; }
    public void setHolderDid(String holderDid) { this.holderDid = holderDid; }

    public String getCredentialData() { return credentialData; }
    public void setCredentialData(String credentialData) { this.credentialData = credentialData; }

    public String getStatusListId() { return statusListId; }
    public void setStatusListId(String statusListId) { this.statusListId = statusListId; }

    public Integer getStatusListIndex() { return statusListIndex; }
    public void setStatusListIndex(Integer statusListIndex) { this.statusListIndex = statusListIndex; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getJwsToken() { return jwsToken; }
    public void setJwsToken(String jwsToken) { this.jwsToken = jwsToken; }

    public String getIssuerWalletAddress() { return issuerWalletAddress; }
    public void setIssuerWalletAddress(String issuerWalletAddress) { this.issuerWalletAddress = issuerWalletAddress; }
}
