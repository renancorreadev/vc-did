package br.com.idhub.custody.domain;

import java.math.BigInteger;

public class TxRequest {
    private String from;
    private String to;
    private BigInteger value;
    private String data;
    private BigInteger gasLimit;
    private BigInteger gasPrice;
    private BigInteger nonce;
    private String txHash;
    private TxStatus status;

    public enum TxStatus {
        PENDING, SIGNED, SENT, CONFIRMED, FAILED
    }

    public TxRequest() {}

    public TxRequest(String from, String to, BigInteger value, String data) {
        this.from = from;
        this.to = to;
        this.value = value;
        this.data = data;
        this.status = TxStatus.PENDING;
    }

    // Getters e Setters
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public TxStatus getStatus() {
        return status;
    }

    public void setStatus(TxStatus status) {
        this.status = status;
    }
}
