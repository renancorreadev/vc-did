package br.com.idhub.custody.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "statuslists")
public class StatusList {

    @Id
    private String id;

    @Field("listId")
    private String listId;

    @Field("uri")
    private String uri;

    @Field("hash")
    private String hash;

    @Field("version")
    private Long version;

    @Field("purpose")
    private String purpose; // revocation, suspension

    @Field("issuer")
    private String issuer;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;

    @Field("statusListData")
    private String statusListData; // JSON da lista de status

    @Field("issuerWalletAddress")
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
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
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

    public String getStatusListData() {
        return statusListData;
    }

    public void setStatusListData(String statusListData) {
        this.statusListData = statusListData;
    }

    public String getIssuerWalletAddress() {
        return issuerWalletAddress;
    }

    public void setIssuerWalletAddress(String issuerWalletAddress) {
        this.issuerWalletAddress = issuerWalletAddress;
    }
}
