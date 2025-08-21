package br.com.idhub.web3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;

@Configuration
public class Web3Config {

    @Value("${blockchain.besu.node-url:http://144.22.179.183}")
    private String nodeUrl;

    @Value("${web3j.chain-id:1337}")
    private Long chainId;

    @Value("${ADMIN_PRIVATE_KEY:8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63}")
    private String adminPrivateKey;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(nodeUrl));
    }

    @Bean
    public Credentials adminCredentials() throws Exception {
        if (adminPrivateKey != null && !adminPrivateKey.isEmpty()) {
            return Credentials.create(adminPrivateKey);
        }
        throw new RuntimeException("ADMIN_PRIVATE_KEY não configurada");
    }

    @Bean
    public Long chainId() {
        return chainId;
    }
}
