#!/bin/bash

echo "🚀 Iniciando custody-service em modo desenvolvimento..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando. Por favor, inicie o Docker primeiro."
    exit 1
fi

# Verificar se o arquivo .env existe, se não, criar baseado no exemplo
if [ ! -f .env ]; then
    echo "📝 Arquivo .env não encontrado. Criando baseado no env.example..."
    cp env.example .env
    echo "⚠️  Por favor, edite o arquivo .env com suas configurações de RPC antes de continuar."
    echo "   - BESU_NODE_URL: URL do seu nó RPC"
    echo "   - BESU_CHAIN_ID: ID da chain"
    read -p "Pressione Enter após configurar o .env..."
fi

# Carregar variáveis do .env
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "🔧 Configurações:"
echo "   RPC URL: ${BESU_NODE_URL:-http://144.22.179.183}"
echo "   Chain ID: ${BESU_CHAIN_ID:-1337}"
echo "   Profile: ${SPRING_PROFILES_ACTIVE:-dev}"

# Construir e executar
echo "🏗️  Construindo e executando o projeto..."
docker compose up --build

echo "✅ Projeto iniciado! Acesse: http://localhost:8082"
