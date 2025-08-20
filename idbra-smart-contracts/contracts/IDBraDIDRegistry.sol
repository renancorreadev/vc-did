// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {ReentrancyGuard} from "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * @title IDBraUnifiedRegistry
 * @notice Contrato unificado EIP-1056 + StatusList + Revogação para Hyperledger Besu
 * @dev Implementação completa para identidade digital bancária com máxima rastreabilidade
 */
contract IDBraDIDRegistry is AccessControl, Pausable, ReentrancyGuard {

    // ========= Roles =========
    bytes32 public constant REGISTRAR_ROLE = keccak256("REGISTRAR_ROLE");
    bytes32 public constant ISSUER_ROLE = keccak256("ISSUER_ROLE");
    bytes32 public constant AUDITOR_ROLE = keccak256("AUDITOR_ROLE");
    bytes32 public constant EMERGENCY_ROLE = keccak256("EMERGENCY_ROLE");

    // ========= EIP-1056 Core State =========

    /// @dev Mapeia identidade para delegates por tipo e validade (EIP-1056 padrão)
    mapping(address => mapping(bytes32 => mapping(address => uint256))) public delegates;

    /// @dev Último bloco que mudou cada identidade (EIP-1056 padrão)
    mapping(address => uint256) public changed;

    // ========= Identity Management State =========

    /// @dev Status de verificação KYC para compliance bancária
    mapping(address => bool) public isKYCVerified;

    /// @dev Metadata IPFS para documentos DID W3C
    mapping(address => string) public didDocuments;

    /// @dev Controle de existência de DID
    mapping(address => bool) public didExists;

    /// @dev Timestamps para auditoria
    mapping(address => uint256) public lastActivity;

    // ========= Credential Revocation State =========

    struct RevocationRecord {
        bool revoked;
        uint256 timestamp;
        address revoker;
        string reason;
        bytes32 credentialHash; // Hash da credencial para verificação
    }

    /// @dev Registro de revogações de credenciais
    mapping(bytes32 => RevocationRecord) public credentialRevocations;

    /// @dev Mapeamento de identidade para suas credenciais
    mapping(address => bytes32[]) public identityCredentials;

    /// @dev Contador de credenciais por identidade
    mapping(address => uint256) public credentialCount;



    // ========= Metrics State =========

    uint256 public totalDIDs;
    uint256 public totalVerifiedDIDs;
    uint256 public totalCredentials;
    uint256 public totalRevokedCredentials;
    uint256 public totalOperations;

    // ========= EIP-1056 Standard Events =========

    event DIDOwnerChanged(
        address indexed identity,
        address owner,
        uint256 previousChange
    );

    event DIDDelegateChanged(
        address indexed identity,
        bytes32 delegateType,
        address delegate,
        uint256 validTo,
        uint256 previousChange
    );

    event DIDAttributeChanged(
        address indexed identity,
        bytes32 name,
        bytes value,
        uint256 validTo,
        uint256 previousChange
    );

    // ========= Identity Events =========

    event DIDCreated(
        address indexed identity,
        address indexed creator,
        string didDocument,
        uint256 timestamp
    );

    event DIDUpdated(
        address indexed identity,
        address indexed updater,
        string newDocument,
        uint256 timestamp
    );

    event KYCStatusChanged(
        address indexed identity,
        bool verified,
        address indexed verifier,
        uint256 timestamp
    );

    // ========= Credential Events =========

    event CredentialIssued(
        bytes32 indexed credentialId,
        address indexed issuer,
        address indexed subject,
        bytes32 credentialHash,
        uint256 timestamp
    );

    event CredentialRevoked(
        bytes32 indexed credentialId,
        address indexed revoker,
        address indexed subject,
        string reason,
        uint256 timestamp
    );

    event CredentialRestored(
        bytes32 indexed credentialId,
        address indexed restorer,
        address indexed subject,
        string reason,
        uint256 timestamp
    );

    // ========= Audit Events =========

    event BankingAuditLog(
        address indexed identity,
        string indexed action,
        address indexed actor,
        bytes32 dataHash,
        uint256 timestamp
    );

    event SystemMetricsUpdated(
        uint256 totalDIDs,
        uint256 totalCredentials,
        uint256 totalRevoked,
        uint256 timestamp
    );

    // ========= Errors =========

    error NotOwner();
    error DIDAlreadyExists();
    error DIDNotFound();
    error InvalidDocument();
    error CredentialAlreadyExists();
    error CredentialNotFound();
    error CredentialAlreadyRevoked();
    error CredentialNotRevoked();

    // ========= Modifiers =========

    modifier onlyOwner(address identity, address actor) {
        if (actor != identity) revert NotOwner();
        _;
    }

    modifier didMustExist(address identity) {
        if (!didExists[identity]) revert DIDNotFound();
        _;
    }

    modifier credentialMustExist(bytes32 credentialId) {
        if (credentialRevocations[credentialId].credentialHash == bytes32(0)) revert CredentialNotFound();
        _;
    }

    // ========= Constructor =========

    constructor(address admin) {
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(EMERGENCY_ROLE, admin);
    }

    // ========= EIP-1056 Core Functions =========

    /**
     * @notice Retorna o owner atual de uma identidade (EIP-1056)
     */
    function identityOwner(address identity) public pure returns (address) {
        return identity;
    }

    /**
     * @notice Verifica se address é delegate válido para identidade (EIP-1056)
     */
    function validDelegate(
        address identity,
        bytes32 delegateType,
        address delegate
    ) public view returns (bool) {
        return delegates[identity][delegateType][delegate] >= block.timestamp;
    }

    /**
     * @notice Retorna timestamp de validade do delegate
     */
    function validDelegateFrom(
        address identity,
        bytes32 delegateType,
        address delegate
    ) public view returns (uint256) {
        return delegates[identity][delegateType][delegate];
    }

    /**
     * @notice Adiciona delegate para identidade (EIP-1056)
     */
    function addDelegate(
        address identity,
        bytes32 delegateType,
        address delegate,
        uint256 validity
    ) external whenNotPaused onlyOwner(identity, msg.sender) didMustExist(identity) {
        uint256 prev = changed[identity];
        delegates[identity][delegateType][delegate] = block.timestamp + validity;
        changed[identity] = block.number;
        lastActivity[identity] = block.timestamp;
        totalOperations++;

        emit DIDDelegateChanged(identity, delegateType, delegate, block.timestamp + validity, prev);
        emit BankingAuditLog(
            identity,
            "DELEGATE_ADDED",
            msg.sender,
            keccak256(abi.encodePacked(delegateType, delegate, validity)),
            block.timestamp
        );
    }

    /**
     * @notice Remove delegate da identidade (EIP-1056)
     */
    function revokeDelegate(
        address identity,
        bytes32 delegateType,
        address delegate
    ) external whenNotPaused onlyOwner(identity, msg.sender) didMustExist(identity) {
        uint256 prev = changed[identity];
        delegates[identity][delegateType][delegate] = block.timestamp;
        changed[identity] = block.number;
        lastActivity[identity] = block.timestamp;
        totalOperations++;

        emit DIDDelegateChanged(identity, delegateType, delegate, block.timestamp, prev);
        emit BankingAuditLog(
            identity,
            "DELEGATE_REVOKED",
            msg.sender,
            keccak256(abi.encodePacked(delegateType, delegate)),
            block.timestamp
        );
    }

    /**
     * @notice Define atributo para identidade (EIP-1056)
     */
    function setAttribute(
        address identity,
        bytes32 name,
        bytes calldata value,
        uint256 validity
    ) external whenNotPaused onlyOwner(identity, msg.sender) didMustExist(identity) {
        uint256 prev = changed[identity];
        changed[identity] = block.number;
        lastActivity[identity] = block.timestamp;
        totalOperations++;

        emit DIDAttributeChanged(identity, name, value, block.timestamp + validity, prev);
        emit BankingAuditLog(
            identity,
            "ATTRIBUTE_SET",
            msg.sender,
            keccak256(abi.encodePacked(name, value)),
            block.timestamp
        );
    }

    /**
     * @notice Remove atributo da identidade (EIP-1056)
     */
    function revokeAttribute(
        address identity,
        bytes32 name,
        bytes calldata value
    ) external whenNotPaused onlyOwner(identity, msg.sender) didMustExist(identity) {
        uint256 prev = changed[identity];
        changed[identity] = block.number;
        lastActivity[identity] = block.timestamp;
        totalOperations++;

        emit DIDAttributeChanged(identity, name, value, 0, prev);
        emit BankingAuditLog(
            identity,
            "ATTRIBUTE_REVOKED",
            msg.sender,
            keccak256(abi.encodePacked(name, value)),
            block.timestamp
        );
    }

    // ========= Identity Management Functions =========

    /**
     * @notice Cria nova identidade DID
     */
    function createDID(
        address identity,
        string calldata didDocument
    ) external whenNotPaused nonReentrant returns (bool) {
        if (didExists[identity]) revert DIDAlreadyExists();
        if (bytes(didDocument).length == 0) revert InvalidDocument();

        didExists[identity] = true;
        didDocuments[identity] = didDocument;
        lastActivity[identity] = block.timestamp;

        totalDIDs++;
        totalOperations++;

        uint256 prev = changed[identity];
        changed[identity] = block.number;

        emit DIDCreated(identity, msg.sender, didDocument, block.timestamp);
        emit DIDOwnerChanged(identity, identity, prev);
        emit BankingAuditLog(
            identity,
            "DID_CREATED",
            msg.sender,
            keccak256(bytes(didDocument)),
            block.timestamp
        );

        return true;
    }

    /**
     * @notice Atualiza documento DID
     */
    function updateDIDDocument(
        address identity,
        string calldata newDidDocument
    ) external whenNotPaused onlyOwner(identity, msg.sender) didMustExist(identity) {
        if (bytes(newDidDocument).length == 0) revert InvalidDocument();

        string memory oldDocument = didDocuments[identity];
        didDocuments[identity] = newDidDocument;
        lastActivity[identity] = block.timestamp;
        totalOperations++;

        emit DIDUpdated(identity, msg.sender, newDidDocument, block.timestamp);
        emit BankingAuditLog(
            identity,
            "DID_UPDATED",
            msg.sender,
            keccak256(abi.encodePacked(oldDocument, newDidDocument)),
            block.timestamp
        );
    }

    /**
     * @notice Define status KYC
     */
    function setKYCStatus(
        address identity,
        bool verified
    ) external whenNotPaused onlyRole(REGISTRAR_ROLE) didMustExist(identity) {
        bool wasVerified = isKYCVerified[identity];
        isKYCVerified[identity] = verified;
        lastActivity[identity] = block.timestamp;
        totalOperations++;

        if (verified && !wasVerified) {
            totalVerifiedDIDs++;
        } else if (!verified && wasVerified) {
            totalVerifiedDIDs--;
        }

        emit KYCStatusChanged(identity, verified, msg.sender, block.timestamp);
        emit BankingAuditLog(
            identity,
            verified ? "KYC_VERIFIED" : "KYC_REVOKED",
            msg.sender,
            keccak256(abi.encodePacked(verified, wasVerified)),
            block.timestamp
        );
    }

    // ========= Credential Management Functions =========

    /**
     * @notice Registra uma nova credencial
     */
    function issueCredential(
        bytes32 credentialId,
        address subject,
        bytes32 credentialHash
    ) external whenNotPaused onlyRole(ISSUER_ROLE) didMustExist(subject) returns (bool) {
        if (credentialRevocations[credentialId].credentialHash != bytes32(0)) revert CredentialAlreadyExists();

        credentialRevocations[credentialId] = RevocationRecord({
            revoked: false,
            timestamp: block.timestamp,
            revoker: msg.sender,
            reason: "",
            credentialHash: credentialHash
        });

        identityCredentials[subject].push(credentialId);
        credentialCount[subject]++;
        totalCredentials++;
        totalOperations++;

        emit CredentialIssued(credentialId, msg.sender, subject, credentialHash, block.timestamp);
        emit BankingAuditLog(
            subject,
            "CREDENTIAL_ISSUED",
            msg.sender,
            credentialHash,
            block.timestamp
        );

        return true;
    }

    /**
     * @notice Revoga uma credencial
     */
    function revokeCredential(
        bytes32 credentialId,
        address subject,
        string calldata reason
    ) external whenNotPaused onlyRole(ISSUER_ROLE) credentialMustExist(credentialId) {
        RevocationRecord storage record = credentialRevocations[credentialId];
        if (record.revoked) revert CredentialAlreadyRevoked();

        record.revoked = true;
        record.timestamp = block.timestamp;
        record.revoker = msg.sender;
        record.reason = reason;

        totalRevokedCredentials++;
        totalOperations++;

        emit CredentialRevoked(credentialId, msg.sender, subject, reason, block.timestamp);
        emit BankingAuditLog(
            subject,
            "CREDENTIAL_REVOKED",
            msg.sender,
            keccak256(abi.encodePacked(credentialId, reason)),
            block.timestamp
        );
    }

    /**
     * @notice Restaura uma credencial revogada
     */
    function restoreCredential(
        bytes32 credentialId,
        address subject,
        string calldata reason
    ) external whenNotPaused onlyRole(ISSUER_ROLE) credentialMustExist(credentialId) {
        RevocationRecord storage record = credentialRevocations[credentialId];
        if (!record.revoked) revert CredentialNotRevoked();

        record.revoked = false;
        record.timestamp = block.timestamp;
        record.revoker = msg.sender;
        record.reason = reason;

        totalRevokedCredentials--;
        totalOperations++;

        emit CredentialRestored(credentialId, msg.sender, subject, reason, block.timestamp);
        emit BankingAuditLog(
            subject,
            "CREDENTIAL_RESTORED",
            msg.sender,
            keccak256(abi.encodePacked(credentialId, reason)),
            block.timestamp
        );
    }

    /**
     * @notice Verifica se uma credencial está revogada
     */
    function isCredentialRevoked(bytes32 credentialId) external view returns (bool) {
        return credentialRevocations[credentialId].revoked;
    }

    // ========= View Functions =========

    /**
     * @notice Retorna informações completas da identidade
     */
    function getIdentityInfo(address identity) external view returns (
        address owner,
        string memory didDocument,
        bool kycVerified,
        uint256 lastActivityTime,
        uint256 lastChanged,
        uint256 credentialCountForIdentity
    ) {
        return (
            identity,
            didDocuments[identity],
            isKYCVerified[identity],
            lastActivity[identity],
            changed[identity],
            credentialCount[identity]
        );
    }

    /**
     * @notice Retorna registro de revogação de credencial
     */
    function getCredentialRevocation(bytes32 credentialId) external view returns (RevocationRecord memory) {
        return credentialRevocations[credentialId];
    }

    /**
     * @notice Retorna credenciais de uma identidade
     */
    function getIdentityCredentials(address identity) external view returns (bytes32[] memory) {
        return identityCredentials[identity];
    }

    /**
     * @notice Retorna métricas do sistema
     */
    function getSystemMetrics() external view returns (
        uint256 totalDIDCount,
        uint256 totalVerifiedDIDCount,
        uint256 totalCredentialCount,
        uint256 totalRevokedCredentialCount,
        uint256 totalOperationCount
    ) {
        return (
            totalDIDs,
            totalVerifiedDIDs,
            totalCredentials,
            totalRevokedCredentials,
            totalOperations
        );
    }

    /**
     * @notice Verifica se DID existe
     */
    function exists(address identity) external view returns (bool) {
        return didExists[identity];
    }

    // ========= Admin Functions =========

    function pause() external onlyRole(EMERGENCY_ROLE) {
        _pause();
        emit BankingAuditLog(msg.sender, "SYSTEM_PAUSED", msg.sender, bytes32(0), block.timestamp);
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
        emit BankingAuditLog(msg.sender, "SYSTEM_UNPAUSED", msg.sender, bytes32(0), block.timestamp);
    }
}
