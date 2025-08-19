package br.com.idhub.custody.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_lists",
       uniqueConstraints = @UniqueConstraint(columnNames = {"listId", "version"}))
public class StatusList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)  // Remover unique = true
    private String listId;

    @Column(nullable = false)
    private String uri;

    @Column(nullable = false)
    private String hash;

    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String purpose; // revocation, suspension

    @Column(nullable = false)
    private String issuer;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String statusListData; // JSON da lista de status

    @Column
    private String issuerWalletAddress;

    // Construtores
    public StatusList() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public StatusList(String listId, String uri, String hash, Long version,
                     String purpose, String issuer) {
        this();
        this.listId = listId;
        this.uri = uri;
        this.hash = hash;
        this.version = version;
        this.purpose = purpose;
        this.issuer = issuer;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getListId() { return listId; }
    public void setListId(String listId) { this.listId = listId; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) {
        this.version = version;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatusListData() { return statusListData; }
    public void setStatusListData(String statusListData) { this.statusListData = statusListData; }

    public String getIssuerWalletAddress() { return issuerWalletAddress; }
    public void setIssuerWalletAddress(String issuerWalletAddress) { this.issuerWalletAddress = issuerWalletAddress; }
}
