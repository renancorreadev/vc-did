package br.com.idhub.custody.web;

import br.com.idhub.custody.domain.TxRequest;
import br.com.idhub.custody.service.TxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TxController {

    private final TxService txService;

    public TxController(TxService txService) {
        this.txService = txService;
    }

    @PostMapping("/create")
    public ResponseEntity<TxRequest> createTransaction(
            @RequestParam(name = "to") String to,
            @RequestParam(name = "value") BigInteger value,
            @RequestParam(name = "data", required = false) String data) {
        try {
            if (data == null) {
                data = "";
            }
            TxRequest txRequest = txService.createTransaction(to, value, data);
            return ResponseEntity.ok(txRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendTransaction(@RequestBody TxRequest txRequest) {
        try {
            CompletableFuture<String> txHashFuture = txService.signAndSendTransaction(txRequest);
            String txHash = txHashFuture.get();
            return ResponseEntity.ok(txHash);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao enviar transação: " + e.getMessage());
        }
    }

    @GetMapping("/{txHash}/status")
    public ResponseEntity<String> getTransactionStatus(@PathVariable String txHash) {
        try {
            String status = txService.getTransactionStatus(txHash);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao obter status: " + e.getMessage());
        }
    }

    @GetMapping("/{txHash}/receipt")
    public ResponseEntity<TransactionReceipt> getTransactionReceipt(@PathVariable String txHash) {
        try {
            CompletableFuture<TransactionReceipt> receiptFuture = txService.waitForTransactionReceipt(txHash);
            TransactionReceipt receipt = receiptFuture.get();
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/estimate-gas")
    public ResponseEntity<BigInteger> estimateGas(
            @RequestParam(name = "to") String to,
            @RequestParam(name = "value") BigInteger value,
            @RequestParam(name = "data", required = false) String data) {
        try {
            // Implementação simplificada - retorna gas padrão para transação ETH
            if (data == null || data.isEmpty()) {
                return ResponseEntity.ok(BigInteger.valueOf(21000));
            } else {
                // Para transações com dados, estimar baseado no tamanho
                int dataSize = data.length() / 2; // Hex string
                return ResponseEntity.ok(BigInteger.valueOf(21000 + (dataSize * 68)));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
