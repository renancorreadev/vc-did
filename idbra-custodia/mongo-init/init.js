// Script de inicialização do MongoDB para IDBra Custody Service
// Este script é executado automaticamente quando o container MongoDB é criado pela primeira vez

print('🚀 Inicializando MongoDB para IDBra Custody Service...');

// Conectar ao banco admin
db = db.getSiblingDB('admin');

// Criar usuário root se não existir
if (!db.getUser("admin")) {
    print('👤 Criando usuário root...');
    db.createUser({
        user: "admin",
        pwd: "admin123",
        roles: [
            { role: "userAdminAnyDatabase", db: "admin" },
            { role: "readWriteAnyDatabase", db: "admin" },
            { role: "dbAdminAnyDatabase", db: "admin" }
        ]
    });
    print('✅ Usuário root criado com sucesso!');
} else {
    print('ℹ️ Usuário root já existe');
}

// Conectar ao banco custody
db = db.getSiblingDB('custody');

// Criar coleções principais
print('📋 Criando coleções principais...');

// Coleção para wallets
db.createCollection('wallets');
db.wallets.createIndex({ "address": 1 }, { unique: true });
db.wallets.createIndex({ "name": 1 });
print('✅ Coleção wallets criada com índices');

// Coleção para credenciais
db.createCollection('credentials');
db.credentials.createIndex({ "credentialId": 1 }, { unique: true });
db.credentials.createIndex({ "issuerDid": 1 });
db.credentials.createIndex({ "holderDid": 1 });
db.credentials.createIndex({ "status": 1 });
print('✅ Coleção credentials criada com índices');

// Coleção para status lists
db.createCollection('statuslists');
db.statuslists.createIndex({ "listId": 1 }, { unique: true });
db.statuslists.createIndex({ "issuer": 1 });
print('✅ Coleção statuslists criada com índices');

// Coleção para transações
db.createCollection('transactions');
db.transactions.createIndex({ "txHash": 1 }, { unique: true });
db.transactions.createIndex({ "status": 1 });
print('✅ Coleção transactions criada com índices');

// Coleção para métricas do sistema
db.createCollection('system_metrics');
db.system_metrics.createIndex({ "timestamp": 1 });
print('✅ Coleção system_metrics criada com índices');

// Inserir dados de exemplo para desenvolvimento
if (db.wallets.countDocuments() === 0) {
    print('📝 Inserindo dados de exemplo...');

    // Wallet administrativa de exemplo
    db.wallets.insertOne({
        address: "0x905126b37bd5087319e61d8dc633208c183ace67",
        name: "Admin Wallet",
        description: "Wallet administrativa para operações de smart contract",
        createdAt: new Date(),
        updatedAt: new Date(),
        active: true,
        isAdmin: true
    });

    print('✅ Dados de exemplo inseridos');
} else {
    print('ℹ️ Dados de exemplo já existem');
}

// Criar usuário específico para o banco custody
db = db.getSiblingDB('admin');
if (!db.getUser("custody_user")) {
    print('👤 Criando usuário específico para custody...');
    db.createUser({
        user: "custody_user",
        pwd: "custody123",
        roles: [
            { role: "readWrite", db: "custody" },
            { role: "dbAdmin", db: "custody" }
        ]
    });
    print('✅ Usuário custody criado com sucesso!');
} else {
    print('ℹ️ Usuário custody já existe');
}

print('🎉 MongoDB inicializado com sucesso para IDBra Custody Service!');
print('📊 Banco: custody');
print('🔐 Usuário: admin / custody_user');
print('🌐 Porta: 27017');
