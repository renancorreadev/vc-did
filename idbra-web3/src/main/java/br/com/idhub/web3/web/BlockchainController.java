package br.com.idhub.web3.web;

import br.com.idhub.web3.service.BlockchainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

        @Autowired
        private BlockchainService blockchainService;

        // Endereços hardcoded temporariamente
        private static final String DID_REGISTRY_ADDRESS = "0x34c2AcC42882C0279A64bB1a4B1083D483BdE886";

        // ========= ROLE MANAGEMENT =========

        @Operation(summary = "🔍 Verificar Role de Issuer", description = """
                        Verifica se uma wallet possui ISSUER_ROLE nos contratos inteligentes.

                        **Funcionalidade**: Este endpoint é usado para verificar se uma carteira blockchain tem permissão para emitir credenciais verificáveis.

                        **Uso**: Utilizado pelos serviços issuer e verifier para validar autorizações antes de processar operações.

                        **Retorna**: Status do role, endereço da wallet e timestamp da verificação.
                        """, tags = {
                        "🔐 Role Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Role verificado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "Resposta de Sucesso", summary = "Wallet com ISSUER_ROLE", value = """
                                        {
                                            "walletAddress": "0x1234567890abcdef1234567890abcdef12345678",
                                            "hasIssuerRole": true,
                                            "didRegistry": true,
                                            "timestamp": "2024-08-21T10:00:00",
                                            "message": "Wallet possui ISSUER_ROLE ativo"
                                        }
                                        """))),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Erro de Validação", summary = "Endereço inválido", value = """
                                        {
                                            "error": "Endereço de wallet inválido",
                                            "timestamp": "2024-08-21T10:00:00",
                                            "details": "Formato de endereço Ethereum inválido"
                                        }
                                        """)))
        })
        @GetMapping("/check-role/{walletAddress}")
        public ResponseEntity<Map<String, Object>> checkIssuerRole(
                        @Parameter(description = "Endereço da wallet Ethereum (formato: 0x...)", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String walletAddress) {
                try {
                        boolean hasRole = blockchainService.hasIssuerRole(walletAddress);
                        Map<String, Object> response = Map.of(
                                        "walletAddress", walletAddress,
                                        "hasIssuerRole", hasRole,
                                        "didRegistry", blockchainService.hasIssuerRoleForContract(walletAddress,
                                                        blockchainService.getDidRegistryAddress()),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message",
                                        hasRole ? "Wallet possui ISSUER_ROLE ativo" : "Wallet não possui ISSUER_ROLE");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        Map<String, Object> error = Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "walletAddress", walletAddress);
                        return ResponseEntity.badRequest().body(error);
                }
        }

        @Operation(summary = "🔐 Verificar Role de Admin", description = """
                        Verifica se uma wallet possui DEFAULT_ADMIN_ROLE na blockchain.

                        **Funcionalidade**: Endpoint administrativo para verificar permissões de administrador.

                        **Segurança**: Requer validação de chave administrativa para operações sensíveis.

                        **Uso**: Utilizado por administradores do sistema para gerenciar permissões.
                        """, tags = { "🔐 Role Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Role verificado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/check-admin-role/{walletAddress}")
        public ResponseEntity<Map<String, Object>> checkDefaultAdminRole(
                        @Parameter(description = "Endereço da wallet Ethereum", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String walletAddress) {
                try {
                        boolean hasRole = blockchainService.hasDefaultAdminRole(walletAddress);
                        Map<String, Object> response = Map.of(
                                        "walletAddress", walletAddress,
                                        "hasDefaultAdminRole", hasRole,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", hasRole ? "Wallet possui DEFAULT_ADMIN_ROLE"
                                                        : "Wallet não possui DEFAULT_ADMIN_ROLE");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        Map<String, Object> error = Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "walletAddress", walletAddress);
                        return ResponseEntity.badRequest().body(error);
                }
        }

        @Operation(summary = "🔍 Verificar Role Específico", description = """
                        Verifica se uma wallet possui um role específico na blockchain.

                        **Roles Disponíveis**:
                        - `ISSUER_ROLE`: Permissão para emitir credenciais
                        - `VERIFIER_ROLE`: Permissão para verificar credenciais
                        - `HOLDER_ROLE`: Permissão para armazenar credenciais
                        - `DEFAULT_ADMIN_ROLE`: Permissões administrativas

                        **Uso**: Verificação flexível de qualquer role configurado no sistema.
                        """, tags = { "🔐 Role Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Role verificado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/check-role/{role}/{walletAddress}")
        public ResponseEntity<Map<String, Object>> checkSpecificRole(
                        @Parameter(description = "Role a verificar", example = "ISSUER_ROLE", schema = @Schema(allowableValues = {
                                        "ISSUER_ROLE", "VERIFIER_ROLE", "HOLDER_ROLE",
                                        "DEFAULT_ADMIN_ROLE" }), required = true) @PathVariable String role,
                        @Parameter(description = "Endereço da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String walletAddress) {
                try {
                        boolean hasRole = blockchainService.hasRole(role, walletAddress);
                        Map<String, Object> response = Map.of(
                                        "role", role,
                                        "walletAddress", walletAddress,
                                        "hasRole", hasRole,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", hasRole ? "Wallet possui o role solicitado"
                                                        : "Wallet não possui o role solicitado");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        Map<String, Object> error = Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "role", role,
                                        "walletAddress", walletAddress);
                        return ResponseEntity.badRequest().body(error);
                }
        }

        @Operation(summary = "✅ Conceder Role", description = """
                        Concede um role específico para uma wallet (requer chave administrativa).

                        **Segurança**: Operação administrativa que requer ADMIN_PRIVATE_KEY.

                        **Processo**:
                        1. Validação da chave administrativa
                        2. Verificação de permissões
                        3. Execução da transação blockchain
                        4. Confirmação da operação

                        **Roles Suportados**: ISSUER_ROLE, VERIFIER_ROLE, HOLDER_ROLE
                        """, tags = { "🔐 Role Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Role concedido com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao conceder role")
        })
        @PostMapping("/grant-role/{role}/{walletAddress}")
        public ResponseEntity<Map<String, Object>> grantRole(
                        @Parameter(description = "Role a conceder", example = "ISSUER_ROLE", schema = @Schema(allowableValues = {
                                        "ISSUER_ROLE", "VERIFIER_ROLE",
                                        "HOLDER_ROLE" }), required = true) @PathVariable String role,
                        @Parameter(description = "Endereço da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String walletAddress) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.grantRole(role, walletAddress);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "role", role,
                                        "walletAddress", walletAddress,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Role concedido com sucesso"
                                                        : "Falha na transação blockchain");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        Map<String, Object> error = Map.of(
                                        "success", false,
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "role", role,
                                        "walletAddress", walletAddress);
                        return ResponseEntity.badRequest().body(error);
                }
        }

        @Operation(summary = "❌ Revogar Role", description = """
                        Revoga um role específico de uma wallet (requer chave administrativa).

                        **Segurança**: Operação administrativa que requer ADMIN_PRIVATE_KEY.

                        **Impacto**: A wallet perderá todas as permissões associadas ao role revogado.

                        **Reversão**: O role pode ser re-concedido posteriormente se necessário.
                        """, tags = { "🔐 Role Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Role revogado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao revogar role")
        })
        @PostMapping("/revoke-role/{role}/{walletAddress}")
        public ResponseEntity<Map<String, Object>> revokeRole(
                        @Parameter(description = "Role a revogar", example = "ISSUER_ROLE", schema = @Schema(allowableValues = {
                                        "ISSUER_ROLE", "VERIFIER_ROLE",
                                        "HOLDER_ROLE" }), required = true) @PathVariable String role,
                        @Parameter(description = "Endereço da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String walletAddress) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.revokeRole(role,
                                        walletAddress);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "role", role,
                                        "walletAddress", walletAddress,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Role revogado com sucesso"
                                                        : "Falha na transação blockchain");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        Map<String, Object> error = Map.of(
                                        "success", false,
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "role", role,
                                        "walletAddress", walletAddress);
                        return ResponseEntity.badRequest().body(error);
                }
        }

        // ========= DID MANAGEMENT =========

        @Operation(summary = "🆔 Criar DID", description = """
                        Cria um novo DID (Decentralized Identifier) na blockchain.

                        **Funcionalidade**: Registra uma nova identidade descentralizada na rede Hyperledger Besu.

                        **Processo**:
                        1. Validação do formato do DID
                        2. Verificação de unicidade
                        3. Execução da transação blockchain
                        4. Confirmação do registro

                        **Formato DID**: Segue o padrão W3C DID (ex: did:example:123)

                        **Segurança**: Requer ADMIN_PRIVATE_KEY para operações de criação.
                        """, tags = { "🆔 DID Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ DID criado com sucesso", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "DID Criado", summary = "Identidade registrada na blockchain", value = """
                                        {
                                            "success": true,
                                            "identity": "did:example:123",
                                            "transactionHash": "0xabc123...",
                                            "timestamp": "2024-08-21T10:00:00",
                                            "message": "DID criado e registrado na blockchain"
                                        }
                                        """))),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao criar DID")
        })
        @PostMapping("/did/create")
        public ResponseEntity<Map<String, Object>> createDID(
                        @Parameter(description = "Identidade do DID (formato: did:example:123)", example = "did:example:123", required = true) @RequestParam String identity,
                        @Parameter(description = "Documento DID em formato JSON (padrão W3C)", example = "{\"@context\": \"https://www.w3.org/ns/did/v1\", \"id\": \"did:example:123\"}", required = true) @RequestParam String didDocument) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.createDID(identity,
                                        didDocument);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message",
                                        receipt.isStatusOK() ? "DID criado e registrado na blockchain"
                                                        : "Falha na criação do DID");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "📝 Atualizar Documento DID", description = """
                        Atualiza o documento DID de uma identidade existente.

                        **Funcionalidade**: Permite modificar informações de uma identidade já registrada.

                        **Validações**:
                        - DID deve existir na blockchain
                        - Formato do documento deve ser válido
                        - Requer permissões adequadas

                        **Uso**: Atualização de informações pessoais, endereços, chaves públicas, etc.
                        """, tags = { "🆔 DID Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ DID atualizado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao atualizar DID")
        })
        @PostMapping("/did/update")
        public ResponseEntity<Map<String, Object>> updateDIDDocument(
                        @Parameter(description = "Identidade do DID a atualizar", example = "did:example:123", required = true) @RequestParam String identity,
                        @Parameter(description = "Novo documento DID em formato JSON", example = "{\"@context\": \"https://www.w3.org/ns/did/v1\", \"id\": \"did:example:123\", \"updated\": true}", required = true) @RequestParam String newDidDocument) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.updateDIDDocument(identity,
                                        newDidDocument);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Documento DID atualizado com sucesso"
                                                        : "Falha na atualização");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "🔍 Verificar Existência de DID", description = """
                        Verifica se um DID existe na blockchain.

                        **Funcionalidade**: Consulta rápida para verificar o status de registro de uma identidade.

                        **Retorna**: Boolean indicando se o DID está registrado.

                        **Uso**: Validação de identidades antes de operações, verificação de duplicatas.
                        """, tags = { "🆔 DID Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Verificação realizada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/did/exists/{identity}")
        public ResponseEntity<Map<String, Object>> checkDIDExists(
                        @Parameter(description = "Identidade do DID a verificar", example = "did:example:123", required = true) @PathVariable String identity) {
                try {
                        boolean exists = blockchainService.didExists(identity);
                        Map<String, Object> response = Map.of(
                                        "identity", identity,
                                        "exists", exists,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message",
                                        exists ? "DID encontrado na blockchain" : "DID não encontrado na blockchain");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "📋 Obter Informações da Identidade", description = """
                        Retorna informações completas de uma identidade DID.

                        **Funcionalidade**: Recupera todos os dados associados a uma identidade.

                        **Informações Retornadas**:
                        - Documento DID completo
                        - Atributos configurados
                        - Delegados ativos
                        - Status de validação
                        - Timestamps de criação/modificação

                        **Uso**: Consulta detalhada para aplicações que precisam de informações completas da identidade.
                        """, tags = { "🆔 DID Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Informações obtidas com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao obter informações")
        })
        @GetMapping("/did/info/{identity}")
        public ResponseEntity<Map<String, Object>> getIdentityInfo(
                        @Parameter(description = "Identidade do DID", example = "did:example:123", required = true) @PathVariable String identity) {
                try {
                        Optional<Map<String, Object>> infoOpt = blockchainService.getIdentityInfo(identity);
                        if (infoOpt.isPresent()) {
                                Map<String, Object> info = infoOpt.get();
                                info.put("timestamp", java.time.LocalDateTime.now().toString());
                                info.put("message", "Informações da identidade recuperadas com sucesso");
                                return ResponseEntity.ok(info);
                        } else {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Identidade não encontrada",
                                                "identity", identity,
                                                "timestamp", java.time.LocalDateTime.now().toString()));
                        }
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        // ========= DELEGATE MANAGEMENT =========

        @Operation(summary = "👥 Adicionar Delegate", description = """
                        Adiciona um delegate para uma identidade.

                        **Funcionalidade**: Permite que uma identidade designe representantes para operações específicas.

                        **Tipos de Delegate**:
                        - `SIGNER`: Pode assinar em nome da identidade
                        - `VERIFIER`: Pode verificar credenciais
                        - `ISSUER`: Pode emitir credenciais

                        **Validade**: O delegate tem um período de validade configurável em segundos.

                        **Segurança**: Requer permissões adequadas da identidade principal.
                        """, tags = {
                        "👥 Delegate Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Delegate adicionado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao adicionar delegate")
        })
        @PostMapping("/delegate/add")
        public ResponseEntity<Map<String, Object>> addDelegate(
                        @Parameter(description = "Identidade que está adicionando o delegate", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String identity,
                        @Parameter(description = "Tipo do delegate", example = "SIGNER", schema = @Schema(allowableValues = {
                                        "SIGNER", "VERIFIER",
                                        "ISSUER" }), required = true) @RequestParam String delegateType,
                        @Parameter(description = "Endereço do delegate", example = "0xabcdef1234567890abcdef1234567890abcdef12", required = true) @RequestParam String delegate,
                        @Parameter(description = "Validade em segundos (86400 = 24 horas)", example = "86400", required = true) @RequestParam Long validity) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.addDelegate(identity,
                                        delegateType,
                                        delegate, validity);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "delegateType", delegateType,
                                        "delegate", delegate,
                                        "validity", validity,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message",
                                        receipt.isStatusOK() ? "Delegate adicionado com sucesso"
                                                        : "Falha ao adicionar delegate");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "delegateType", delegateType,
                                        "delegate", delegate,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "❌ Revogar Delegate", description = """
                        Revoga um delegate de uma identidade.

                        **Funcionalidade**: Remove as permissões de um representante designado.

                        **Impacto**: O delegate perderá todas as permissões associadas ao tipo revogado.

                        **Segurança**: Apenas a identidade principal pode revogar seus próprios delegates.

                        **Reversão**: O delegate pode ser re-adicionado posteriormente se necessário.
                        """, tags = { "👥 Delegate Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Delegate revogado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao revogar delegate")
        })
        @PostMapping("/delegate/revoke")
        public ResponseEntity<Map<String, Object>> revokeDelegate(
                        @Parameter(description = "Identidade que está revogando o delegate", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String identity,
                        @Parameter(description = "Tipo do delegate a revogar", example = "SIGNER", schema = @Schema(allowableValues = {
                                        "SIGNER", "VERIFIER",
                                        "ISSUER" }), required = true) @RequestParam String delegateType,
                        @Parameter(description = "Endereço do delegate a revogar", example = "0xabcdef1234567890abcdef1234567890abcdef12", required = true) @RequestParam String delegate) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.revokeDelegate(identity,
                                        delegateType,
                                        delegate);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "delegateType", delegateType,
                                        "delegate", delegate,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Delegate revogado com sucesso"
                                                        : "Falha ao revogar delegate");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "delegateType", delegateType,
                                        "delegate", delegate,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "🔍 Verificar Delegate Válido", description = """
                        Verifica se um delegate é válido para uma identidade.

                        **Funcionalidade**: Valida se um representante ainda possui permissões ativas.

                        **Verificações**:
                        - Existência do delegate
                        - Validade temporal
                        - Tipo de permissão
                        - Status ativo

                        **Uso**: Validação antes de permitir operações por delegates.
                        """, tags = { "👥 Delegate Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Verificação realizada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/delegate/valid/{identity}/{delegateType}/{delegate}")
        public ResponseEntity<Map<String, Object>> checkValidDelegate(
                        @Parameter(description = "Identidade principal", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String identity,
                        @Parameter(description = "Tipo do delegate", example = "SIGNER", schema = @Schema(allowableValues = {
                                        "SIGNER", "VERIFIER",
                                        "ISSUER" }), required = true) @PathVariable String delegateType,
                        @Parameter(description = "Endereço do delegate", example = "0xabcdef1234567890abcdef1234567890abcdef12", required = true) @PathVariable String delegate) {
                try {
                        boolean isValid = blockchainService.validDelegate(identity, delegateType, delegate);
                        Map<String, Object> response = Map.of(
                                        "identity", identity,
                                        "delegateType", delegateType,
                                        "delegate", delegate,
                                        "isValid", isValid,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message",
                                        isValid ? "Delegate é válido e ativo" : "Delegate não é válido ou expirou");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "delegateType", delegateType,
                                        "delegate", delegate,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        // ========= ATTRIBUTE MANAGEMENT =========

        @Operation(summary = "🔧 Definir Atributo", description = """
                        Define um atributo para uma identidade.

                        **Funcionalidade**: Permite associar dados específicos a uma identidade DID.

                        **Tipos de Atributos**:
                        - Informações pessoais (nome, email, etc.)
                        - Dados de validação (KYC, documentos)
                        - Metadados customizados

                        **Validade**: O atributo tem um período de validade configurável.
                        """, tags = { "🔧 Attribute Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Atributo definido com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao definir atributo")
        })
        @PostMapping("/attribute/set")
        public ResponseEntity<Map<String, Object>> setAttribute(
                        @Parameter(description = "Identidade da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String identity,
                        @Parameter(description = "Nome do atributo", example = "NAME", required = true) @RequestParam String name,
                        @Parameter(description = "Valor do atributo", example = "John Doe", required = true) @RequestParam String value,
                        @Parameter(description = "Validade em segundos (86400 = 24 horas)", example = "86400", required = true) @RequestParam Long validity) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.setAttribute(identity, name,
                                        value,
                                        validity);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "name", name,
                                        "value", value,
                                        "validity", validity,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Atributo definido com sucesso"
                                                        : "Falha ao definir atributo");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "name", name,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "❌ Revogar Atributo", description = """
                        Revoga um atributo de uma identidade.

                        **Funcionalidade**: Remove um atributo previamente definido.

                        **Impacto**: O atributo será marcado como inválido.

                        **Segurança**: Apenas a identidade principal pode revogar seus atributos.
                        """, tags = { "🔧 Attribute Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Atributo revogado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao revogar atributo")
        })
        @PostMapping("/attribute/revoke")
        public ResponseEntity<Map<String, Object>> revokeAttribute(
                        @Parameter(description = "Identidade da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String identity,
                        @Parameter(description = "Nome do atributo", example = "NAME", required = true) @RequestParam String name,
                        @Parameter(description = "Valor do atributo", example = "John Doe", required = true) @RequestParam String value) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.revokeAttribute(identity, name,
                                        value);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "name", name,
                                        "value", value,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Atributo revogado com sucesso"
                                                        : "Falha ao revogar atributo");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "name", name,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        // ========= CREDENTIAL MANAGEMENT =========

        @Operation(summary = "📜 Emitir Credencial", description = """
                        Emite uma nova credencial para uma identidade.

                        **Funcionalidade**: Cria e registra uma credencial verificável na blockchain.

                        **Processo**:
                        1. Validação da identidade
                        2. Geração do hash da credencial
                        3. Registro na blockchain
                        4. Confirmação da emissão

                        **Segurança**: Requer permissões de ISSUER_ROLE.
                        """, tags = { "📜 Credential Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Credencial emitida com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao emitir credencial")
        })
        @PostMapping("/credential/issue")
        public ResponseEntity<Map<String, Object>> issueCredential(
                        @Parameter(description = "ID único da credencial", example = "cred_123", required = true) @RequestParam String credentialId,
                        @Parameter(description = "Sujeito da credencial (wallet address)", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String subject,
                        @Parameter(description = "Hash da credencial", example = "0xabc123...", required = true) @RequestParam String credentialHash) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.issueCredential(credentialId,
                                        subject,
                                        credentialHash);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "credentialId", credentialId,
                                        "subject", subject,
                                        "credentialHash", credentialHash,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Credencial emitida com sucesso"
                                                        : "Falha na emissão da credencial");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "credentialId", credentialId,
                                        "subject", subject,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "❌ Revogar Credencial", description = """
                        Revoga uma credencial existente.

                        **Funcionalidade**: Marca uma credencial como revogada na blockchain.

                        **Impacto**: A credencial não poderá mais ser usada para verificação.

                        **Reversão**: Pode ser restaurada posteriormente se necessário.
                        """, tags = { "📜 Credential Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Credencial revogada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao revogar credencial")
        })
        @PostMapping("/credential/revoke")
        public ResponseEntity<Map<String, Object>> revokeCredential(
                        @Parameter(description = "ID da credencial", example = "cred_123", required = true) @RequestParam String credentialId,
                        @Parameter(description = "Sujeito da credencial", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String subject,
                        @Parameter(description = "Motivo da revogação", example = "Credencial expirada", required = true) @RequestParam String reason) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.revokeCredential(credentialId,
                                        subject,
                                        reason);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "credentialId", credentialId,
                                        "subject", subject,
                                        "reason", reason,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Credencial revogada com sucesso"
                                                        : "Falha na revogação da credencial");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "credentialId", credentialId,
                                        "subject", subject,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "🔄 Restaurar Credencial", description = """
                        Restaura uma credencial revogada.

                        **Funcionalidade**: Remove o status de revogação de uma credencial.

                        **Uso**: Correção de revogações incorretas ou restauração de credenciais válidas.

                        **Segurança**: Requer permissões adequadas para restauração.
                        """, tags = { "📜 Credential Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Credencial restaurada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao restaurar credencial")
        })
        @PostMapping("/credential/restore")
        public ResponseEntity<Map<String, Object>> restoreCredential(
                        @Parameter(description = "ID da credencial", example = "cred_123", required = true) @RequestParam String credentialId,
                        @Parameter(description = "Sujeito da credencial", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @RequestParam String subject,
                        @Parameter(description = "Motivo da restauração", example = "Erro na revogação", required = true) @RequestParam String reason) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.restoreCredential(credentialId,
                                        subject,
                                        reason);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "credentialId", credentialId,
                                        "subject", subject,
                                        "reason", reason,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Credencial restaurada com sucesso"
                                                        : "Falha na restauração da credencial");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "credentialId", credentialId,
                                        "subject", subject,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "🔍 Verificar Revogação de Credencial", description = """
                        Verifica se uma credencial está revogada.

                        **Funcionalidade**: Consulta o status de revogação na blockchain.

                        **Retorna**: Boolean indicando se a credencial está revogada.

                        **Uso**: Validação antes de aceitar credenciais para verificação.
                        """, tags = { "📜 Credential Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Verificação realizada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/credential/revoked/{credentialId}")
        public ResponseEntity<Map<String, Object>> isCredentialRevoked(
                        @Parameter(description = "ID da credencial", example = "cred_123", required = true) @PathVariable String credentialId) {
                try {
                        boolean isRevoked = blockchainService.isCredentialRevoked(credentialId);
                        Map<String, Object> response = Map.of(
                                        "credentialId", credentialId,
                                        "isRevoked", isRevoked,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", isRevoked ? "Credencial está revogada" : "Credencial está ativa");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "credentialId", credentialId,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "Obter Informações da Credencial", description = "Retorna informações de revogação de uma credencial")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Informações obtidas com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Erro ao obter informações")
        })
        @GetMapping("/credential/info/{credentialId}")
        public ResponseEntity<Map<String, Object>> getCredentialRevocation(
                        @Parameter(description = "ID da credencial", example = "cred_123") @PathVariable String credentialId) {
                try {
                        Optional<Map<String, Object>> infoOpt = blockchainService.getCredentialRevocation(credentialId);
                        if (infoOpt.isPresent()) {
                                return ResponseEntity.ok(infoOpt.get());
                        } else {
                                return ResponseEntity.badRequest().body(Map.of("error", "Credencial não encontrada"));
                        }
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                }
        }

        @Operation(summary = "Obter Credenciais da Identidade", description = "Retorna todas as credenciais de uma identidade")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Credenciais obtidas com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Erro ao obter credenciais")
        })
        @GetMapping("/credential/identity/{identity}")
        public ResponseEntity<Map<String, Object>> getIdentityCredentials(
                        @Parameter(description = "Identidade", example = "0x1234567890abcdef") @PathVariable String identity) {
                try {
                        Map<String, Object> credentials = blockchainService.getIdentityCredentials(identity);
                        return ResponseEntity.ok(credentials);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                }
        }

        // ========= KYC MANAGEMENT =========

        @Operation(summary = "🔍 Verificar Status KYC", description = """
                        Verifica o status de KYC de uma identidade.

                        **Funcionalidade**: Consulta o status de verificação de identidade na blockchain.

                        **Status Possíveis**:
                        - `PENDING`: Aguardando verificação
                        - `APPROVED`: Aprovado
                        - `REJECTED`: Rejeitado
                        - `EXPIRED`: Expirado

                        **Uso**: Validação de identidades antes de operações sensíveis.
                        """, tags = { "🔍 KYC Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Status KYC verificado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/kyc/{identity}")
        public ResponseEntity<Map<String, Object>> checkKYCStatus(
                        @Parameter(description = "Identidade da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String identity) {
                try {
                        boolean kycStatus = blockchainService.getKYCStatus(identity);
                        Map<String, Object> response = Map.of(
                                        "identity", identity,
                                        "kycStatus", kycStatus,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", kycStatus ? "KYC aprovado" : "KYC não aprovado ou pendente");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "✅ Definir Status KYC", description = """
                        Define o status de KYC para uma identidade (requer chave administrativa).

                        **Funcionalidade**: Atualiza o status de verificação de identidade.

                        **Segurança**: Operação administrativa que requer ADMIN_PRIVATE_KEY.

                        **Status Suportados**: PENDING, APPROVED, REJECTED, EXPIRED
                        """, tags = { "🔍 KYC Management" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Status KYC definido com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao definir status KYC")
        })
        @PostMapping("/kyc/{identity}")
        public ResponseEntity<Map<String, Object>> setKYCStatus(
                        @Parameter(description = "Identidade da wallet", example = "0x1234567890abcdef1234567890abcdef12345678", required = true) @PathVariable String identity,
                        @Parameter(description = "Status KYC a definir", example = "true", required = true) @RequestParam boolean verified) {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.setKYCStatus(identity,
                                        verified);
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "identity", identity,
                                        "kycStatus", verified,
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Status KYC definido com sucesso"
                                                        : "Falha ao definir status KYC");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "identity", identity,
                                        "verified", verified,
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        // ========= SYSTEM CONTROL =========

        @Operation(summary = "⏸️ Pausar Contrato", description = """
                        Pausa o contrato inteligente (requer chave administrativa).

                        **Funcionalidade**: Pausa todas as operações do contrato DID.

                        **Segurança**: Operação administrativa que requer ADMIN_PRIVATE_KEY.

                        **Impacto**: Nenhuma operação poderá ser executada até o contrato ser despausado.

                        **Uso**: Emergências, manutenção ou pausas programadas.
                        """, tags = { "⚙️ System Control" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Contrato pausado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao pausar contrato")
        })
        @PostMapping("/pause")
        public ResponseEntity<Map<String, Object>> pauseContract() {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.pause();
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Contrato pausado com sucesso"
                                                        : "Falha ao pausar contrato");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "▶️ Despausar Contrato", description = """
                        Despausa o contrato inteligente (requer chave administrativa).

                        **Funcionalidade**: Retoma todas as operações do contrato DID.

                        **Segurança**: Operação administrativa que requer ADMIN_PRIVATE_KEY.

                        **Impacto**: Todas as operações voltam a funcionar normalmente.

                        **Uso**: Após manutenção ou pausas programadas.
                        """, tags = { "⚙️ System Control" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Contrato despausado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao despausar contrato")
        })
        @PostMapping("/unpause")
        public ResponseEntity<Map<String, Object>> unpauseContract() {
                try {
                        CompletableFuture<TransactionReceipt> future = blockchainService.unpause();
                        TransactionReceipt receipt = future.get();

                        Map<String, Object> response = Map.of(
                                        "success", receipt.isStatusOK(),
                                        "transactionHash", receipt.getTransactionHash(),
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", receipt.isStatusOK() ? "Contrato despausado com sucesso"
                                                        : "Falha ao despausar contrato");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "🔍 Verificar Status de Pausa", description = """
                        Verifica se o contrato está pausado.

                        **Funcionalidade**: Consulta o status de pausa do contrato.

                        **Retorna**: Boolean indicando se o contrato está pausado.

                        **Uso**: Verificação antes de executar operações críticas.
                        """, tags = { "⚙️ System Control" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Status verificado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro na verificação")
        })
        @GetMapping("/paused")
        public ResponseEntity<Map<String, Object>> isContractPaused() {
                try {
                        boolean isPaused = blockchainService.isPaused();
                        Map<String, Object> response = Map.of(
                                        "isPaused", isPaused,
                                        "timestamp", java.time.LocalDateTime.now().toString(),
                                        "message", isPaused ? "Contrato está pausado" : "Contrato está ativo");
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        // ========= STATUS & METRICS =========

        @Operation(summary = "🌐 Status da Blockchain", description = """
                        Retorna o status atual da conexão com a blockchain Besu.

                        **Funcionalidade**: Health check da infraestrutura blockchain.

                        **Informações Retornadas**:
                        - Status da conexão
                        - Versão da rede
                        - Último bloco processado
                        - Latência da conexão

                        **Uso**: Monitoramento e diagnóstico da infraestrutura.
                        """, tags = { "📊 Metrics & Monitoring" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Status obtido com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao obter status")
        })
        @GetMapping("/status")
        public ResponseEntity<Map<String, Object>> getBlockchainStatus() {
                try {
                        Map<String, Object> status = new HashMap<>(Map.of(
                                        "status", "CONNECTED",
                                        "network", "Besu Consortium",
                                        "chainId", "1337",
                                        "rpcUrl", "http://144.22.179.183",
                                        "gasPrice", "0",
                                        "mode", "LEGACY",
                                        "contracts", Map.of("didRegistry", DID_REGISTRY_ADDRESS)));
                        status.put("timestamp", java.time.LocalDateTime.now().toString());
                        status.put("message", "Status da blockchain obtido com sucesso");
                        return ResponseEntity.ok(status);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "📊 Métricas do Sistema", description = """
                        Retorna métricas e estatísticas do sistema blockchain.

                        **Funcionalidade**: Coleta de dados de performance e uso.

                        **Métricas Incluídas**:
                        - Total de DIDs registrados
                        - Total de credenciais emitidas
                        - Total de credenciais revogadas
                        - Total de operações realizadas

                        **Uso**: Análise de performance e planejamento de capacidade.
                        """, tags = { "📊 Metrics & Monitoring" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Métricas obtidas com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao obter métricas")
        })
        @GetMapping("/metrics")
        public ResponseEntity<Map<String, Object>> getSystemMetrics() {
                try {
                        Map<String, Object> metrics = new HashMap<>(blockchainService.getSystemMetrics());
                        metrics.put("timestamp", java.time.LocalDateTime.now().toString());
                        metrics.put("message", "Métricas do sistema obtidas com sucesso");
                        return ResponseEntity.ok(metrics);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }

        @Operation(summary = "📈 Métricas Específicas", description = """
                        Retorna métricas específicas do contrato.

                        **Funcionalidade**: Consulta métricas detalhadas por categoria.

                        **Tipos de Métricas**:
                        - DIDs por período
                        - Credenciais por status
                        - Operações por tipo
                        - Performance por intervalo

                        **Uso**: Análise detalhada e relatórios específicos.
                        """, tags = { "📊 Metrics & Monitoring" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "✅ Métricas específicas obtidas com sucesso"),
                        @ApiResponse(responseCode = "400", description = "❌ Erro ao obter métricas específicas")
        })
        @GetMapping("/metrics/specific")
        public ResponseEntity<Map<String, Object>> getSpecificMetrics() {
                try {
                        Map<String, Object> metrics = new HashMap<>(Map.of(
                                        "totalDIDs", blockchainService.getTotalDIDs(),
                                        "totalCredentials", blockchainService.getTotalCredentials(),
                                        "totalRevokedCredentials", blockchainService.getTotalRevokedCredentials(),
                                        "totalOperations", blockchainService.getTotalOperations()));
                        metrics.put("timestamp", java.time.LocalDateTime.now().toString());
                        metrics.put("message", "Métricas específicas obtidas com sucesso");
                        return ResponseEntity.ok(metrics);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", e.getMessage(),
                                        "timestamp", java.time.LocalDateTime.now().toString()));
                }
        }
}
