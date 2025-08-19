// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {RegistryAccess} from "./RegistryAccess.sol";

/**
 * @title DIDRegistry
 * @notice Registro simples de DID estilo EIP-1056 para consorcio Besu.
 *         - Controller por identidade (address)
 *         - Eventos de atributos para ancoragem (sem DCP)
 */
contract DIDRegistry is RegistryAccess {
    /// @dev Ultimo bloco que mudou cada identidade (compatibilidade com indexacao)
    mapping(address => uint256) public changed;

    /// @dev Controlador atual de cada identidade (address DID)
    mapping(address => address) public controllers;

    /// @dev Ultima ancora armazenada por nome->hash(valor)
    mapping(address => mapping(bytes32 => bytes32)) public lastAttrValueHash;

    /// Eventos (compat EIP-1056-ish)
    event DIDOwnerChanged(address indexed identity, address owner, uint256 previousChange);
    event DIDAttributeChanged(
        address indexed identity,
        bytes32 name,
        bytes value,
        uint256 validTo,
        uint256 previousChange
    );
    event DIDAttributeRevoked(
        address indexed identity,
        bytes32 name,
        bytes value,
        uint256 previousChange
    );

    error NotController();
    error ZeroAddress();
    error Forbidden();

    constructor(address admin) RegistryAccess(admin) {}

    // ========= Helpers =========
    function _isController(address identity, address caller) internal view returns (bool) {
        address ctrl = controllers[identity];
        return (ctrl == address(0) && caller == identity) || (ctrl != address(0) && caller == ctrl);
    }

    modifier onlyController(address identity) {
        if (!_isController(identity, msg.sender)) revert NotController();
        _;
    }

    // ========= Controller =========
    function setController(address identity, address newController) external whenNotPaused onlyController(identity) {
        if (newController == address(0)) revert ZeroAddress();
        uint256 prev = changed[identity];
        controllers[identity] = newController;
        changed[identity] = block.number;
        emit DIDOwnerChanged(identity, newController, prev);
    }

    /**
     * @notice Recovery/ajuste de controlador pela governança (REGISTRAR_ROLE).
     */
    function adminSetController(address identity, address newController) external whenNotPaused onlyRole(REGISTRAR_ROLE) {
        if (newController == address(0)) revert ZeroAddress();
        uint256 prev = changed[identity];
        controllers[identity] = newController;
        changed[identity] = block.number;
        emit DIDOwnerChanged(identity, newController, prev);
    }

    // ========= Atributos (anchor only, sem DCP) =========
    function setAttribute(
        address identity,
        bytes32 name,
        bytes calldata value,
        uint256 validTo
    ) external whenNotPaused onlyController(identity) {
        uint256 prev = changed[identity];
        changed[identity] = block.number;

        // Armazena apenas hash para leitura rapida opcional
        lastAttrValueHash[identity][name] = keccak256(value);

        emit DIDAttributeChanged(identity, name, value, validTo, prev);
    }

    function revokeAttribute(
        address identity,
        bytes32 name,
        bytes calldata value
    ) external whenNotPaused onlyController(identity) {
        uint256 prev = changed[identity];
        changed[identity] = block.number;

        // Zera hash se corresponder
        bytes32 h = keccak256(value);
        if (lastAttrValueHash[identity][name] == h) {
            lastAttrValueHash[identity][name] = bytes32(0);
        }

        emit DIDAttributeRevoked(identity, name, value, prev);
    }
}
