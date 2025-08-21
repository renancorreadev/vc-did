package br.com.idhub.custody.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "credential_offers")
public class CredentialOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String offerId;

    @Column(nullable = false)
    private String holderName; // Nome do contribuinte

    @Column(nullable = false)
    private String holderEmail; // Email para contato

    @Column(nullable = false)
    private String holderCpf; // CPF do contribuinte

    @Column(nullable = false)
    private String credentialType; // Tipo de credencial solicitada

    @Column(columnDefinition = "TEXT")
    private String requestedData; // Dados solicitados em JSON

    @Column(nullable = false)
    private String issuerDid; // DID do emissor (ex: Receita Federal)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OfferStatus status;

    @Column
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private String approvedBy; // Wallet address do aprovador

    @Column
    private String holderWalletAddress; // Preenchido após aprovação

    public enum OfferStatus {
        PENDING,    // Aguardando aprovação
        APPROVED,   // Aprovada pelo emissor
        REJECTED,   // Rejeitada pelo emissor
        COMPLETED   // Credencial emitida
    }

    // Construtores
    public CredentialOffer() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = OfferStatus.PENDING;
    }

    public CredentialOffer(String offerId, String holderName, String holderEmail,
                          String holderCpf, String credentialType, String requestedData,
                          String issuerDid) {
        this();
        this.offerId = offerId;
        this.holderName = holderName;
        this.holderEmail = holderEmail;
        this.holderCpf = holderCpf;
        this.credentialType = credentialType;
        this.requestedData = requestedData;
        this.issuerDid = issuerDid;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public String getHolderEmail() { return holderEmail; }
    public void setHolderEmail(String holderEmail) { this.holderEmail = holderEmail; }

    public String getHolderCpf() { return holderCpf; }
    public void setHolderCpf(String holderCpf) { this.holderCpf = holderCpf; }

    public String getCredentialType() { return credentialType; }
    public void setCredentialType(String credentialType) { this.credentialType = credentialType; }

    public String getRequestedData() { return requestedData; }
    public void setRequestedData(String requestedData) { this.requestedData = requestedData; }

    public String getIssuerDid() { return issuerDid; }
    public void setIssuerDid(String issuerDid) { this.issuerDid = issuerDid; }

    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getHolderWalletAddress() { return holderWalletAddress; }
    public void setHolderWalletAddress(String holderWalletAddress) { this.holderWalletAddress = holderWalletAddress; }
}
