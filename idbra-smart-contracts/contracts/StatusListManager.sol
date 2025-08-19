// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {RegistryAccess} from "./RegistryAccess.sol";

/**
 * @title StatusListManager
 * @notice Âncora de StatusList 2021 (bitstring off-chain) para VCs.
 *         Armazena {uri, hash, version, size, purpose} por listId.
 *         - Dados pessoais ficam off-chain (LGPD).
 */
contract StatusListManager is RegistryAccess {
    struct ListMeta {
        string  uri;       // URI off-chain (ex: S3/MinIO) da lista (bitstring)
        bytes32 hash;      // hash do conteúdo versionado (ex: keccak256 do arquivo)
        uint256 version;   // versão monotônica
        uint64  size;      // tamanho em bits (opcional, referencia/consistencia)
        bytes32 purpose;   // ex: keccak256("revocation") / "suspension"
        address issuer;    // quem controla a lista (deve ter ISSUER_ROLE)
        bool    exists;
    }

    mapping(bytes32 => ListMeta) private lists; // key = keccak256(listId) (listId arbitrario: string/UUID)
    event StatusListCreated(bytes32 indexed key, string listId, string uri, bytes32 hash, uint256 version, uint64 size, bytes32 purpose, address issuer);
    event StatusListUpdated(bytes32 indexed key, string listId, string uri, bytes32 hash, uint256 version, address issuer);
    event StatusListControllerTransferred(bytes32 indexed key, string listId, address oldIssuer, address newIssuer);

    error ListAlreadyExists();
    error ListNotFound();
    error NotIssuer();
    error InvalidVersion();

    constructor(address admin) RegistryAccess(admin) {}

    function _key(string memory listId) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(listId));
    }

    modifier onlyListIssuer(bytes32 key) {
        if (!lists[key].exists) revert ListNotFound();
        if (lists[key].issuer != msg.sender) revert NotIssuer();
        _;
    }

    function getList(string calldata listId) external view returns (ListMeta memory) {
        return lists[_key(listId)];
    }

    /**
     * @notice Cria nova lista (primeira versao). Emissor precisa ISSUER_ROLE.
     */
    function createList(
        string calldata listId,
        string calldata uri,
        bytes32 hash_,
        uint64 size,
        bytes32 purpose
    ) external whenNotPaused onlyRole(ISSUER_ROLE) {
        bytes32 key = _key(listId);
        if (lists[key].exists) revert ListAlreadyExists();

        lists[key] = ListMeta({
            uri: uri,
            hash: hash_,
            version: 1,
            size: size,
            purpose: purpose,
            issuer: msg.sender,
            exists: true
        });

        emit StatusListCreated(key, listId, uri, hash_, 1, size, purpose, msg.sender);
    }

    /**
     * @notice Publica/atualiza versão da lista. Emissor deve ser o controlador atual.
     * @dev version deve ser estritamente maior (monotônico).
     */
    function publish(
        string calldata listId,
        uint256 newVersion,
        string calldata newUri,
        bytes32 newHash
    ) external whenNotPaused onlyListIssuer(_key(listId)) {
        bytes32 key = _key(listId);
        if (newVersion <= lists[key].version) revert InvalidVersion();

        lists[key].version = newVersion;
        lists[key].uri     = newUri;
        lists[key].hash    = newHash;

        emit StatusListUpdated(key, listId, newUri, newHash, newVersion, msg.sender);
    }

    /**
     * @notice Transferencia de controle da lista (governanca/admin).
     */
    function transferListController(
        string calldata listId,
        address newIssuer
    ) external whenNotPaused onlyRole(DEFAULT_ADMIN_ROLE) {
        if (newIssuer == address(0)) revert NotIssuer();
        bytes32 key = _key(listId);
        if (!lists[key].exists) revert ListNotFound();

        address old = lists[key].issuer;
        lists[key].issuer = newIssuer;
        emit StatusListControllerTransferred(key, listId, old, newIssuer);
    }
}
