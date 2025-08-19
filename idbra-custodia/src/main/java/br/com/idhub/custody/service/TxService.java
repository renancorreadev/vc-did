package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.TxRequest;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

@Service
public class TxService {

    private final Web3j web3j;
    private final Credentials credentials;

    public TxService(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
    }

    public TxRequest createTransaction(String to, BigInteger value, String data) {
        try {
            String from = credentials.getAddress();
            TxRequest txRequest = new TxRequest(from, to, value, data);

            // Obter nonce atual
            BigInteger nonce = web3j.ethGetTransactionCount(from, null).send().getTransactionCount();
            txRequest.setNonce(nonce);

            // Estimar gas limit (simplificado)
            txRequest.setGasLimit(BigInteger.valueOf(21000));

            // Obter gas price atual
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            txRequest.setGasPrice(gasPrice);

            return txRequest;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar transação: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<String> signAndSendTransaction(TxRequest txRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Criar transação raw - usando parâmetros corretos para Web3j 4.x
                RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    txRequest.getNonce(),
                    txRequest.getGasPrice(),
                    txRequest.getGasLimit(),
                    txRequest.getTo(),
                    txRequest.getValue()
                );

                // Assinar transação
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                // Enviar transação
                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

                if (ethSendTransaction.hasError()) {
                    throw new RuntimeException("Erro ao enviar transação: " + ethSendTransaction.getError().getMessage());
                }

                String txHash = ethSendTransaction.getTransactionHash();
                txRequest.setTxHash(txHash);
                txRequest.setStatus(TxRequest.TxStatus.SENT);

                return txHash;
            } catch (Exception e) {
                txRequest.setStatus(TxRequest.TxStatus.FAILED);
                throw new RuntimeException("Erro ao assinar/enviar transação: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<TransactionReceipt> waitForTransactionReceipt(String txHash) {
        return web3j.ethGetTransactionReceipt(txHash)
                .sendAsync()
                .thenCompose(receipt -> {
                    if (receipt.getTransactionReceipt().isPresent()) {
                        return CompletableFuture.completedFuture(receipt.getTransactionReceipt().get());
                    } else {
                        // Aguardar e tentar novamente
                        try {
                            Thread.sleep(1000);
                            return waitForTransactionReceipt(txHash);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    public String getTransactionStatus(String txHash) {
        try {
            TransactionReceipt receipt = waitForTransactionReceipt(txHash).get();
            return receipt.isStatusOK() ? "CONFIRMED" : "FAILED";
        } catch (Exception e) {
            return "PENDING";
        }
    }
}
