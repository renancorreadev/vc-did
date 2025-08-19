package br.com.idhub.custody.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

@Configuration
public class Web3Config {

    @Value("${web3.node.url:http://localhost:8545}")
    private String nodeUrl;

    @Value("${web3.chain.id:1337}")
    private Long chainId;

    @Value("${web3.wallet.keystore.path:}")
    private String keystorePath;

    @Value("${web3.wallet.password:}")
    private String walletPassword;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(nodeUrl));
    }

    @Bean
    public Credentials credentials() throws Exception {
        if (keystorePath != null && !keystorePath.isEmpty()) {
            return WalletUtils.loadCredentials(walletPassword, keystorePath);
        }
        return Credentials.create("0x0000000000000000000000000000000000000000000000000000000000000001");
    }

    @Bean
    public Long chainId() {
        return chainId;
    }
}
