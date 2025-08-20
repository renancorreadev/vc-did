import { network } from "hardhat";
import { privateKeyToAccount } from "viem/accounts";

// Conecta no Besu (nao precisa chainType para Besu)
const connection = await network.connect({ network: "besu" });
const { viem } = connection;

console.log(">> Configurando roles usando viem na rede:", connection.networkName);

const publicClient = await viem.getPublicClient();

// *** IMPORTANTE ***: usar conta LOCAL (assinatura off-line)
// para evitar eth_sendTransaction (nao suportado pelo Besu)
const PK = process.env.BESU_PRIVATE_KEY as `0x${string}`;
if (!PK) throw new Error("Defina BESU_PRIVATE_KEY no .env");

// Converter string para bytes (remover 0x se existir)
const privateKey = PK.startsWith('0x') ? PK.slice(2) : PK;
const account = privateKeyToAccount(`0x${privateKey}`);
const walletClient = await viem.getWalletClient(account.address);

console.log("Admin:", account.address);

// Opcional: forcar gas legacy se sua rede nao usa 1559
const request = {
    gasPrice: 0n,
    gas: 0x47b760n, // Gas limit padrão mais razoável
    type: "legacy" as const,
};

// Endereço do novo contrato IDBraDIDRegistry deployado
const IDBRA_DID_REGISTRY = "0x34c2AcC42882C0279A64bB1a4B1083D483BdE886" as `0x${string}`;

// Endereço que receberá todas as roles
const TARGET_ADDRESS = "0xFE3B557E8Fb62b89F4916B721be55cEb828dBd73" as `0x${string}`;

// ---------- Configurar IDBraDIDRegistry ----------
console.log("\nConfigurando IDBraDIDRegistry...");
const didRegistry = await viem.getContractAt("IDBraDIDRegistry", IDBRA_DID_REGISTRY);

// Obter as roles direto do contrato
const REGISTRAR_ROLE = await didRegistry.read.REGISTRAR_ROLE();
const ISSUER_ROLE = await didRegistry.read.ISSUER_ROLE();
const AUDITOR_ROLE = await didRegistry.read.AUDITOR_ROLE();

console.log("Grant REGISTRAR_ROLE ->", TARGET_ADDRESS);
const txGrantRegistrar = await didRegistry.write.grantRole([REGISTRAR_ROLE, TARGET_ADDRESS], {
    account: account.address,
    ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantRegistrar });

console.log("Grant ISSUER_ROLE ->", TARGET_ADDRESS);
const txGrantIssuer = await didRegistry.write.grantRole([ISSUER_ROLE, TARGET_ADDRESS], {
    account: account.address,
    ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantIssuer });

console.log("Grant AUDITOR_ROLE ->", TARGET_ADDRESS);
const txGrantAuditor = await didRegistry.write.grantRole([AUDITOR_ROLE, TARGET_ADDRESS], {
    account: account.address,
    ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantAuditor });

console.log("\n✅ Configuração concluída");
console.log({
    network: connection.networkName,
    admin: account.address,
    IDBraDIDRegistry: IDBRA_DID_REGISTRY,
    targetAddress: TARGET_ADDRESS,
    roles: {
        REGISTRAR_ROLE,
        ISSUER_ROLE,
        AUDITOR_ROLE
    }
});
