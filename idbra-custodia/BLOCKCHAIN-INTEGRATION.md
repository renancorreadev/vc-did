# 🔗 Integração com Smart Contracts

## **📋 Visão Geral**

O **Custody Service** agora está totalmente integrado com os smart contracts **StatusListManager** e **DIDRegistry** para fornecer um sistema completo de credenciais verificáveis com ancoragem on-chain.

## **🏗️ Arquitetura da Integração**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Custody       │    │   Smart          │    │   Blockchain    │
│   Service       │◄──►│   Contracts      │◄──►│   Network       │
│                 │    │                  │    │   (Besu)        │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## **🔐 Smart Contracts Integrados**

### **1. StatusListManager**

- **Função:** Gerenciar StatusLists para credenciais
- **Operações:**
  - `createList()` - Criar nova StatusList
  - `publish()` - Atualizar versão existente
  - `transferListController()` - Transferir controle

### **2. DIDRegistry**

- **Função:** Registrar metadados de credenciais
- **Operações:**
  - `setAttribute()` - Ancorar metadados
  - `revokeAttribute()` - Revogar metadados
  - `setController()` - Definir controlador

### **3. RegistryAccess**

- **Função:** Controle de acesso baseado em roles
- **Roles:**
  - `DEFAULT_ADMIN_ROLE` - Governança do consórcio
  - `REGISTRAR_ROLE` - Operadores do registro
  - `ISSUER_ROLE` - Emissores autorizados

## **⚡ Configuração Zero Gas**

### **Modo Legacy sem ETH**

```java
// Gas provider para zero gas (modo legacy)
private final ContractGasProvider zeroGasProvider = new StaticGasProvider(
    BigInteger.ZERO,  // gasPrice = 0
    BigInteger.valueOf(4_700_000)  // gasLimit = 4.7M
);
```

### **Transações Raw**

```java
RawTransaction rawTransaction = RawTransaction.createTransaction(
    nonce,
    BigInteger.ZERO,  // gasPrice = 0
    BigInteger.valueOf(4_700_000),  // gasLimit
    null,  // to (será definido pelo contrato)
    BigInteger.ZERO,  // value
    functionData
);
```

## **🚀 Fluxo de Operações**

### **1. Criação de StatusList**

```
1. Usuário cria StatusList via API
2. Sistema gera hash SHA-256 dos dados
3. Salva localmente no banco
4. Publica on-chain via StatusListManager
5. Retorna confirmação
```

### **2. Emissão de Credencial**

```
1. Usuário cria credencial via API
2. Sistema assina com JWT
3. Salva localmente no banco
4. Ancorar metadados no DIDRegistry (opcional)
5. Retorna JWT assinado
```

### **3. Revogação de Credencial**

```
1. Usuário revoga credencial via API
2. Sistema marca como revogada localmente
3. Atualiza StatusList on-chain
4. Retorna confirmação
```

## **🔍 Endpoints da API**

### **Blockchain Operations**

- `GET /api/blockchain/check-role/{walletAddress}` - Verificar ISSUER_ROLE
- `GET /api/blockchain/status` - Status da conexão blockchain

### **StatusList Operations**

- `POST /api/statuslist` - Criar nova StatusList
- `PUT /api/statuslist/{listId}` - Atualizar StatusList
- `POST /api/statuslist/{listId}/revoke/{index}` - Revogar credencial
- `GET /api/statuslist/{listId}/json` - Obter StatusList como JSON
- `GET /api/statuslist/{listId}/metadata` - Metadados da StatusList

### **Credential Operations**

- `POST /api/credentials` - Criar credencial verificável
- `POST /api/credentials/verify` - Verificar credencial
- `POST /api/credentials/{id}/revoke` - Revogar credencial

## **🛡️ Segurança e Validação**

### **Verificação de Roles**

- Todas as operações verificam se a wallet tem `ISSUER_ROLE`
- Controle de acesso baseado em endereços Ethereum
- Validação de assinaturas para operações críticas

### **Integridade dos Dados**

- Hash SHA-256 para todas as StatusLists
- Versionamento automático com controle de integridade
- Auditoria completa de mudanças

## **📊 Monitoramento e Logs**

### **Logs de Transações**

```java
blockchainService.publishStatusList(...)
    .thenAccept(receipt -> {
        System.out.println("StatusList publicada on-chain: " + listId +
                         " - TX: " + receipt.getTransactionHash());
    })
    .exceptionally(throwable -> {
        System.err.println("Erro ao publicar StatusList on-chain: " +
                         listId + " - " + throwable.getMessage());
        return null;
    });
```

### **Métricas Disponíveis**

- Status de conexão blockchain
- Contagem de transações bem-sucedidas
- Histórico de erros e falhas
- Estatísticas de StatusLists

## **🔧 Configuração**

### **Variáveis de Ambiente**

```yaml
web3j:
  client-address: ${BESU_NODE_URL:http://144.22.179.183:8545}
  chain-id: ${WEB3J_CHAIN_ID:1337}
  network-id: ${WEB3J_NETWORK_ID:1337}
```

### **Dependências Maven**

```xml
<dependency>
    <groupId>org.web3j</groupId>
    <artifactId>core</artifactId>
    <version>${web3j.version}</version>
</dependency>
<dependency>
    <groupId>org.web3j</groupId>
    <artifactId>contracts</artifactId>
    <version>${web3j.version}</version>
</dependency>
```

## **🚀 Próximos Passos**

### **Implementações Futuras**

1. **Verificação Real de Roles** - Integração com StatusListManager
2. **Eventos On-Chain** - Listener para eventos dos contratos
3. **Fallback Offline** - Operação sem blockchain quando necessário
4. **Batch Operations** - Operações em lote para múltiplas credenciais

### **Melhorias de Performance**

1. **Cache de StatusLists** - Reduzir consultas on-chain
2. **Transações Assíncronas** - Não bloquear operações da API
3. **Retry Mechanism** - Tentativas automáticas em caso de falha

## **📚 Recursos Adicionais**

- [Web3j Documentation](https://docs.web3j.io/)
- [Solidity Documentation](https://docs.soliditylang.org/)
- [Besu Documentation](https://besu.hyperledger.org/)
- [StatusList 2021 Specification](https://w3c.github.io/vc-status-list-2021/)
