# 🧩 IDBra Microserviços - API de Credenciais Verificáveis

Este repositório contém os 4 micro-serviços da solução IDBra para gestão de credenciais verificáveis baseadas em blockchain Hyperledger Besu e DID (EIP-1056).

---

## 📦 Micro-serviços

### 1. `idbra-custody`
Gerencia wallets e operações blockchain.

**Endpoints:**

- `/api/wallets/...`
- `/api/blockchain/check-role/{walletAddress}`
- `/api/blockchain/check-admin-role/{walletAddress}`
- `/api/blockchain/grant-issuer-role/{walletAddress}`
- `/api/blockchain/pause`
- `/api/blockchain/unpause`
- `/api/blockchain/status`
- `/api/blockchain/metrics`
- `/api/transactions/...`

---

### 2. `idbra-issuer`
Responsável por emitir, revogar e restaurar credenciais verificáveis e status lists.

**Endpoints:**

- `POST /api/credentials`
- `POST /api/credentials/statuslist`
- `POST /api/credentials/{credentialId}/revoke`
- `POST /api/credentials/{credentialId}/restore`
- `GET /api/credentials`
- `GET /api/credentials/{credentialId}`
- `GET /api/credentials/issuer/{issuerDid}`
- `GET /api/credentials/status/{status}`
- `/api/statuslist/...`

---

### 3. `idbra-holder`
Gerencia as credenciais relacionadas aos usuários (holders).

**Endpoints:**

- `GET /api/credentials/holder/{holderDid}`
- `GET /api/credentials/{credentialId}` (uso compartilhado com issuer)

---

### 4. `idbra-verifier`
Responsável por verificar a validade e status de credenciais.

**Endpoints:**

- `POST /api/credentials/verify`
- `GET /api/credentials/{credentialId}/revocation-status`
- `GET /api/credentials/{credentialId}/exists`
- `GET /api/statuslist/{listId}/status/{index}`

---

## ⚙️ Tecnologias
- Java + Spring Boot
- Hyperledger Besu
- EIP-1056 + W3C Verifiable Credentials
- PostgreSQL
- JWT + JWS (ES256)
- Docker / Docker Compose

---

## 🗃️ Arquitetura de Dados

Cada micro-serviço deve utilizar seu próprio banco de dados para garantir isolamento, segurança e escalabilidade.

---

## 🛡️ Segurança
- Controle de roles e permissões via smart contract
- Criptografia de chaves (custódia)
- StatusList 2021 para revogação
- Assinatura digital com ECDSA secp256k1

---

## 🏁 Exemplo de criação de credencial
```bash
POST /api/credentials
Content-Type: application/json

{
  "issuerDid": "did:web:exemplo.com",
  "holderDid": "did:ethr:0x123...",
  "credentialType": "EducationCredential",
  "credentialSubject": {
    ...
  }
}
```

---

## ✨ Status
✅ PoC funcional com endpoints completos, seguindo os padrões do IDBra.
🚧 Em fase de modularização por micro-serviço.