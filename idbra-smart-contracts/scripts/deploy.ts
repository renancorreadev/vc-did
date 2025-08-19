// scripts/deploy-besu.ts
import { network } from "hardhat";
import { privateKeyToAccount } from "viem/accounts";

// Conecta no Besu (nao precisa chainType para Besu)
const connection = await network.connect({ network: "besu" });
const { viem } = connection;

console.log(">> Deploy usando viem na rede:", connection.networkName);

const publicClient = await viem.getPublicClient();

// *** IMPORTANTe ***: usar conta LOCAL (assinatura off-line)
// para evitar eth_sendTransaction (nao suportado pelo Besu)
const PK = process.env.BESU_PRIVATE_KEY as `0x${string}`;
if (!PK) throw new Error("Defina BESU_PRIVATE_KEY no .env");

// Converter string para bytes (remover 0x se existir)
const privateKey = PK.startsWith('0x') ? PK.slice(2) : PK;
const account = privateKeyToAccount(`0x${privateKey}`);
const walletClient = await viem.getWalletClient(account.address);

console.log("Deployer:", account.address);

// Opcional: forcar gas legacy se sua rede nao usa 1559
const request = {
  gasPrice: 0n,
  gas: 0x47b760n, // Gas limit padrão mais razoável
  type: "legacy" as const,
};

// Enderecos de roles (fallback = deployer)
const ADMIN = (process.env.ADMIN_ADDRESS ?? account.address) as `0x${string}`;
const REGISTRAR = (process.env.REGISTRAR_ADDRESS ?? account.address) as `0x${string}`;
const ISSUER = (process.env.ISSUER_ADDRESS ?? account.address) as `0x${string}`;

// ---------- Deploy DIDRegistry ----------
console.log("\nDeploying DIDRegistry...");
const did = await viem.deployContract("DIDRegistry", [ADMIN], {
  client: { wallet: walletClient, public: publicClient },
  ...request,
});
console.log("DIDRegistry:", did.address);

// REGISTRAR_ROLE direto do contrato
const registrarRole = await did.read.REGISTRAR_ROLE();
console.log("Grant REGISTRAR_ROLE ->", REGISTRAR);
const txGrantRegistrar = await did.write.grantRole([registrarRole, REGISTRAR], {
  account: account.address,
  ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantRegistrar });

// ---------- Deploy StatusListManager ----------
console.log("\nDeploying StatusListManager...");
const slm = await viem.deployContract("StatusListManager", [ADMIN], {
  client: { wallet: walletClient, public: publicClient },
  ...request,
});
console.log("StatusListManager:", slm.address);

// ISSUER_ROLE direto do contrato
const issuerRole = await slm.read.ISSUER_ROLE();
console.log("Grant ISSUER_ROLE ->", ISSUER);
const txGrantIssuer = await slm.write.grantRole([issuerRole, ISSUER], {
  account: account.address,
  ...request,
});
await publicClient.waitForTransactionReceipt({ hash: txGrantIssuer });

console.log("\n✅ Deploy concluido");
console.log({
  network: connection.networkName,
  deployer: account.address,
  DIDRegistry: did.address,
  StatusListManager: slm.address,
  ADMIN,
  REGISTRAR,
  ISSUER,
});
