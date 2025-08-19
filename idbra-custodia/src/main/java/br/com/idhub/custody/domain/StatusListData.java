package br.com.idhub.custody.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class StatusListData {

    @JsonProperty("@context")
    private List<String> context;

    private String id;
    private String type;
    private String issuer;
    private String issued;
    private String validFrom;
    private String validUntil;
    private String credentialSubject;
    private List<Integer> statusList;

    // Construtores
    public StatusListData() {
        this.context = List.of(
            "https://www.w3.org/2018/credentials/v1",
            "https://w3id.org/vc/status-list/2021/v1"
        );
        this.type = "StatusList2021Credential";
    }

    public StatusListData(String id, String issuer, String issued, List<Integer> statusList) {
        this();
        this.id = id;
        this.issuer = issuer;
        this.issued = issued;
        this.statusList = statusList;
    }

    // Getters e Setters
    public List<String> getContext() { return context; }
    public void setContext(List<String> context) { this.context = context; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getIssued() { return issued; }
    public void setIssued(String issued) { this.issued = issued; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }

    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }

    public String getCredentialSubject() { return credentialSubject; }
    public void setCredentialSubject(String credentialSubject) { this.credentialSubject = credentialSubject; }

    public List<Integer> getStatusList() { return statusList; }
    public void setStatusList(List<Integer> statusList) { this.statusList = statusList; }
}
