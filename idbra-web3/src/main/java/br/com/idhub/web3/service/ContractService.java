package br.com.idhub.web3.service;

import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Bool;
import org.web3j.utils.Numeric;
import org.web3j.crypto.Hash;

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
    // REMOVER ESTE MÉTODO COMPLETAMENTE - ele usa função 'publish' que não existe na ABI
    // public String publishFunctionData(String listId, Long newVersion, String newUri, String newHash) {
    //     try {
    //         System.out.println("[PUBLISH] Iniciando codificação da função publish...");
    //         System.out.println("[PUBLISH] listId: " + listId);
    //         System.out.println("[PUBLISH] newVersion: " + newVersion);
    //         System.out.println("[PUBLISH] newUri: " + newUri);
    //         System.out.println("[PUBLISH] newHash original: " + newHash);
    //
    //         // Validar e corrigir formato do hash
    //         String cleanHash = newHash;
    //         if (newHash.startsWith("0x0x")) {
    //             cleanHash = newHash.substring(2);
    //             System.out.println("[PUBLISH] hash corrigido (duplo 0x): " + cleanHash);
    //         } else if (!newHash.startsWith("0x")) {
    //             cleanHash = "0x" + newHash;
    //             System.out.println("[PUBLISH] hash corrigido (sem 0x): " + cleanHash);
    //         }
    //
    //         // Validar se o hash tem o tamanho correto (32 bytes = 64 caracteres hex + 0x)
    //         if (cleanHash.length() != 66) {
    //             throw new RuntimeException("Hash inválido - deve ter 32 bytes (66 caracteres com 0x): " + cleanHash);
    //         }
    //
    //         System.out.println("[PUBLISH] hash final: " + cleanHash);
    //
    //
    //     Function function = new Function(
    //         "publish",
    //         Arrays.asList(
    //             new Utf8String(listId),
    //             new Uint256(BigInteger.valueOf(newVersion)), // CORRIGIDO: voltando para Uint256 como no contrato
    //             new Utf8String(newUri),
    //             new Bytes32(Numeric.hexStringToByteArray(cleanHash))
    //         ),
    //         Arrays.asList()
    //     );
    //
    //     // Codificar função
    //     String encodedFunction = FunctionEncoder.encode(function);
    //     System.out.println("[PUBLISH] Função codificada com sucesso: " + encodedFunction.substring(0, Math.min(50, encodedFunction.length())) + "...");
    //
    //     return encodedFunction;
    //
    //     } catch (Exception e) {
    //         System.err.println("[PUBLISH] ERRO ao codificar função publish: " + e.getMessage());
    //         e.printStackTrace();
    //         throw new RuntimeException("Erro ao codificar função publish: " + e.getMessage(), e);
    //     }
    // }

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
     * Valor hash constante do ISSUER_ROLE = keccak256("ISSUER_ROLE")
     */
    private static final String ISSUER_ROLE_HASH = "0x114e74f6ea3bd819998f78687bfcb11b140da08e9b7d222fa9c1f1ba1f2aa122";

    /**
     * Gerar dados da função hasRole(bytes32 role, address account) usando valor constante
     */
    public String getHasRoleFunctionData(String accountAddress) {
        try {
            // Validar formato do endereço
            if (accountAddress == null || !accountAddress.matches("^0x[a-fA-F0-9]{40}$")) {
                throw new IllegalArgumentException("Account address deve ser um endereço Ethereum válido. Recebido: " + accountAddress);
            }

            // Função hasRole(bytes32 role, address account) usando ISSUER_ROLE_HASH constante
            Function function = new Function(
                "hasRole",
                Arrays.asList(
                    new Bytes32(Numeric.hexStringToByteArray(ISSUER_ROLE_HASH)),
                    new Address(accountAddress)
                ),
                Arrays.asList() // sem retorno especificado para encoding
            );

            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função hasRole: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função grantRole(bytes32 role, address account) usando valor constante
     */
    public String getGrantRoleFunctionData(String accountAddress) {
        try {
            // Validar formato do endereço
            if (accountAddress == null || !accountAddress.matches("^0x[a-fA-F0-9]{40}$")) {
                throw new IllegalArgumentException("Account address deve ser um endereço Ethereum válido. Recebido: " + accountAddress);
            }

            // Função grantRole(bytes32 role, address account) usando ISSUER_ROLE_HASH constante
            Function function = new Function(
                "grantRole",
                Arrays.asList(
                    new Bytes32(Numeric.hexStringToByteArray(ISSUER_ROLE_HASH)),
                    new Address(accountAddress)
                ),
                Arrays.asList() // sem retorno especificado para encoding
            );

            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função grantRole: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função revokeAttribute do DIDRegistry
     */
    public String revokeAttributeFunctionData(String identity, String name, String value) {
        try {
            System.out.println("[DEBUG] revokeAttributeFunctionData - identity: " + identity);
            System.out.println("[DEBUG] revokeAttributeFunctionData - name: " + name);
            System.out.println("[DEBUG] revokeAttributeFunctionData - value: " + value);

            // Validar parâmetros de entrada
            if (identity == null || name == null || value == null) {
                throw new IllegalArgumentException("Parâmetros não podem ser nulos");
            }

            // Converter identity para address - se não for endereço válido, usar hash
            String identityAddress;
            if (identity.startsWith("0x") && identity.length() == 42) {
                // Já é um endereço Ethereum válido
                identityAddress = identity;
            } else {
                // Converter para hash e depois para endereço
                byte[] identityHash = org.web3j.crypto.Hash.sha3(identity.getBytes());
                // Pegar os últimos 20 bytes (160 bits) para formar um endereço
                byte[] addressBytes = new byte[20];
                System.arraycopy(identityHash, 12, addressBytes, 0, 20);
                identityAddress = "0x" + org.web3j.utils.Numeric.toHexStringNoPrefix(addressBytes);
            }
            System.out.println("[DEBUG] identityAddress: " + identityAddress);

            // Converter name para bytes32 (hash do string)
            String nameHash = "0x" + org.web3j.utils.Numeric.toHexStringNoPrefix(
                org.web3j.crypto.Hash.sha3(name.getBytes())
            );
            System.out.println("[DEBUG] nameHash: " + nameHash);

            // Converter value para bytes - tratamento corrigido para prefixo duplo
            byte[] valueBytes;
            if (value.startsWith("0x")) {
                try {
                    // Corrigir prefixo duplo 0x0x
                    String cleanValue = value;
                    if (cleanValue.startsWith("0x0x")) {
                        cleanValue = cleanValue.substring(2); // Remove o primeiro 0x
                        System.out.println("[DEBUG] Removido prefixo duplo 0x: " + cleanValue);
                    }

                    // Remover o prefixo 0x e converter
                    String hexValue = cleanValue.substring(2);

                    // Validar que é um hash de 32 bytes (64 caracteres hex)
                    if (hexValue.length() != 64) {
                        throw new IllegalArgumentException("Hash deve ter exatamente 32 bytes (64 caracteres hex), mas tem: " + hexValue.length());
                    }

                    valueBytes = org.web3j.utils.Numeric.hexStringToByteArray("0x" + hexValue);
                    System.out.println("[DEBUG] Convertido de hex - length: " + valueBytes.length);
                } catch (Exception hexError) {
                    System.err.println("[DEBUG] Erro na conversão hex, usando fallback: " + hexError.getMessage());
                    // Fallback: tratar como string normal
                    valueBytes = value.getBytes("UTF-8");
                }
            } else {
                // Se não é hex, converter string para bytes
                valueBytes = value.getBytes("UTF-8");
                System.out.println("[DEBUG] Convertido de string - length: " + valueBytes.length);
            }

            // Criar função seguindo a ABI: revokeAttribute(address,bytes32,bytes)
            Function function = new Function(
                "revokeAttribute",
                Arrays.asList(
                    new Address(identityAddress),        // address identity (convertido)
                    new Bytes32(org.web3j.utils.Numeric.hexStringToByteArray(nameHash)), // bytes32 name
                    new DynamicBytes(valueBytes)         // bytes value
                ),
                Arrays.asList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            System.out.println("[DEBUG] Função codificada com sucesso - length: " + encodedFunction.length());
            return encodedFunction;

        } catch (Exception e) {
            System.err.println("[ERROR] Erro detalhado ao codificar função revokeAttribute:");
            System.err.println("[ERROR] - identity: " + identity);
            System.err.println("[ERROR] - name: " + name);
            System.err.println("[ERROR] - value: " + value);
            System.err.println("[ERROR] - Exception: " + e.getClass().getSimpleName());
            System.err.println("[ERROR] - Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao codificar função revokeAttribute: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função createDID do IDBraDIDRegistry
     */
    public String createDIDFunctionData(String identity, String didDocument) {
        try {
            Function function = new Function(
                "createDID",
                Arrays.asList(
                    new Address(identity),
                    new Utf8String(didDocument)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função createDID: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função updateDIDDocument do IDBraDIDRegistry
     */
    public String updateDIDDocumentFunctionData(String identity, String newDidDocument) {
        try {
            Function function = new Function(
                "updateDIDDocument",
                Arrays.asList(
                    new Address(identity),
                    new Utf8String(newDidDocument)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função updateDIDDocument: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função setKYCStatus do IDBraDIDRegistry
     */
    public String setKYCStatusFunctionData(String identity, boolean verified) {
        try {
            Function function = new Function(
                "setKYCStatus",
                Arrays.asList(
                    new Address(identity),
                    new org.web3j.abi.datatypes.Bool(verified)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função setKYCStatus: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função exists do IDBraDIDRegistry
     */
    public String didExistsFunctionData(String identity) {
        try {
            Function function = new Function(
                "exists",
                Arrays.asList(
                    new Address(identity)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função exists: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função getIdentityInfo do IDBraDIDRegistry
     */
    public String getIdentityInfoFunctionData(String identity) {
        try {
            Function function = new Function(
                "getIdentityInfo",
                Arrays.asList(
                    new Address(identity)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função getIdentityInfo: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função issueCredential do IDBraDIDRegistry
     * Assinatura correta: issueCredential(bytes32 credentialId, address subject, bytes32 credentialHash)
     */
    public String issueCredentialFunctionData(String credentialId, String subject, String credentialHash) {
        try {
            System.out.println("[DEBUG] issueCredential - credentialId: " + credentialId);
            System.out.println("[DEBUG] issueCredential - subject: " + subject);
            System.out.println("[DEBUG] issueCredential - credentialHash: " + credentialHash);

            // Converter credentialId para bytes32
            byte[] credentialIdBytes = credentialId.getBytes();
            byte[] credentialIdHash = Hash.sha3(credentialIdBytes);
            System.out.println("[DEBUG] credentialIdHash length: " + credentialIdHash.length);

            // Validar e converter credentialHash para bytes32
            String cleanHash = credentialHash;
            if (!credentialHash.startsWith("0x")) {
                cleanHash = "0x" + credentialHash;
            }

            // Remover prefixo 0x para validação
            String hexPart = cleanHash.substring(2);

            // Validar que o hash tem exatamente 64 caracteres hex (32 bytes)
            if (hexPart.length() != 64) {
                throw new IllegalArgumentException("Hash deve ter exatamente 32 bytes (64 caracteres hex), mas tem: " + hexPart.length() + " caracteres");
            }

            // Converter para byte array
            byte[] hashBytes = Numeric.hexStringToByteArray(cleanHash);
            System.out.println("[DEBUG] hashBytes length: " + hashBytes.length);

            if (hashBytes.length != 32) {
                throw new IllegalArgumentException("Hash convertido deve ter exatamente 32 bytes, mas tem: " + hashBytes.length);
            }

            Function function = new Function(
                "issueCredential",
                Arrays.asList(
                    new Bytes32(credentialIdHash),
                    new Address(subject),
                    new Bytes32(hashBytes)
                ),
                Arrays.asList()
            );

            String encoded = FunctionEncoder.encode(function);
            System.out.println("[DEBUG] Função codificada com sucesso");
            return encoded;

        } catch (Exception e) {
            System.err.println("[ERROR] Erro detalhado ao codificar issueCredential:");
            System.err.println("[ERROR] - credentialId: " + credentialId);
            System.err.println("[ERROR] - subject: " + subject);
            System.err.println("[ERROR] - credentialHash: " + credentialHash);
            System.err.println("[ERROR] - Erro: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao codificar função issueCredential: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função revokeCredential do IDBraDIDRegistry
     * Assinatura correta: revokeCredential(bytes32 credentialId, address subject, string reason)
     */
    public String revokeCredentialFunctionData(String credentialId, String subject, String reason) {
        try {
            // Converter credentialId para bytes32
            byte[] credentialIdBytes = credentialId.getBytes();
            byte[] credentialIdHash = Hash.sha3(credentialIdBytes);

            Function function = new Function(
                "revokeCredential",
                Arrays.asList(
                    new Bytes32(credentialIdHash),
                    new Address(subject),
                    new Utf8String(reason)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função revokeCredential: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função restoreCredential do IDBraDIDRegistry
     * Assinatura correta: restoreCredential(bytes32 credentialId, address subject, string reason)
     */
    public String restoreCredentialFunctionData(String credentialId, String subject, String reason) {
        try {
            // Converter credentialId para bytes32
            byte[] credentialIdBytes = credentialId.getBytes();
            byte[] credentialIdHash = Hash.sha3(credentialIdBytes);

            Function function = new Function(
                "restoreCredential",
                Arrays.asList(
                    new Bytes32(credentialIdHash),
                    new Address(subject),
                    new Utf8String(reason)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função restoreCredential: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função addDelegate do IDBraDIDRegistry
     * Corrigindo para usar hash do delegateType
     */
    public String addDelegateFunctionData(String identity, String delegateType, String delegate, Long validity) {
        try {
            // Converter delegateType para bytes32 (hash do tipo)
            byte[] delegateTypeBytes = delegateType.getBytes();
            byte[] delegateTypeHash = Hash.sha3(delegateTypeBytes);

            Function function = new Function(
                "addDelegate",
                Arrays.asList(
                    new Address(identity),
                    new Bytes32(delegateTypeHash),
                    new Address(delegate),
                    new Uint256(validity)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função addDelegate: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função revokeDelegate do IDBraDIDRegistry
     * Corrigindo para usar hash do delegateType
     */
    public String revokeDelegateFunctionData(String identity, String delegateType, String delegate) {
        try {
            // Converter delegateType para bytes32 (hash do tipo)
            byte[] delegateTypeBytes = delegateType.getBytes();
            byte[] delegateTypeHash = Hash.sha3(delegateTypeBytes);

            Function function = new Function(
                "revokeDelegate",
                Arrays.asList(
                    new Address(identity),
                    new Bytes32(delegateTypeHash),
                    new Address(delegate)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função revokeDelegate: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função validDelegate do IDBraDIDRegistry
     * Corrigindo para usar hash do delegateType
     */
    public String isValidDelegateFunctionData(String identity, String delegateType, String delegate) {
        try {
            // Converter delegateType para bytes32 (hash do tipo)
            byte[] delegateTypeBytes = delegateType.getBytes();
            byte[] delegateTypeHash = Hash.sha3(delegateTypeBytes);

            Function function = new Function(
                "validDelegate",
                Arrays.asList(
                    new Address(identity),
                    new Bytes32(delegateTypeHash),
                    new Address(delegate)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função validDelegate: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função isCredentialRevoked do IDBraDIDRegistry
     * Corrigindo para usar bytes32 credentialId
     */
    public String isCredentialRevokedFunctionData(String credentialId) {
        try {
            // Converter credentialId para bytes32
            byte[] credentialIdBytes = credentialId.getBytes();
            byte[] credentialIdHash = Hash.sha3(credentialIdBytes);

            Function function = new Function(
                "isCredentialRevoked",
                Arrays.asList(
                    new Bytes32(credentialIdHash)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função isCredentialRevoked: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função getCredentialRevocation do IDBraDIDRegistry
     * Corrigindo para usar bytes32 credentialId
     */
    public String getCredentialRevocationFunctionData(String credentialId) {
        try {
            // Converter credentialId para bytes32
            byte[] credentialIdBytes = credentialId.getBytes();
            byte[] credentialIdHash = Hash.sha3(credentialIdBytes);

            Function function = new Function(
                "getCredentialRevocation",
                Arrays.asList(
                    new Bytes32(credentialIdHash)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função getCredentialRevocation: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função getKYCStatus (nova função)
     */
    public String getKYCStatusFunctionData(String identity) {
        try {
            Function function = new Function(
                "isKYCVerified",
                Arrays.asList(
                    new Address(identity)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função isKYCVerified: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função pause
     */
    public String pauseFunctionData() {
        try {
            Function function = new Function(
                "pause",
                Arrays.asList(),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função pause: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função unpause
     */
    public String unpauseFunctionData() {
        try {
            Function function = new Function(
                "unpause",
                Arrays.asList(),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função unpause: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função getIdentityCredentials do IDBraDIDRegistry
     */
    public String getIdentityCredentialsFunctionData(String identity) {
        try {
            Function function = new Function(
                "getIdentityCredentials",
                Arrays.asList(
                    new Address(identity)
                ),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função getIdentityCredentials: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função getSystemMetrics do IDBraDIDRegistry
     */
    public String getSystemMetricsFunctionData() {
        try {
            Function function = new Function(
                "getSystemMetrics",
                Arrays.asList(),
                Arrays.asList()
            );
            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar função getSystemMetrics: " + e.getMessage(), e);
        }
    }

    /**
     * Gerar dados da função setAttribute para armazenar StatusList
     */
    public String publishStatusListFunctionData(String issuerAddress, String listId, String uri, String hash) {
        try {
            // Usar setAttribute para armazenar dados da StatusList
            String attributeName = "statuslist:" + listId;
            byte[] nameBytes = attributeName.getBytes();

            // Criar JSON com os dados da StatusList
            String statusListData = String.format("{\"uri\":\"%s\",\"hash\":\"%s\"}", uri, hash);
            byte[] valueBytes = statusListData.getBytes();

            // Pad nameBytes para 32 bytes
            byte[] paddedNameBytes = new byte[32];
            System.arraycopy(nameBytes, 0, paddedNameBytes, 0, Math.min(nameBytes.length, 32));

            Function function = new Function(
                "setAttribute",
                Arrays.asList(
                    new Address(issuerAddress),
                    new Bytes32(paddedNameBytes),
                    new DynamicBytes(valueBytes),
                    new Uint256(BigInteger.valueOf(System.currentTimeMillis() / 1000 + 31536000)) // Válido por 1 ano
                ),
                Arrays.asList()
            );

            return FunctionEncoder.encode(function);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar dados da função setAttribute: " + e.getMessage(), e);
        }
    }

}


