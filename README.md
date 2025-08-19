# IDBRA DID - Sistema de Identidade Digital Descentralizada

## 🎯 Visão Geral

O IDBRA DID é um sistema completo de identidade digital descentralizada baseado em blockchain, implementando os padrões W3C DID (Decentralized Identifiers) e Verifiable Credentials. O sistema permite a emissão, verificação e revogação de credenciais digitais de forma segura e descentralizada.

## 🏗️ Arquitetura do Sistema

### Microsserviços

1. **🏦 Custody Service** (Porta 8082)
   - Gerenciamento de carteiras digitais
   - Interação com blockchain
   - Armazenamento seguro de chaves
   - Gestão de Status Lists

2. **📝 Issuer API** (Porta 8083)
   - Emissão de credenciais verificáveis
   - Revogação de credenciais
   - Proxy para operações de custódia

3. **✅ Verifier API** (Porta 8084)
   - Verificação de credenciais
   - Validação de assinaturas
   - Consulta de status de revogação

### 🔗 Smart Contracts

- **DIDRegistry**: Registro de DIDs na blockchain
- **StatusListManager**: Gerenciamento de listas de status para revogação
- **RegistryAccess**: Controle de acesso aos registros

## 🔄 Fluxo de Operações

### 1. 📝 Emissão de Credencial

1. **Criar Carteira** (Custody Service)
   ```http
   POST /api/wallets
