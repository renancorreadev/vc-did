import type { HardhatUserConfig } from "hardhat/config";
import hardhatToolboxViemPlugin from "@nomicfoundation/hardhat-toolbox-viem";
import { configVariable } from "hardhat/config";
import * as dotenv from "dotenv";

// Carregar variáveis de ambiente
dotenv.config();

const config: HardhatUserConfig = {
  plugins: [hardhatToolboxViemPlugin],
  solidity: {
    profiles: {
      default: {
        version: "0.8.20",
        settings: {
          evmVersion: "london",
          optimizer: { enabled: true, runs: 200 },
        },
      },
      production: {
        version: "0.8.20",
        settings: {
          evmVersion: "london",
          optimizer: { enabled: true, runs: 200 },
        },
      },
    },
  },
  networks: {
    hardhatMainnet: { type: "edr-simulated", chainType: "l1" },
    hardhatOp: { type: "edr-simulated", chainType: "op" },

    // === Besu (RPC HTTP) ===
    besu: {
      type: "http",
      chainType: "l1",
      url: "http://144.22.179.183",
      chainId: 1337,
      accounts: [process.env.BESU_PRIVATE_KEY || "0xa1b7f78123ab0bb66add0a8d59a30932b3e6bc6b823ea245e6461fdce38a9d7a"],
      gasPrice: 0,
      gas: 0x1ffffffffffffen,
    },
  },
};

export default config;
