// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";

/**
 * @title RegistryAccess
 * @notice Base de acesso para o consorcio (roles + pausa).
 *
 * Roles:
 * - DEFAULT_ADMIN_ROLE: governanca do consorcio
 * - REGISTRAR_ROLE: operadores do registro DID (recovery/ajustes)
 * - ISSUER_ROLE: emissores autorizados a publicar StatusLists
 */
abstract contract RegistryAccess is AccessControl, Pausable {
    bytes32 public constant REGISTRAR_ROLE = keccak256("REGISTRAR_ROLE");
    bytes32 public constant ISSUER_ROLE    = keccak256("ISSUER_ROLE");

    constructor(address admin) {
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
    }

    /// Pausa/despausa por admin. Os eventos sao emitidos dentro de _pause/_unpause.
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }
}
