# Custody Service - Desenvolvimento com Docker

Este projeto é um serviço de custódia para blockchain construído com Spring Boot e Web3j.

## 🚀 Executando com Docker

### Pré-requisitos
- Docker e Docker Compose instalados
- Nó RPC configurado e acessível

### Configuração Rápida

1. **Clone e configure:**
   ```bash
   cd idbra-custodia
   cp env.example .env
   ```

2. **Edite o arquivo `.env`:**
   ```bash
   # Sua URL RPC
   BESU_NODE_URL=http://seu-rpc-url:porta
   BESU_CHAIN_ID=1337
   ```

3. **Execute o projeto:**
   ```bash
   ./run-dev.sh
   ```

### Execução Manual

Se preferir executar manualmente:

```bash
# Construir e executar
docker compose up --build

# Executar em background
docker compose up -d --build

# Parar serviços
docker compose down
```

## 🏗️ Estrutura do Projeto

```
custody-service/
├─ pom.xml                    # Dependências Maven
├─ Dockerfile.dev            # Docker para desenvolvimento
├─ docker-compose.yml        # Orquestração dos serviços
├─ env.example               # Exemplo de variáveis de ambiente
├─ run-dev.sh                # Script de execução
└─ src/main/java/br/com/idhub/custody/
   ├─ CustodyApplication.java
   ├─ config/Web3Config.java
   ├─ domain/                # Entidades de domínio
   ├─ service/               # Lógica de negócio
   └─ web/                   # Controllers REST
```

## 🔧 Configurações

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|---------|
| `BESU_NODE_URL` | URL do nó RPC | `http://144.22.179.183` |
| `BESU_CHAIN_ID` | ID da chain | `1337` |
| `SPRING_PROFILES_ACTIVE` | Profile Spring | `dev` |
| `JAVA_OPTS` | Opções JVM | `-Xmx1g -Xms512m` |

### Portas

- **8082**: Aplicação Spring Boot
- **6380**: Redis (cache)

## 📡 APIs Disponíveis

### Carteiras
- `POST /api/wallets` - Criar carteira
- `GET /api/wallets/{address}` - Info da carteira
- `GET /api/wallets/{address}/balance` - Saldo formatado
- `GET /api/wallets/{address}/balance/raw` - Saldo em Wei

### Transações
- `POST /api/transactions/create` - Criar transação
- `POST /api/transactions/send` - Enviar transação
- `GET /api/transactions/{txHash}/status` - Status da transação
- `GET /api/transactions/{txHash}/receipt` - Receipt da transação

## 🐛 Debug e Logs

```bash
# Ver logs da aplicação
docker compose logs -f custody-service

# Ver logs do Redis
docker compose logs -f redis

# Executar com debug
docker compose up --build -e JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 🔍 Troubleshooting

### Problemas Comuns

1. **Porta já em uso:**
   ```bash
   # Verificar portas em uso
   netstat -tulpn | grep :8082

   # Parar processo
   docker compose down
   ```

2. **Erro de conexão RPC:**
   - Verifique se a URL RPC está correta no `.env`
   - Teste a conectividade: `curl -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' http://seu-rpc-url:porta`

3. **Erro de memória:**
   - Ajuste `JAVA_OPTS` no `.env`
   - Verifique recursos disponíveis: `docker stats`

## 🚀 Deploy

Para produção, use o `Dockerfile` padrão:

```bash
# Construir imagem de produção
docker build -t custody-service:latest .

# Executar
docker run -p 8080:8080 \
  -e BESU_NODE_URL=http://seu-rpc-prod \
  -e BESU_CHAIN_ID=1 \
  custody-service:latest
```
