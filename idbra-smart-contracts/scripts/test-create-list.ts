// scripts/test-create-list.ts
import { network } from "hardhat";
import { privateKeyToAccount } from "viem/accounts";
import { keccak256, toBytes } from "viem";

async function main() {
    console.log(">> Testando função publish do StatusListManager");

    // Conecta no Besu
    const connection = await network.connect({ network: "besu" });
    const { viem } = connection;

    const publicClient = await viem.getPublicClient();

    // Usar a chave privada da carteira administrativa
    const ADMIN_PK = process.env.ADMIN_PRIVATE_KEY as `0x${string}`;
    if (!ADMIN_PK) {
        throw new Error("Configure ADMIN_PRIVATE_KEY no .env");
    }

    const privateKey = ADMIN_PK.startsWith('0x') ? ADMIN_PK.slice(2) : ADMIN_PK;
    const adminAccount = privateKeyToAccount(`0x${privateKey}`);
    const walletClient = await viem.getWalletClient(adminAccount.address);

    console.log("Carteira administrativa:", adminAccount.address);

    // Endereço do contrato StatusListManager
    const STATUS_LIST_MANAGER = "0x68a185cab9607b9beb0b210bf7cc320f3b3a3efb" as `0x${string}`;
    console.log("StatusListManager:", STATUS_LIST_MANAGER);

    // Conectar ao contrato
    const slm = await viem.getContractAt("StatusListManager", STATUS_LIST_MANAGER);

    // Parâmetros da lista
    const listId = "teste-universitario-001";
    const version = 2n;
    const uri = "https://teste.com/status/001.json";
    const hash = "0x1d00c995c183fdf3aa5449e258081443aee6e59ff43a851b6e4584404057bcd6" as `0x${string}`;
    const size = 16384n; // Tamanho típico de StatusList
    const purpose = keccak256(toBytes("revocation")); // Hash específico para o purpose

    try {
        // Forçar gas legacy
        const request = {
            gasPrice: 0n,
            gas: 0x47b760n, // Gas limit padrão mais razoável
            type: "legacy" as const,
        };

        console.log("\n=== Criando StatusList ===");
        console.log({
            listId,
            uri,
            hash,
            size,
            purpose
        });

        const createListResult = await slm.write.createList(
            [
                listId,
                uri,
                hash,
                size,
                purpose
            ],
            {
                account: adminAccount, // Corrigido: passar o objeto adminAccount completo
                ...request
            }
        );

        console.log("TX Hash:", createListResult);
        const createListReceipt = await publicClient.waitForTransactionReceipt({ hash: createListResult });
        console.log("Status:", createListReceipt.status === 'success' ? '✅ Success' : '❌ Failed');

        if (createListReceipt.status === 'success') {
            console.log("\n=== Publicando atualização da StatusList ===");
            console.log({
                listId,
                version,
                uri,
                hash
            });

            // Chamar função publish com os 4 parâmetros corretos
            const tx = await slm.write.publish(
                [listId, version, uri, hash],
                {
                    account: adminAccount,
                    ...request,
                }
            );

            console.log("\nTX Hash:", tx);

            // Aguardar recibo
            const receipt = await publicClient.waitForTransactionReceipt({ hash: tx });
            console.log("Status:", receipt.status === "success" ? "✅ Success" : "❌ Failed");

            if (receipt.status === "success") {
                // Buscar eventos emitidos
                const events = await publicClient.getLogs({
                    address: STATUS_LIST_MANAGER,
                    event: {
                        type: "event",
                        name: "StatusListUpdated",
                        inputs: [
                            { name: "key", type: "bytes32", indexed: true },
                            { name: "listId", type: "string", indexed: false },
                            { name: "uri", type: "string", indexed: false },
                            { name: "hash", type: "bytes32", indexed: false },
                            { name: "version", type: "uint256", indexed: false },
                            { name: "issuer", type: "address", indexed: false }
                        ]
                    },
                    fromBlock: receipt.blockNumber,
                    toBlock: receipt.blockNumber
                });

                console.log("\nEventos emitidos:", events);
            }
        }
    } catch (error) {
        console.error("\n❌ Erro ao executar operações:", error);
    }
}

// Executar a função principal
main().catch((error) => {
    console.error("❌ Erro fatal:", error);
    process.exitCode = 1;
});
