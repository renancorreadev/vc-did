// scripts/fix-admin-roles-correct.ts
import { network } from "hardhat";
import { privateKeyToAccount } from "viem/accounts";

async function main() {
    console.log(">> Verificando e corrigindo permissões com endereços corretos");

    // Conecta no Besu
    const connection = await network.connect({ network: "besu" });
    const { viem } = connection;

    const publicClient = await viem.getPublicClient();

    // Usar a chave privada da carteira administrativa (que fez o deploy)
    const ADMIN_PK = process.env.ADMIN_PRIVATE_KEY as `0x${string}`;
    if (!ADMIN_PK) {
        throw new Error("Configure ADMIN_PRIVATE_KEY no .env (chave da carteira 0xFE3B557E8Fb62b89F4916B721be55cEb828dBd73)");
    }

    const privateKey = ADMIN_PK.startsWith('0x') ? ADMIN_PK.slice(2) : ADMIN_PK;
    const adminAccount = privateKeyToAccount(`0x${privateKey}`);

    console.log("Carteira administrativa (deployer):", adminAccount.address);

    // Endereços corretos dos contratos
    const DID_REGISTRY = "0x8553c57aC9a666EAfC517Ffc4CF57e21d2D3a1cb" as `0x${string}`;
    const STATUS_LIST_MANAGER = "0x93a284C91768F3010D52cD37f84f22c5052be40b" as `0x${string}`;

    console.log("DIDRegistry:", DID_REGISTRY);
    console.log("StatusListManager:", STATUS_LIST_MANAGER);

    // Conectar aos contratos
    const slm = await viem.getContractAt("StatusListManager", STATUS_LIST_MANAGER);

    // Roles
    const DEFAULT_ADMIN_ROLE = "0x0000000000000000000000000000000000000000000000000000000000000000" as `0x${string}`;
    const ISSUER_ROLE = await slm.read.ISSUER_ROLE();

    console.log("\n=== Verificando Permissões Atuais ===");

    // Verificar se a carteira administrativa tem as permissões
    const adminHasAdmin = await slm.read.hasRole([DEFAULT_ADMIN_ROLE, adminAccount.address]);
    const adminHasIssuer = await slm.read.hasRole([ISSUER_ROLE, adminAccount.address]);

    console.log(`Admin wallet tem DEFAULT_ADMIN_ROLE: ${adminHasAdmin}`);
    console.log(`Admin wallet tem ISSUER_ROLE: ${adminHasIssuer}`);

    if (adminHasAdmin && adminHasIssuer) {
        console.log("\n✅ A carteira administrativa já possui todas as permissões necessárias!");
        console.log("Os endpoints Java devem funcionar corretamente.");
        return;
    }

    console.log("\n=== Concedendo Permissões Faltantes ===");

    // Se a carteira não tem DEFAULT_ADMIN_ROLE, pode ser um problema
    if (!adminHasAdmin) {
        console.log("⚠️  Carteira não tem DEFAULT_ADMIN_ROLE. Tentando auto-concessão...");

        try {
            const txAdmin = await slm.write.grantRole([DEFAULT_ADMIN_ROLE, adminAccount.address], {
                account: adminAccount.address,
                gas: 100000n,
            });

            console.log(`TX DEFAULT_ADMIN_ROLE: ${txAdmin}`);
            const receiptAdmin = await publicClient.waitForTransactionReceipt({ hash: txAdmin });
            console.log(`Status: ${receiptAdmin.status === 'success' ? '✅ Success' : '❌ Failed'}`);
        } catch (error) {
            console.error("❌ Erro ao conceder DEFAULT_ADMIN_ROLE:", error);
        }
    }

    // Conceder ISSUER_ROLE se necessário
    if (!adminHasIssuer) {
        console.log("Concedendo ISSUER_ROLE...");

        try {
            const txIssuer = await slm.write.grantRole([ISSUER_ROLE, adminAccount.address], {
                account: adminAccount.address,
                gas: 100000n,
            });

            console.log(`TX ISSUER_ROLE: ${txIssuer}`);
            const receiptIssuer = await publicClient.waitForTransactionReceipt({ hash: txIssuer });
            console.log(`Status: ${receiptIssuer.status === 'success' ? '✅ Success' : '❌ Failed'}`);
        } catch (error) {
            console.error("❌ Erro ao conceder ISSUER_ROLE:", error);
        }
    }

    console.log("\n=== Verificação Final ===");

    // Aguardar um pouco para sincronização
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Verificar novamente
    const finalAdminHasAdmin = await slm.read.hasRole([DEFAULT_ADMIN_ROLE, adminAccount.address]);
    const finalAdminHasIssuer = await slm.read.hasRole([ISSUER_ROLE, adminAccount.address]);

    console.log(`✅ Admin wallet tem DEFAULT_ADMIN_ROLE: ${finalAdminHasAdmin}`);
    console.log(`✅ Admin wallet tem ISSUER_ROLE: ${finalAdminHasIssuer}`);

    if (finalAdminHasAdmin && finalAdminHasIssuer) {
        console.log("\n🎉 Todas as permissões estão configuradas corretamente!");
        console.log("Agora você pode testar os endpoints Java:");
        console.log("- POST /api/blockchain/grant-issuer-role/{walletAddress}");
        console.log("- GET /api/blockchain/check-role/{walletAddress}");
    } else {
        console.log("\n❌ Ainda há problemas com as permissões.");
        console.log("Pode ser necessário investigar a implementação do contrato.");
    }

    // Verificar também quem mais tem essas roles
    console.log("\n=== Investigando Outras Contas com Roles ===");

    try {
        // Buscar eventos RoleGranted para DEFAULT_ADMIN_ROLE
        const adminRoleEvents = await publicClient.getLogs({
            address: STATUS_LIST_MANAGER,
            event: {
                type: 'event',
                name: 'RoleGranted',
                inputs: [
                    { name: 'role', type: 'bytes32', indexed: true },
                    { name: 'account', type: 'address', indexed: true },
                    { name: 'sender', type: 'address', indexed: true }
                ]
            },
            args: {
                role: DEFAULT_ADMIN_ROLE
            },
            fromBlock: 'earliest',
            toBlock: 'latest'
        });

        console.log(`Encontrados ${adminRoleEvents.length} eventos de concessão de DEFAULT_ADMIN_ROLE`);

        for (const event of adminRoleEvents) {
            console.log(`  - Conta: ${event.args.account} (concedido por: ${event.args.sender})`);
        }

    } catch (error) {
        console.log("⚠️  Não foi possível buscar eventos históricos (limitação do RPC)");
    }
}

// Executar a função principal
main().catch((error) => {
    console.error("❌ Erro fatal:", error);
    process.exitCode = 1;
});
