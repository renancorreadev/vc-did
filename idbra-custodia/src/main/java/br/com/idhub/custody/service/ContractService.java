package br.com.idhub.custody.service;

import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

@Service
public class ContractService {

    /**
     * Gerar dados da função createList do StatusListManager
     */
    public String createListFunctionData(String listId, String uri, String hash, Long size, String purpose) {
        try {
            System.out.println("=== DEBUG CONTRACTSERVICE ===");
            System.out.println("listId: " + listId);
            System.out.println("uri: " + uri);
            System.out.println("hash original: " + hash);
            System.out.println("size: " + size);
            System.out.println("purpose: " + purpose);

            // Validar e corrigir formato do hash
            String cleanHash = hash;
            if (hash.startsWith("0x0x")) {
                cleanHash = hash.substring(2); // Remove o primeiro 0x
                System.out.println("hash corrigido: " + cleanHash);
            } else if (!hash.startsWith("0x")) {
                cleanHash = "0x" + hash; // Adiciona 0x se não tiver
                System.out.println("hash corrigido: " + cleanHash);
            }

            // Converter purpose para bytes32 (hash do string)
            byte[] purposeBytes = purpose.getBytes();
            byte[] purposeHashBytes = org.web3j.crypto.Hash.sha3(purposeBytes);

            // Criar função
            Function function = new Function(
                "createList",
                Arrays.asList(
                    new Utf8String(listId),
                    new Utf8String(uri),
                    new Bytes32(Numeric.hexStringToByteArray(cleanHash)),
                    new Uint64(size),
                    new Bytes32(purposeHashBytes)
                ),
                Arrays.asList()
            );
            System.out.println("Função criada com sucesso");

            // Codificar função
            String result = FunctionEncoder.encode(function);
            System.out.println("Função codificada: " + result.substring(0, Math.min(100, result.length())));
            System.out.println("=== FIM DEBUG ===");

            return result;

        } catch (Exception e) {
            System.err.println("=== ERRO DETALHADO ===");
            System.err.println("Erro: " + e.getClass().getSimpleName());
            System.err.println("Mensagem: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== FIM ERRO ===");
            throw new RuntimeException("Erro ao codificar função createList: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função publish do StatusListManager
     */
    public String publishFunctionData(String listId, Long newVersion, String newUri, String newHash) {
        try {
            System.out.println("[PUBLISH] Iniciando codificação da função publish...");
            System.out.println("[PUBLISH] listId: " + listId);
            System.out.println("[PUBLISH] newVersion: " + newVersion);
            System.out.println("[PUBLISH] newUri: " + newUri);
            System.out.println("[PUBLISH] newHash original: " + newHash);

            // Validar e corrigir formato do hash
            String cleanHash = newHash;
            if (newHash.startsWith("0x0x")) {
                cleanHash = newHash.substring(2);
                System.out.println("[PUBLISH] hash corrigido (duplo 0x): " + cleanHash);
            } else if (!newHash.startsWith("0x")) {
                cleanHash = "0x" + newHash;
                System.out.println("[PUBLISH] hash corrigido (sem 0x): " + cleanHash);
            }

            // Validar se o hash tem o tamanho correto (32 bytes = 64 caracteres hex + 0x)
            if (cleanHash.length() != 66) {
                throw new RuntimeException("Hash inválido - deve ter 32 bytes (66 caracteres com 0x): " + cleanHash);
            }

            System.out.println("[PUBLISH] hash final: " + cleanHash);


            Function function = new Function(
                "publish",
                Arrays.asList(
                    new Utf8String(listId),
                    new Uint256(BigInteger.valueOf(newVersion)), // CORRIGIDO: voltando para Uint256 como no contrato
                    new Utf8String(newUri),
                    new Bytes32(Numeric.hexStringToByteArray(cleanHash))
                ),
                Arrays.asList()
            );

            // Codificar função
            String encodedFunction = FunctionEncoder.encode(function);
            System.out.println("[PUBLISH] Função codificada com sucesso: " + encodedFunction.substring(0, Math.min(50, encodedFunction.length())) + "...");

            return encodedFunction;

        } catch (Exception e) {
            System.err.println("[PUBLISH] ERRO ao codificar função publish: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao codificar função publish: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função setAttribute do DIDRegistry
     */
    public String setAttributeFunctionData(String identity, String name, String value, Long validTo) {
        try {
            // Converter name para bytes32 (hash do string)
            String nameHash = "0x" + Numeric.toHexStringNoPrefix(
                org.web3j.crypto.Hash.sha3(name.getBytes())
            );

            // Criar função
            Function function = new Function(
                "setAttribute",
                Arrays.asList(
                    new Address(identity),
                    new Bytes32(Numeric.hexStringToByteArray(nameHash)),
                    new DynamicBytes(Numeric.hexStringToByteArray(value)),
                    new Uint256(validTo)
                ),
                Arrays.asList()
            );

            // Codificar função
            return FunctionEncoder.encode(function);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função setAttribute", e);
        }
    }

    /**
     * Gerar dados da função ISSUER_ROLE() do contrato
     */
    public String getIssuerRoleFunctionData() {
        try {
            // Função ISSUER_ROLE() não tem parâmetros e retorna bytes32
            Function function = new Function(
                "ISSUER_ROLE",
                Arrays.asList(), // sem parâmetros
                Arrays.asList() // sem retorno especificado para encoding
            );

            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função ISSUER_ROLE", e);
        }
    }

    /**
     * Gerar dados da função hasRole(bytes32 role, address account)
     */
    public String getHasRoleFunctionData(String roleValue, String accountAddress) {
        try {
            // Limpar o roleValue se necessário
            String cleanRoleValue = roleValue;
            if (!roleValue.startsWith("0x")) {
                cleanRoleValue = "0x" + roleValue;
            }

            // Função hasRole(bytes32 role, address account)
            Function function = new Function(
                "hasRole",
                Arrays.asList(
                    new Bytes32(Numeric.hexStringToByteArray(cleanRoleValue)),
                    new Address(accountAddress)
                ),
                Arrays.asList() // sem retorno especificado para encoding
            );

            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função hasRole", e);
        }
    }

    /**
     * Gerar dados da função grantRole(bytes32 role, address account)
     */
    public String getGrantRoleFunctionData(String roleValue, String accountAddress) {
        try {
            // Limpar o roleValue se necessário
            String cleanRoleValue = roleValue;
            if (!roleValue.startsWith("0x")) {
                cleanRoleValue = "0x" + roleValue;
            }

            // Função grantRole(bytes32 role, address account)
            Function function = new Function(
                "grantRole",
                Arrays.asList(
                    new Bytes32(Numeric.hexStringToByteArray(cleanRoleValue)),
                    new Address(accountAddress)
                ),
                Arrays.asList() // sem retorno especificado para encoding
            );

            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função grantRole", e);
        }
    }

    /**
     * Gerar dados da função revokeAttribute do DIDRegistry
     */
    public String revokeAttributeFunctionData(String identity, String name, String value) {
        try {
            // Converter name para bytes32 (hash do string)
            String nameHash = "0x" + Numeric.toHexStringNoPrefix(
                org.web3j.crypto.Hash.sha3(name.getBytes())
            );

            // Criar função
            Function function = new Function(
                "revokeAttribute",
                Arrays.asList(
                    new Address(identity),
                    new Bytes32(Numeric.hexStringToByteArray(nameHash)),
                    new DynamicBytes(Numeric.hexStringToByteArray(value))
                ),
                Arrays.asList()
            );

            // Codificar função
            return FunctionEncoder.encode(function);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função revokeAttribute", e);
        }
    }
}
