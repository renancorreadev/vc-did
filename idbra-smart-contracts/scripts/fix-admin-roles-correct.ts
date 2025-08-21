// scripts/fix-admin-roles-correct.ts
import { network } from "hardhat";
import { privateKeyToAccount } from "viem/accounts";

async function main() {
    console.log(">> Verificando e corrigindo permissões no IDBraDIDRegistry");

    // Conecta no Besu
    const connection = await network.connect({ network: "besu" });
    const { viem } = connection;

    const publicClient = await viem.getPublicClient();

    // Usar a chave privada da carteira administrativa (que fez o deploy)
    const ADMIN_PK = process.env.ADMIN_PRIVATE_KEY as `0x${string}`;
    if (!ADMIN_PK) {
        throw new Error("Configure ADMIN_PRIVATE_KEY no .env (chave da carteira administrativa)");
    }

    const privateKey = ADMIN_PK.startsWith('0x') ? ADMIN_PK.slice(2) : ADMIN_PK;
    const adminAccount = privateKeyToAccount(`0x${privateKey}`);

    console.log("Carteira administrativa (deployer):", adminAccount.address);

    // Endereço do contrato único IDBraDIDRegistry
    const IDBRA_DID_REGISTRY = "0xc47a675198759Cf712a53Bb4a7EDbC33bb799285" as `0x${string}`;

    console.log("IDBraDIDRegistry:", IDBRA_DID_REGISTRY);

    // Conectar ao contrato
    const didRegistry = await viem.getContractAt("IDBraDIDRegistry", IDBRA_DID_REGISTRY);

    // Roles definidas no contrato
    const DEFAULT_ADMIN_ROLE = "0x0000000000000000000000000000000000000000000000000000000000000000" as `0x${string}`;
    const REGISTRAR_ROLE = await didRegistry.read.REGISTRAR_ROLE();
    const ISSUER_ROLE = await didRegistry.read.ISSUER_ROLE();
    const AUDITOR_ROLE = await didRegistry.read.AUDITOR_ROLE();
    const EMERGENCY_ROLE = await didRegistry.read.EMERGENCY_ROLE();

    console.log("\n=== Verificando Permissões Atuais ===");

    // Verificar se a carteira administrativa tem as permissões
    const adminHasAdmin = await didRegistry.read.hasRole([DEFAULT_ADMIN_ROLE, adminAccount.address]);
    const adminHasRegistrar = await didRegistry.read.hasRole([REGISTRAR_ROLE, adminAccount.address]);
    const adminHasIssuer = await didRegistry.read.hasRole([ISSUER_ROLE, adminAccount.address]);
    const adminHasAuditor = await didRegistry.read.hasRole([AUDITOR_ROLE, adminAccount.address]);
    const adminHasEmergency = await didRegistry.read.hasRole([EMERGENCY_ROLE, adminAccount.address]);

    console.log(`Admin wallet tem DEFAULT_ADMIN_ROLE: ${adminHasAdmin}`);
    console.log(`Admin wallet tem REGISTRAR_ROLE: ${adminHasRegistrar}`);
    console.log(`Admin wallet tem ISSUER_ROLE: ${adminHasIssuer}`);
    console.log(`Admin wallet tem AUDITOR_ROLE: ${adminHasAuditor}`);
    console.log(`Admin wallet tem EMERGENCY_ROLE: ${adminHasEmergency}`);

    if (adminHasAdmin && adminHasRegistrar && adminHasIssuer && adminHasAuditor && adminHasEmergency) {
        console.log("\n✅ A carteira administrativa já possui todas as permissões necessárias!");
        console.log("Os endpoints Java devem funcionar corretamente.");
        return;
    }

    console.log("\n=== Concedendo Permissões Faltantes ===");

    // Array de roles para conceder
    const rolesToGrant = [
        { name: "DEFAULT_ADMIN_ROLE", role: DEFAULT_ADMIN_ROLE, hasRole: adminHasAdmin },
        { name: "REGISTRAR_ROLE", role: REGISTRAR_ROLE, hasRole: adminHasRegistrar },
        { name: "ISSUER_ROLE", role: ISSUER_ROLE, hasRole: adminHasIssuer },
        { name: "AUDITOR_ROLE", role: AUDITOR_ROLE, hasRole: adminHasAuditor },
        { name: "EMERGENCY_ROLE", role: EMERGENCY_ROLE, hasRole: adminHasEmergency }
    ];

    for (const roleInfo of rolesToGrant) {
        if (!roleInfo.hasRole) {
            console.log(`Concedendo ${roleInfo.name}...`);

            try {
                const tx = await didRegistry.write.grantRole([roleInfo.role, adminAccount.address], {
                    account: adminAccount.address,
                    gas: 100000n,
                });

                console.log(`TX ${roleInfo.name}: ${tx}`);
                const receipt = await publicClient.waitForTransactionReceipt({ hash: tx });
                console.log(`Status: ${receipt.status === 'success' ? '✅ Success' : '❌ Failed'}`);
            } catch (error) {
                console.error(`❌ Erro ao conceder ${roleInfo.name}:`, error);
            }
        }
    }

    console.log("\n=== Verificação Final ===");

    // Aguardar um pouco para sincronização
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Verificar novamente
    const finalAdminHasAdmin = await didRegistry.read.hasRole([DEFAULT_ADMIN_ROLE, adminAccount.address]);
    const finalAdminHasRegistrar = await didRegistry.read.hasRole([REGISTRAR_ROLE, adminAccount.address]);
    const finalAdminHasIssuer = await didRegistry.read.hasRole([ISSUER_ROLE, adminAccount.address]);
    const finalAdminHasAuditor = await didRegistry.read.hasRole([AUDITOR_ROLE, adminAccount.address]);
    const finalAdminHasEmergency = await didRegistry.read.hasRole([EMERGENCY_ROLE, adminAccount.address]);

    console.log(`✅ Admin wallet tem DEFAULT_ADMIN_ROLE: ${finalAdminHasAdmin}`);
    console.log(`✅ Admin wallet tem REGISTRAR_ROLE: ${finalAdminHasRegistrar}`);
    console.log(`✅ Admin wallet tem ISSUER_ROLE: ${finalAdminHasIssuer}`);
    console.log(`✅ Admin wallet tem AUDITOR_ROLE: ${finalAdminHasAuditor}`);
    console.log(`✅ Admin wallet tem EMERGENCY_ROLE: ${finalAdminHasEmergency}`);

    if (finalAdminHasAdmin && finalAdminHasRegistrar && finalAdminHasIssuer && finalAdminHasAuditor && finalAdminHasEmergency) {
        console.log("\n🎉 Todas as permissões estão configuradas corretamente!");
        console.log("Agora você pode testar os endpoints Java:");
        console.log("- POST /api/blockchain/grant-issuer-role/{walletAddress}");
        console.log("- GET /api/blockchain/check-role/{walletAddress}");
        console.log("- POST /api/credentials/revoke (deve funcionar agora)");
    } else {
        console.log("\n❌ Ainda há problemas com as permissões.");
        console.log("Pode ser necessário investigar a implementação do contrato.");
    }

    // Verificar também quem mais tem essas roles
    console.log("\n=== Investigando Outras Contas com Roles ===");

    try {
        // Buscar eventos RoleGranted para DEFAULT_ADMIN_ROLE
        const adminRoleEvents = await publicClient.getLogs({
            address: IDBRA_DID_REGISTRY,
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
            console.log(`- Conta: ${event.args.account} (concedido por: ${event.args.sender})`);
        }
    } catch (error) {
        console.error("Erro ao buscar eventos:", error);
    }
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
