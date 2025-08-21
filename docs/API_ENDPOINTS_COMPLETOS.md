# 🚀 **API DE CUSTÓDIA IDBRA - ENDPOINTS COMPLETOS**

## 📋 **Base URL: `http://147.93.11.54:8082`**

---

## **1. 🏦 WALLET CONTROLLER**
**Base Path: `/api/wallets`**

### **🔧 Criação e Gerenciamento**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/` | Criar nova wallet | `name`, `description` |
| `POST` | `/with-password` | Criar wallet com senha | `name`, `description`, `password` |
| `POST` | `/import-admin` | Importar wallet administrativa | - |

### **📊 Consultas e Informações**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/` | Listar todas as wallets | - |
| `GET` | `/{address}` | Obter informações da wallet | `address` |
| `GET` | `/{address}/balance` | Obter saldo formatado | `address` |
| `GET` | `/{address}/balance/raw` | Obter saldo bruto | `address` |
| `GET` | `/credentials` | Obter credenciais da wallet | - |
| `GET` | `/{address}/credentials` | Obter credenciais da wallet | `address`, `password` |
| `GET` | `/{address}/credentials/master` | Obter credenciais com senha master | `address` |

### **🔐 Desenvolvimento (⚠️ APENAS DEV)**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/{address}/private-key/dev` | Obter chave privada (DEV) | `address` |
| `GET` | `/{address}/private-key/dev/encrypted` | Obter chave privada criptografada (DEV) | `address` |

### **✏️ Atualizações**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `PUT` | `/{address}` | Atualizar wallet | `address`, `name`, `description` |
| `DELETE` | `/{address}` | Desativar wallet | `address` |

---

## **2. ⛓️ BLOCKCHAIN CONTROLLER**
**Base Path: `/api/blockchain`**

### **🔐 Gerenciamento de Roles**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/check-role/{walletAddress}` | Verificar ISSUER_ROLE | `walletAddress` |
| `GET` | `/check-admin-role/{walletAddress}` | Verificar DEFAULT_ADMIN_ROLE | `walletAddress` |
| `POST` | `/grant-issuer-role/{walletAddress}` | Conceder ISSUER_ROLE | `walletAddress` |

### **🆔 Gerenciamento de DIDs**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/did/create` | Criar novo DID | `identity`, `didDocument` |

### **✅ Verificação KYC**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/kyc/{identity}` | Verificar status KYC | `identity` |
| `POST` | `/kyc/{identity}` | Definir status KYC | `identity`, `verified` |

### **⏸️ Controle de Contratos**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/pause` | Pausar contrato (admin) | - |
| `POST` | `/unpause` | Despausar contrato (admin) | - |

### **📊 Status e Métricas**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/status` | Status da conexão blockchain | - |
| `GET` | `/metrics` | Métricas do sistema | - |

---

## **3. 🎫 CREDENTIAL CONTROLLER**
**Base Path: `/api/credentials`**

### **🔧 Criação e Gerenciamento**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/` | Criar credencial verificável | `CredentialRequest` (body) |
| `POST` | `/statuslist` | Criar/atualizar StatusList | `listId`, `uri`, `purpose`, `issuer`, `issuerWalletAddress` |

### **✅ Verificação**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/verify` | Verificar credencial | `credential` (body) |

### **🚫 Revogação e Restauração**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/{credentialId}/revoke` | Revogar credencial | `credentialId` |
| `POST` | `/{credentialId}/restore` | Restaurar credencial | `credentialId`, `subject`, `reason` |

### **📊 Consultas de Credenciais**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/` | Listar todas as credenciais | - |
| `GET` | `/{credentialId}` | Obter credencial por ID | `credentialId` |
| `GET` | `/issuer/{issuerDid}` | Listar credenciais por emissor | `issuerDid` |
| `GET` | `/holder/{holderDid}` | Listar credenciais por holder | `holderDid` |
| `GET` | `/status/{status}` | Listar credenciais por status | `status` |

### **📋 Consultas de StatusList**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/statuslist/{listId}` | Obter StatusList por ID | `listId` |
| `GET` | `/statuslist/issuer/{issuer}` | Listar StatusLists por emissor | `issuer` |

