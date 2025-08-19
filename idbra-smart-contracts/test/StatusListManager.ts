import assert from "node:assert/strict";
import { describe, it, beforeEach } from "node:test";
import { network } from "hardhat";
import { keccak256, toBytes, parseEther, getAddress } from "viem";

describe("StatusListManager", async function () {
    const { viem } = await network.connect();
    const publicClient = await viem.getPublicClient();

    let statusListManager: any;
    let admin: any;
    let issuer: any;
    let nonIssuer: any;

    beforeEach(async function () {
        // Deploy do contrato
        const accounts = await viem.getWalletClients();
        admin = accounts[0];
        issuer = accounts[1];
        nonIssuer = accounts[2];

        // Deploy StatusListManager com admin
        statusListManager = await viem.deployContract("StatusListManager", [admin.account.address]);

        // Conceder role ISSUER_ROLE para o issuer
        const ISSUER_ROLE = keccak256(toBytes("ISSUER_ROLE"));
        await statusListManager.write.grantRole([ISSUER_ROLE, issuer.account.address], {
            account: admin.account
        });
    });

    describe("Criação de StatusList", function () {
        it("Deve criar uma nova StatusList com sucesso", async function () {
            const listId = "teste-universitario-001";
            const uri = "https://teste.com/status/001.json";
            const hash = keccak256(toBytes("test-content"));
            const size = 1000n;
            const purpose = keccak256(toBytes("revocation"));

            // Criar StatusList
            const tx = statusListManager.write.createList([
                listId,
                uri,
                hash,
                size,
                purpose
            ], {
                account: issuer.account
            });

            // Verificar evento emitido - usando getAddress para normalizar
            await viem.assertions.emitWithArgs(
                tx,
                statusListManager,
                "StatusListCreated",
                [
                    keccak256(toBytes(listId)), // key
                    listId,
                    uri,
                    hash,
                    1n, // version inicial
                    size,
                    purpose,
                    getAddress(issuer.account.address) // Normalizar endereço
                ]
            );

            // Verificar se a lista foi criada corretamente
            const list = await statusListManager.read.getList([listId]);
            assert.equal(list.uri, uri);
            assert.equal(list.hash, hash);
            assert.equal(list.version, 1n);
            assert.equal(list.size, size);
            assert.equal(list.purpose, purpose);
            assert.equal(getAddress(list.issuer), getAddress(issuer.account.address));
            assert.equal(list.exists, true);
        });

        it("Deve falhar ao criar StatusList sem ISSUER_ROLE", async function () {
            const listId = "teste-sem-permissao";
            const uri = "https://teste.com/status/002.json";
            const hash = keccak256(toBytes("test-content"));
            const size = 1000n;
            const purpose = keccak256(toBytes("revocation"));

            // Tentar criar sem permissão deve falhar
            await assert.rejects(
                statusListManager.write.createList([
                    listId,
                    uri,
                    hash,
                    size,
                    purpose
                ], {
                    account: nonIssuer.account
                }),
                /AccessControlUnauthorizedAccount/
            );
        });

        it("Deve falhar ao criar StatusList duplicada", async function () {
            const listId = "teste-duplicado";
            const uri = "https://teste.com/status/003.json";
            const hash = keccak256(toBytes("test-content"));
            const size = 1000n;
            const purpose = keccak256(toBytes("revocation"));

            // Criar primeira vez
            await statusListManager.write.createList([
                listId,
                uri,
                hash,
                size,
                purpose
            ], {
                account: issuer.account
            });

            // Tentar criar novamente deve falhar
            await assert.rejects(
                statusListManager.write.createList([
                    listId,
                    uri,
                    hash,
                    size,
                    purpose
                ], {
                    account: issuer.account
                }),
                /ListAlreadyExists/
            );
        });
    });

    describe("Atualização de StatusList", function () {
        beforeEach(async function () {
            // Criar uma lista para os testes de atualização
            await statusListManager.write.createList([
                "lista-teste",
                "https://teste.com/status/inicial.json",
                keccak256(toBytes("initial-content")),
                1000n,
                keccak256(toBytes("revocation"))
            ], {
                account: issuer.account
            });
        });

        it("Deve atualizar StatusList com nova versão", async function () {
            const listId = "lista-teste";
            const newVersion = 2n;
            const newUri = "https://teste.com/status/v2.json";
            const newHash = keccak256(toBytes("updated-content"));

            const tx = statusListManager.write.publish([
                listId,
                newVersion,
                newUri,
                newHash
            ], {
                account: issuer.account
            });

            // Verificar evento - usando getAddress para normalizar
            await viem.assertions.emitWithArgs(
                tx,
                statusListManager,
                "StatusListUpdated",
                [
                    keccak256(toBytes(listId)),
                    listId,
                    newUri,
                    newHash,
                    newVersion,
                    getAddress(issuer.account.address) // Normalizar endereço
                ]
            );

            // Verificar atualização
            const list = await statusListManager.read.getList([listId]);
            assert.equal(list.uri, newUri);
            assert.equal(list.hash, newHash);
            assert.equal(list.version, newVersion);
        });

        it("Deve falhar ao atualizar com versão menor ou igual", async function () {
            const listId = "lista-teste";

            // Tentar com versão igual
            await assert.rejects(
                statusListManager.write.publish([
                    listId,
                    1n, // versão atual
                    "https://teste.com/status/invalid.json",
                    keccak256(toBytes("invalid-content"))
                ], {
                    account: issuer.account
                }),
                /InvalidVersion/
            );

            // Tentar com versão menor
            await assert.rejects(
                statusListManager.write.publish([
                    listId,
                    0n,
                    "https://teste.com/status/invalid.json",
                    keccak256(toBytes("invalid-content"))
                ], {
                    account: issuer.account
                }),
                /InvalidVersion/
            );
        });

        it("Deve falhar ao atualizar lista inexistente", async function () {
            await assert.rejects(
                statusListManager.write.publish([
                    "lista-inexistente",
                    2n,
                    "https://teste.com/status/inexistente.json",
                    keccak256(toBytes("content"))
                ], {
                    account: issuer.account
                }),
                /ListNotFound/
            );
        });

        it("Deve falhar ao atualizar sem ser o issuer", async function () {
            await assert.rejects(
                statusListManager.write.publish([
                    "lista-teste",
                    2n,
                    "https://teste.com/status/unauthorized.json",
                    keccak256(toBytes("unauthorized-content"))
                ], {
                    account: nonIssuer.account
                }),
                /NotIssuer/
            );
        });
    });

    describe("Transferência de Controle", function () {
        beforeEach(async function () {
            await statusListManager.write.createList([
                "lista-transferencia",
                "https://teste.com/status/transfer.json",
                keccak256(toBytes("transfer-content")),
                1000n,
                keccak256(toBytes("revocation"))
            ], {
                account: issuer.account
            });
        });

        it("Deve transferir controle da lista (apenas admin)", async function () {
            const listId = "lista-transferencia";
            const newIssuer = nonIssuer.account.address;

            const tx = statusListManager.write.transferListController([
                listId,
                newIssuer
            ], {
                account: admin.account
            });

            // Verificar evento - usando getAddress para normalizar
            await viem.assertions.emitWithArgs(
                tx,
                statusListManager,
                "StatusListControllerTransferred",
                [
                    keccak256(toBytes(listId)),
                    listId,
                    getAddress(issuer.account.address), // Normalizar endereços
                    getAddress(newIssuer)
                ]
            );

            // Verificar mudança
            const list = await statusListManager.read.getList([listId]);
            assert.equal(getAddress(list.issuer), getAddress(newIssuer));
        });

        it("Deve falhar transferência sem ser admin", async function () {
            await assert.rejects(
                statusListManager.write.transferListController([
                    "lista-transferencia",
                    nonIssuer.account.address
                ], {
                    account: issuer.account
                }),
                /AccessControlUnauthorizedAccount/
            );
        });
    });

    describe("Controle de Acesso e Pausa", function () {
        it("Deve pausar e despausar o contrato (apenas admin)", async function () {
            // Pausar
            await statusListManager.write.pause([], {
                account: admin.account
            });

            // Verificar que operações falham quando pausado
            await assert.rejects(
                statusListManager.write.createList([
                    "lista-pausada",
                    "https://teste.com/status/paused.json",
                    keccak256(toBytes("paused-content")),
                    1000n,
                    keccak256(toBytes("revocation"))
                ], {
                    account: issuer.account
                }),
                /EnforcedPause/
            );

            // Despausar
            await statusListManager.write.unpause([], {
                account: admin.account
            });

            // Verificar que operações funcionam novamente
            await statusListManager.write.createList([
                "lista-despausada",
                "https://teste.com/status/unpaused.json",
                keccak256(toBytes("unpaused-content")),
                1000n,
                keccak256(toBytes("revocation"))
            ], {
                account: issuer.account
            });
        });
    });

    describe("Cenário Completo - Fluxo do Backend", function () {
        it("Deve simular o fluxo completo da rota do backend", async function () {
            // Simular exatamente o que o backend está tentando fazer
            const listId = "teste-universitario-001";
            const uri = "https://teste.com/status/001.json";
            const purpose = "revocation";
            const hash = keccak256(toBytes("status-list-content"));
            const size = 16384n; // Tamanho típico de StatusList
            const purposeHash = keccak256(toBytes(purpose));

            console.log("=== Teste do Fluxo Backend ===");
            console.log(`ListId: ${listId}`);
            console.log(`URI: ${uri}`);
            console.log(`Purpose: ${purpose}`);
            console.log(`Issuer: ${issuer.account.address}`);

            // 1. Criar StatusList
            const createTx = await statusListManager.write.createList([
                listId,
                uri,
                hash,
                size,
                purposeHash
            ], {
                account: issuer.account
            });

            console.log(`Transação de criação: ${createTx}`);

            // 2. Verificar se foi criada - usando getAddress para normalizar
            const list = await statusListManager.read.getList([listId]);
            assert.equal(list.exists, true);
            assert.equal(list.uri, uri);
            assert.equal(list.purpose, purposeHash);
            assert.equal(getAddress(list.issuer), getAddress(issuer.account.address)); // Normalizar endereço
            assert.equal(list.version, 1n);

            console.log("✅ StatusList criada com sucesso!");
            console.log(`Versão: ${list.version}`);
            console.log(`Hash: ${list.hash}`);

            // 3. Atualizar StatusList (simular revogação)
            const newHash = keccak256(toBytes("updated-status-list-content"));
            const updateTx = await statusListManager.write.publish([
                listId,
                2n,
                uri,
                newHash
            ], {
                account: issuer.account
            });

            console.log(`Transação de atualização: ${updateTx}`);

            // 4. Verificar atualização
            const updatedList = await statusListManager.read.getList([listId]);
            assert.equal(updatedList.version, 2n);
            assert.equal(updatedList.hash, newHash);

            console.log("✅ StatusList atualizada com sucesso!");
            console.log(`Nova versão: ${updatedList.version}`);
        });
    });
});
