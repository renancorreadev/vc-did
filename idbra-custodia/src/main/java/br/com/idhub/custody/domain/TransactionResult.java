package br.com.idhub.custody.domain;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class TransactionResult {
    private String transactionHash;
    private TransactionReceipt receipt;

    public TransactionResult(String transactionHash, TransactionReceipt receipt) {
        this.transactionHash = transactionHash;
        this.receipt = receipt;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public TransactionReceipt getReceipt() {
        return receipt;
    }
}