### **🔍 Verificações Blockchain**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/{credentialId}/revocation-status` | Status de revogação na blockchain | `credentialId` |
| `GET` | `/{credentialId}/exists` | Verificar se credencial existe na blockchain | `credentialId` |

---

## **4. 📋 STATUS LIST CONTROLLER**
**Base Path: `/api/statuslist`**

### **🔧 Criação e Atualização**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/` | Criar nova StatusList | `listId`, `uri`, `purpose`, `issuer`, `issuerWalletAddress` |
| `PUT` | `/{listId}` | Atualizar StatusList existente | `listId`, `newUri`, `issuerWalletAddress` |

### **🚫 Revogação**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/{listId}/revoke/{index}` | Revogar credencial na StatusList | `listId`, `index` |

### **📊 Consultas**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/` | Listar todas as StatusLists | - |
| `GET` | `/{listId}` | Obter StatusList por ID | `listId` |
| `GET` | `/{listId}/json` | Obter StatusList como JSON | `listId` |
| `GET` | `/{listId}/metadata` | Obter metadados da StatusList | `listId` |
| `GET` | `/{listId}/status/{index}` | Verificar status de uma credencial | `listId`, `index` |

---

## **5. 💰 TRANSACTION CONTROLLER**
**Base Path: `/api/transactions`**

### **🔧 Criação e Envio**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `POST` | `/create` | Criar transação | `to`, `value`, `data` (opcional) |
| `POST` | `/send` | Enviar transação | `TxRequest` (body) |

### **📊 Consultas e Status**
| Método | Endpoint | Descrição | Parâmetros |
|--------|----------|-----------|------------|
| `GET` | `/{txHash}/status` | Obter status da transação | `txHash` |
| `GET` | `/{txHash}/receipt` | Obter recibo da transação | `txHash` |
| `POST` | `/estimate-gas` | Estimar gas da transação | `to`, `value`, `data` (opcional) |

---

## **📊 RESUMO ESTATÍSTICO**

### **Total de Endpoints: 47**
- **Wallet Controller**: 15 endpoints
- **Blockchain Controller**: 9 endpoints
- **Credential Controller**: 18 endpoints
- **StatusList Controller**: 8 endpoints
- **Transaction Controller**: 5 endpoints

### **Métodos HTTP Utilizados:**
- **GET**: 25 endpoints (53%)
- **POST**: 18 endpoints (38%)
- **PUT**: 3 endpoints (6%)
- **DELETE**: 1 endpoint (2%)

---

## **🔐 AUTENTICAÇÃO E SEGURANÇA**

### **⚠️ Endpoints de Desenvolvimento:**
- `/api/wallets/{address}/private-key/dev` - Chave privada descriptografada
- `/api/wallets/{address}/private-key/dev/encrypted` - Chave privada criptografada

### **🔑 Endpoints Administrativos:**
- `/api/blockchain/pause` - Pausar contrato
- `/api/blockchain/unpause` - Despausar contrato
- `/api/blockchain/grant-issuer-role/{walletAddress}` - Conceder roles

---

## **📱 EXEMPLOS DE USO**

### **Criar Wallet:**
```bash
POST /api/wallets?name=MinhaWallet&description=Wallet para testes
```

### **Verificar Role:**
```bash
GET /api/blockchain/check-role/0x1234...
```

### **Criar Credencial:**
```bash
POST /api/credentials
Content-Type: application/json

{
  "issuerDid": "did:web:exemplo.com",
  "holderDid": "did:ethr:0x1234...",
  "credentialType": "TestCredential"
}
```

### **Verificar Credencial:**
```bash
POST /api/credentials/verify
Content-Type: text/plain

eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## **🎯 FUNCIONALIDADES PRINCIPAIS**

1. **🏦 Gerenciamento de Wallets** - Criação, consulta e controle
2. **⛓️ Operações Blockchain** - Roles, DIDs, KYC e controle de contratos
3. **🎫 Credenciais Verificáveis** - Criação, verificação e revogação
4. **📋 Status Lists** - Controle de revogação de credenciais
5. **💰 Transações** - Criação, envio e monitoramento

**API completa para sistema de credenciais verificáveis baseado em blockchain! 🚀**
