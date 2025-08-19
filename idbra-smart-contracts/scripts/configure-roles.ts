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

// Enderecos dos contratos deployados
const DID_REGISTRY = "0x8553c57aC9a666EAfC517Ffc4CF57e21d2D3a1cb" as `0x${string}`;
const STATUS_LIST_MANAGER = "0x93a284C91768F3010D52cD37f84f22c5052be40b" as `0x${string}`;

// Enderecos de roles (defina no .env ou use o mesmo do admin)
const REGISTRAR = (process.env.REGISTRAR_ADDRESS ?? account.address) as `0x${string}`;
const ISSUER = (process.env.ISSUER_ADDRESS ?? account.address) as `0x${string}`;

// ---------- Configurar DIDRegistry ----------
console.log("\nConfigurando DIDRegistry...");
const did = await viem.getContractAt("DIDRegistry", DID_REGISTRY);

// REGISTRAR_ROLE direto do contrato
const registrarRole = await did.read.REGISTRAR_ROLE();
console.log("Grant REGISTRAR_ROLE ->", REGISTRAR);
const txGrantRegistrar = await did.write.grantRole([registrarRole, REGISTRAR], {
    account: account.address,
    ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantRegistrar });

// ---------- Configurar StatusListManager ----------
console.log("\nConfigurando StatusListManager...");
const slm = await viem.getContractAt("StatusListManager", STATUS_LIST_MANAGER);

// ISSUER_ROLE direto do contrato
const issuerRole = await slm.read.ISSUER_ROLE();
console.log("Grant ISSUER_ROLE ->", ISSUER);
const txGrantIssuer = await slm.write.grantRole([issuerRole, ISSUER], {
    account: account.address,
    ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantIssuer });

console.log("\n✅ Configuração concluída");
console.log({
    network: connection.networkName,
    admin: account.address,
    DIDRegistry: DID_REGISTRY,
    StatusListManager: STATUS_LIST_MANAGER,
    REGISTRAR,
    ISSUER,
});
