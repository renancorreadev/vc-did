
# 📄 Fluxo Completo de Credencial de Renda - IDBra

Este documento descreve o fluxo técnico completo para emissão, verificação e revogação de uma credencial de renda no ecossistema IDBra.

---

## 1. 🛠️ Importação e Configuração Inicial

- Importar wallet administrativa
- Criar wallet do emissor (Receita Federal)
- Conceder e verificar o `ISSUER_ROLE`

## 2. 🧾 Geração de Ofertas

- Holder solicita emissão de credencial com dados de renda
- Emissor visualiza e aprova a oferta

## 3. 👛 Criação de Wallet para o Holder

- Wallet do holder é criada com base na oferta aprovada

## 4. 🆔 Criação de DIDs

- DID do holder (did:ethr)
- DID do issuer (did:web)

## 5. 📋 Criação de StatusList

- Emissor registra StatusList (`revocation`) para controle de validade das credenciais emitidas

## 6. 🧾 Emissão da Credencial

- A Receita Federal emite uma `IncomeCredential` com dados de renda assinados
- Credencial gerada possui formato JWT (`jwsToken`) e ID `urn:uuid`

## 7. ✅ Verificação

- Validação da credencial (assinatura + status de revogação)
- Validação da existência no blockchain
- Consulta à `StatusList`

## 8. 🚫 Revogação

- Credencial pode ser revogada pelo issuer
- Após revogação:
  - `status` muda para `REVOKED`
  - `StatusList` é atualizada
  - Verificação da credencial passa a retornar erro

---

## 🔗 Padrões Utilizados

- **DID**: `did:web` e `did:ethr`
- **VC**: W3C Verifiable Credentials
- **StatusList2021**: para revogação de credenciais
- **JWT (JWS)**: representação compacta e assinada da credencial

---

## 📌 Observações

- O campo `jwsToken` é a credencial válida para verificação externa.
- O campo `credentialId` com prefixo `urn:uuid:` é usado para identificação única da VC no sistema.

---

Gerado automaticamente com base no fluxo de testes da API de custódia IDBra.
