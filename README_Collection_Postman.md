# IDBra Verifiable Credentials - Collection Postman

## 📋 Descrição

Esta collection do Postman contém todos os fluxos de teste para credenciais verificáveis IDBra, organizados por tipo de credencial em pastas separadas. Cada pasta representa um fluxo completo de emissão de credenciais para diferentes entidades emissoras.

## 🏗️ Estrutura da Collection

### 1. Setup Inicial
- **Importar Wallet Administrativa**: Configuração inicial do sistema

### 2. Credenciais Telefônicas - TIM
- Criação de wallets (issuer e holder)
- Gerenciamento de DID
- Criação de StatusList
- Emissão de credencial telefônica
- Verificação e revogação
- **IP/Porta**: `147.93.11.54:8082`

### 3. Credenciais Educacionais - UFRJ
- Fluxo completo para credenciais educacionais
- **IP/Porta**: `147.93.11.54:8082`

### 4. Credenciais de Endereço - Correios
- Fluxo para credenciais de endereço residencial
- **IP/Porta**: `147.93.11.54:8082`

### 5. Credenciais Profissionais - CRM
- Fluxo para credenciais profissionais médicas
- **IP/Porta**: `147.93.11.54:8082`

### 6. Credenciais de Renda - Receita Federal
- Fluxo para credenciais de comprovante de renda
- **IP/Porta**: `147.93.11.54:8082`

### 7. Credenciais Telefônicas - VIVO
- Fluxo alternativo para credenciais telefônicas
- **IP/Porta**: `147.93.11.54:8082`

### 8. Utilitários - Wallet
- Consultas gerais de wallets
- **IP/Porta**: `147.93.11.54:8082`

## 🚀 Como Usar

### 1. Importar a Collection
1. Abra o Postman
2. Clique em "Import"
3. Selecione o arquivo `IDBra_VC_Collection.postman_collection.json`

### 2. Configurar Variáveis
A collection já está configurada com as seguintes variáveis:
- `base_url`: `http://147.93.11.54:8082`
- `admin_wallet_address`: `0x905126b37bd5087319e61d8dc633208c183ace67`

### 3. Executar os Fluxos
Cada pasta pode ser executada independentemente:

#### Fluxo Básico (para cada tipo de credencial):
1. **Setup Inicial**: Importar wallet administrativa
2. **Criar Wallets**: Issuer e holder separados
3. **Verificar/Grant ISSUER_ROLE**: Para a wallet do issuer
4. **Criar DIDs**: Para issuer e holder
5. **Criar StatusList**: Base para as credenciais
6. **Emitir Credencial**: Teste principal
7. **Verificar Credencial no Blockchain**: Validação no blockchain
8. **Verificar Credencial no Sistema**: Validação no sistema
9. **Verificar Status de Revogação**: Status atual da credencial
10. **Verificar Credencial com JWT**: Validação usando JWT retornado
11. **Verificar StatusList Após Emissão**: Monitoramento da StatusList
12. **Revogar Credencial**: Teste de revogação
13. **Verificar Credencial Revogada**: Validação após revogação
14. **Verificar StatusList Após Revogação**: Monitoramento pós-revogação
15. **Verificar Status de Revogação Final**: Confirmação da revogação

## 🔧 Endpoints Principais

### Wallets
- `POST /api/wallets` - Criar wallet
- `GET /api/wallets/{address}` - Verificar wallet
- `GET /api/wallets` - Listar todas as wallets

### Blockchain
- `GET /api/blockchain/check-role/{address}` - Verificar role
- `POST /api/blockchain/grant-issuer-role/{address}` - Conceder ISSUER_ROLE
- `POST /api/blockchain/did/create` - Criar DID

### StatusList
- `POST /api/statuslist` - Criar StatusList
- `GET /api/statuslist` - Listar StatusLists
- `GET /api/statuslist/{listId}/metadata` - Metadados da StatusList

### Credenciais
- `POST /api/credentials` - Criar credencial
- `GET /api/credentials/{id}` - Verificar credencial
- `GET /api/credentials/{id}/exists` - Verificar no blockchain
- `POST /api/credentials/{id}/revoke` - Revogar credencial
- `POST /api/credentials/verify` - Verificar com JWT

## 📊 Dados de Teste

### Credenciais Telefônicas (TIM)
- **Issuer**: TIM S.A.
- **Holder**: Maria Oliveira Costa
- **Tipo**: PhoneLineCredential
- **Plano**: TIM Controle 20GB

### Credenciais Educacionais (UFRJ)
- **Issuer**: Universidade Federal do Rio de Janeiro
- **Holder**: Pedro Henrique Costa
- **Tipo**: EducationCredential
- **Curso**: Engenharia de Computação

### Credenciais de Endereço (Correios)
- **Issuer**: Correios
- **Holder**: João (Cliente)
- **Tipo**: AddressCredential
- **Endereço**: Rua das Flores, 123 - Copacabana/RJ

### Credenciais Profissionais (CRM)
- **Issuer**: Conselho Regional de Medicina
- **Holder**: Dra. Ana Carolina Santos
- **Tipo**: ProfessionalCredential
- **Especialidade**: Cardiologia

### Credenciais de Renda (Receita Federal)
- **Issuer**: Receita Federal
- **Holder**: Lucia Fernanda Oliveira
- **Tipo**: IncomeCredential
- **Renda Anual**: R$ 120.000,00

### Credenciais Telefônicas (VIVO)
- **Issuer**: VIVO S.A.
- **Holder**: Carlos Eduardo Silva
- **Tipo**: PhoneLineCredential
- **Plano**: VIVO Família 50GB

## ⚠️ Observações Importantes

1. **IP e Porta**: Todos os endpoints usam `147.93.11.54:8082`
2. **Sequência**: Execute os requests na ordem apresentada em cada pasta
3. **Dados Únicos**: Cada wallet tem endereços únicos para evitar conflitos
4. **JWT**: As credenciais são retornadas como JWT para verificação
5. **StatusList**: Cada tipo de credencial tem sua própria StatusList

## 🔍 Troubleshooting

### Erro de Wallet já existente
- Use endereços diferentes para cada teste
- Ou delete a wallet existente antes de recriar

### Erro de ISSUER_ROLE
- Execute primeiro o request de grant ISSUER_ROLE
- Verifique se a wallet tem permissões administrativas

### Erro de DID já existente
- Cada wallet deve ter um DID único
- Use endereços diferentes para cada teste

## 📝 Logs e Monitoramento

A collection inclui requests para:
- Verificar status de credenciais
- Monitorar StatusLists
- Consultar metadados
- Validar revogações

## 🎯 Casos de Uso

Esta collection é ideal para:
- **Desenvolvedores**: Testar integrações com a API IDBra
- **QA**: Validar fluxos completos de credenciais
- **DevOps**: Monitorar endpoints e funcionalidades
- **Arquitetos**: Entender o fluxo completo do sistema

## 📞 Suporte

Para dúvidas sobre a collection ou problemas de execução, consulte a documentação da API IDBra ou entre em contato com a equipe de desenvolvimento.
