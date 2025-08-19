#!/bin/bash

echo "🚀 Iniciando custody-service em modo desenvolvimento otimizado..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando. Por favor, inicie o Docker primeiro."
    exit 1
fi

# Criar diretório de dados se não existir
mkdir -p ./data

# Verificar se o arquivo .env existe
if [ ! -f .env ]; then
    echo "📝 Arquivo .env não encontrado. Criando baseado no env.example..."
    cp env.example .env
    echo "⚠️  Por favor, edite o arquivo .env com suas configurações."
fi

# Carregar variáveis do .env
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "🔧 Configurações:"
echo "   RPC URL: ${BESU_NODE_URL:-http://144.22.179.183}"
echo "   Chain ID: ${BESU_CHAIN_ID:-1337}"
echo "   Profile: ${SPRING_PROFILES_ACTIVE:-dev}"
echo "   Banco: Arquivo persistente em ./data/"

# Verificar se já existe um container rodando
if [ "$(docker ps -q -f name=custody-service-dev)" ]; then
    echo "🔄 Container já está rodando. Reiniciando apenas o serviço..."
    docker compose restart custody-service
else
    echo "🏗️  Iniciando containers..."
    docker compose up -d
fi

echo "✅ Projeto iniciado com hot reload!"
echo "   - API: http://localhost:8082"
echo "   - H2 Console: http://localhost:8082/h2-console"
echo "   - Banco persistente em: ./data/custodydb.mv.db"
echo ""
echo "💡 Agora você pode editar arquivos .java e eles serão recarregados automaticamente!"
echo "   Para ver logs: docker compose logs -f custody-service"
