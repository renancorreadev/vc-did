import assert from "node:assert/strict";
import { describe, it, beforeEach } from "node:test";
import { network } from "hardhat";
import { keccak256, toBytes, getAddress, encodePacked, stringToHex } from "viem";

describe("IDBraUnifiedRegistry - Complete Verifiable Credentials Flow", async function () {
    const { viem } = await network.connect();
    const publicClient = await viem.getPublicClient();

    let didRegistry: any;
    let admin: any;
    let registrar: any;
    let issuer: any;
    let auditor: any;
    let identity1: any;
    let identity2: any;
    let delegate: any;
    let verifier: any;
    let nonAuthorized: any;

    // Roles
    const DEFAULT_ADMIN_ROLE = "0x0000000000000000000000000000000000000000000000000000000000000000";
    const REGISTRAR_ROLE = keccak256(toBytes("REGISTRAR_ROLE"));
    const ISSUER_ROLE = keccak256(toBytes("ISSUER_ROLE"));
    const AUDITOR_ROLE = keccak256(toBytes("AUDITOR_ROLE"));
    const EMERGENCY_ROLE = keccak256(toBytes("EMERGENCY_ROLE"));

    // Credential types for testing
    const EDUCATION_CREDENTIAL = "education_certificate";
    const BANK_ACCOUNT_CREDENTIAL = "bank_account_verification";
    const IDENTITY_CREDENTIAL = "identity_verification";

    beforeEach(async function () {
        const accounts = await viem.getWalletClients();
        admin = accounts[0];
        registrar = accounts[1];
        issuer = accounts[2];
        auditor = accounts[3];
        identity1 = accounts[4];
        identity2 = accounts[5];
        delegate = accounts[6];
        verifier = accounts[7];
        nonAuthorized = accounts[8];

        // Deploy IDBraUnifiedRegistry contract
        didRegistry = await viem.deployContract("IDBraDIDRegistry", [admin.account.address]);

        // Setup roles
        await didRegistry.write.grantRole([REGISTRAR_ROLE, registrar.account.address], {
            account: admin.account
        });
        await didRegistry.write.grantRole([ISSUER_ROLE, issuer.account.address], {
            account: admin.account
        });
        await didRegistry.write.grantRole([AUDITOR_ROLE, auditor.account.address], {
            account: admin.account
        });
    });

    describe("1. DID Registration and Management", function () {
        it("Should create DID with proper document structure", async function () {
            const didDocument = JSON.stringify({
                "@context": ["https://www.w3.org/ns/did/v1"],
                "id": `did:idbra:${identity1.account.address}`,
                "verificationMethod": [{
                    "id": `did:idbra:${identity1.account.address}#key-1`,
                    "type": "EcdsaSecp256k1VerificationKey2019",
                    "controller": `did:idbra:${identity1.account.address}`,
                    "publicKeyHex": identity1.account.address.slice(2)
                }],
                "authentication": [`did:idbra:${identity1.account.address}#key-1`],
                "service": [{
                    "id": `did:idbra:${identity1.account.address}#credential-repository`,
                    "type": "CredentialRepository",
                    "serviceEndpoint": "https://idbra.com/credentials"
                }]
            });

            await didRegistry.write.createDID([
                identity1.account.address,
                didDocument
            ], {
                account: identity1.account
            });

            // Verify DID creation
            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(getAddress(info[0]), getAddress(identity1.account.address));
            assert.equal(info[1], didDocument);
            assert.equal(info[2], false); // KYC not verified yet
            assert(info[3] > 0n); // lastActivity timestamp
            assert(info[4] > 0n); // changed block
            assert.equal(info[5], 0n); // credentialCount

            // Verify DID exists
            const exists = await didRegistry.read.exists([identity1.account.address]);
            assert.equal(exists, true);

            // Check metrics
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[0], 1n); // totalDIDs
            assert.equal(metrics[1], 0n); // totalVerifiedDIDs (KYC not done yet)
            assert.equal(metrics[2], 0n); // totalCredentials
            assert.equal(metrics[3], 0n); // totalRevokedCredentials
            assert.equal(metrics[4], 1n); // totalOperations
        });

        it("Should update DID document maintaining integrity", async function () {
            const originalDocument = `{"@context": ["https://www.w3.org/ns/did/v1"], "id": "did:idbra:${identity1.account.address}"}`;

            await didRegistry.write.createDID([
                identity1.account.address,
                originalDocument
            ], {
                account: identity1.account
            });

            const updatedDocument = JSON.stringify({
                "@context": ["https://www.w3.org/ns/did/v1"],
                "id": `did:idbra:${identity1.account.address}`,
                "updated": new Date().toISOString(),
                "verificationMethod": [{
                    "id": `did:idbra:${identity1.account.address}#key-2`,
                    "type": "EcdsaSecp256k1VerificationKey2019",
                    "controller": `did:idbra:${identity1.account.address}`,
                    "publicKeyHex": "0x1234567890abcdef"
                }]
            });

            await didRegistry.write.updateDIDDocument([
                identity1.account.address,
                updatedDocument
            ], {
                account: identity1.account
            });

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[1], updatedDocument);
        });

        it("Should handle KYC verification properly", async function () {
            await didRegistry.write.createDID([
                identity1.account.address,
                `{"id": "did:idbra:${identity1.account.address}"}`
            ], {
                account: identity1.account
            });

            // Verify KYC
            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: registrar.account
            });

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[2], true); // KYC verified

            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[1], 1n); // totalVerifiedDIDs
        });
    });

    describe("2. Credential Issuance Flow", function () {
        beforeEach(async function () {
            // Setup base DID for credential tests
            await didRegistry.write.createDID([
                identity1.account.address,
                `{"id": "did:idbra:${identity1.account.address}"}`
            ], {
                account: identity1.account
            });

            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: registrar.account
            });
        });

        it("Should issue education credential", async function () {
            const credentialData = {
                "@context": ["https://www.w3.org/2018/credentials/v1"],
                "type": ["VerifiableCredential", "EducationCredential"],
                "issuer": `did:idbra:${issuer.account.address}`,
                "issuanceDate": new Date().toISOString(),
                "credentialSubject": {
                    "id": `did:idbra:${identity1.account.address}`,
                    "degree": "Bachelor of Computer Science",
                    "institution": "IDBra University",
                    "graduationDate": "2024-06-15"
                }
            };

            const credentialHash = keccak256(stringToHex(JSON.stringify(credentialData)));
            const credentialId = keccak256(encodePacked(
                ["bytes32", "string", "address", "uint256"],
                [credentialHash, EDUCATION_CREDENTIAL, identity1.account.address, BigInt(Date.now())]
            ));

            await didRegistry.write.issueCredential([
                credentialId,
                identity1.account.address,
                credentialHash
            ], {
                account: issuer.account
            });

            // Verify credential issuance
            const revocationRecord = await didRegistry.read.getCredentialRevocation([credentialId]);
            assert.equal(revocationRecord.revoked, false);
            assert.equal(revocationRecord.credentialHash, credentialHash);
            assert.equal(getAddress(revocationRecord.revoker), getAddress(issuer.account.address));

            // Check identity credentials
            const identityCredentials = await didRegistry.read.getIdentityCredentials([identity1.account.address]);
            assert.equal(identityCredentials.length, 1);
            assert.equal(identityCredentials[0], credentialId);

            // Check metrics
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[2], 1n); // totalCredentials
            assert.equal(metrics[3], 0n); // totalRevokedCredentials

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[5], 1n); // credentialCount
        });

        it("Should issue bank account verification credential", async function () {
            const credentialData = {
                "@context": ["https://www.w3.org/2018/credentials/v1"],
                "type": ["VerifiableCredential", "BankAccountVerification"],
                "issuer": `did:idbra:banco-central`,
                "issuanceDate": new Date().toISOString(),
                "credentialSubject": {
                    "id": `did:idbra:${identity1.account.address}`,
                    "accountNumber": "****1234",
                    "bankName": "IDBra Bank",
                    "accountType": "checking",
                    "verified": true,
                    "verificationDate": new Date().toISOString()
                }
            };

            const credentialHash = keccak256(stringToHex(JSON.stringify(credentialData)));
            const credentialId = keccak256(encodePacked(
                ["bytes32", "string", "address", "uint256"],
                [credentialHash, BANK_ACCOUNT_CREDENTIAL, identity1.account.address, BigInt(Date.now())]
            ));

            await didRegistry.write.issueCredential([
                credentialId,
                identity1.account.address,
                credentialHash
            ], {
                account: issuer.account
            });

            const revocationRecord = await didRegistry.read.getCredentialRevocation([credentialId]);
            assert.equal(revocationRecord.revoked, false);
            assert.equal(revocationRecord.credentialHash, credentialHash);

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[5], 1n); // credentialCount increased
        });

        it("Should issue multiple credentials to same identity", async function () {
            const credentials = [
                {
                    type: EDUCATION_CREDENTIAL,
                    data: { degree: "Master of Science", institution: "IDBra Tech" }
                },
                {
                    type: BANK_ACCOUNT_CREDENTIAL,
                    data: { accountNumber: "****5678", bankName: "IDBra Credit Union" }
                },
                {
                    type: IDENTITY_CREDENTIAL,
                    data: { documentType: "passport", documentNumber: "****9999" }
                }
            ];

            const credentialIds = [];

            for (let i = 0; i < credentials.length; i++) {
                const cred = credentials[i];
                const credentialData = {
                    "@context": ["https://www.w3.org/2018/credentials/v1"],
                    "type": ["VerifiableCredential"],
                    "issuer": `did:idbra:${issuer.account.address}`,
                    "issuanceDate": new Date().toISOString(),
                    "credentialSubject": {
                        "id": `did:idbra:${identity1.account.address}`,
                        ...cred.data
                    }
                };

                const credentialHash = keccak256(stringToHex(JSON.stringify(credentialData)));
                const credentialId = keccak256(encodePacked(
                    ["bytes32", "string", "address", "uint256"],
                    [credentialHash, cred.type, identity1.account.address, BigInt(Date.now() + i)]
                ));

                credentialIds.push(credentialId);

                await didRegistry.write.issueCredential([
                    credentialId,
                    identity1.account.address,
                    credentialHash
                ], {
                    account: issuer.account
                });
            }

            // Verify all credentials were issued
            const identityCredentials = await didRegistry.read.getIdentityCredentials([identity1.account.address]);
            assert.equal(identityCredentials.length, 3);

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[5], 3n); // credentialCount

            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[2], 3n); // totalCredentials
        });

        it("Should prevent duplicate credential issuance", async function () {
            const credentialData = { test: "duplicate" };
            const credentialHash = keccak256(stringToHex(JSON.stringify(credentialData)));
            const credentialId = keccak256(encodePacked(
                ["bytes32", "string"],
                [credentialHash, "test"]
            ));

            // Issue first credential
            await didRegistry.write.issueCredential([
                credentialId,
                identity1.account.address,
                credentialHash
            ], {
                account: issuer.account
            });

            // Try to issue same credential again - should fail
            await assert.rejects(
                didRegistry.write.issueCredential([
                    credentialId,
                    identity1.account.address,
                    credentialHash
                ], {
                    account: issuer.account
                })
            );
        });
    });

    describe("3. Credential Verification Flow", function () {
        let educationCredentialId: any;
        let bankCredentialId: any;

        beforeEach(async function () {
            // Setup DID and credentials for verification tests
            await didRegistry.write.createDID([
                identity1.account.address,
                `{"id": "did:idbra:${identity1.account.address}"}`
            ], {
                account: identity1.account
            });

            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: registrar.account
            });

            // Issue education credential
            const educationData = {
                type: "EducationCredential",
                degree: "PhD in Blockchain Technology",
                institution: "IDBra Institute"
            };
            const educationHash = keccak256(stringToHex(JSON.stringify(educationData)));
            educationCredentialId = keccak256(encodePacked(
                ["bytes32", "string"],
                [educationHash, "education"]
            ));

            await didRegistry.write.issueCredential([
                educationCredentialId,
                identity1.account.address,
                educationHash
            ], {
                account: issuer.account
            });

            // Issue bank credential
            const bankData = {
                type: "BankVerification",
                account: "verified",
                bank: "IDBra National Bank"
            };
            const bankHash = keccak256(stringToHex(JSON.stringify(bankData)));
            bankCredentialId = keccak256(encodePacked(
                ["bytes32", "string"],
                [bankHash, "bank"]
            ));

            await didRegistry.write.issueCredential([
                bankCredentialId,
                identity1.account.address,
                bankHash
            ], {
                account: issuer.account
            });
        });

        it("Should verify active credentials", async function () {
            // Verify education credential is not revoked
            const educationRevoked = await didRegistry.read.isCredentialRevoked([educationCredentialId]);
            assert.equal(educationRevoked, false);

            // Verify bank credential is not revoked
            const bankRevoked = await didRegistry.read.isCredentialRevoked([bankCredentialId]);
            assert.equal(bankRevoked, false);

            // Get credential details
            const educationRecord = await didRegistry.read.getCredentialRevocation([educationCredentialId]);
            assert.equal(educationRecord.revoked, false);
            assert(educationRecord.credentialHash !== "0x0000000000000000000000000000000000000000000000000000000000000000");

            const bankRecord = await didRegistry.read.getCredentialRevocation([bankCredentialId]);
            assert.equal(bankRecord.revoked, false);
            assert(bankRecord.credentialHash !== "0x0000000000000000000000000000000000000000000000000000000000000000");
        });

        it("Should verify identity has correct credentials", async function () {
            const identityCredentials = await didRegistry.read.getIdentityCredentials([identity1.account.address]);
            assert.equal(identityCredentials.length, 2);
            assert(identityCredentials.includes(educationCredentialId));
            assert(identityCredentials.includes(bankCredentialId));

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[5], 2n); // credentialCount
        });

        it("Should verify KYC status for credential validation", async function () {
            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[2], true); // KYC verified

            // Verify DID exists
            const exists = await didRegistry.read.exists([identity1.account.address]);
            assert.equal(exists, true);
        });

        it("Should validate credential integrity through hash verification", async function () {
            const educationRecord = await didRegistry.read.getCredentialRevocation([educationCredentialId]);

            // Recreate the hash to verify integrity
            const educationData = {
                type: "EducationCredential",
                degree: "PhD in Blockchain Technology",
                institution: "IDBra Institute"
            };
            const expectedHash = keccak256(stringToHex(JSON.stringify(educationData)));

            assert.equal(educationRecord.credentialHash, expectedHash);
        });
    });

    describe("4. Status List Management", function () {
        let activeCredentials: any[] = [];

        beforeEach(async function () {
            // Clear previous credentials
            activeCredentials = [];

            // Setup multiple DIDs and credentials for status management
            const identities = [identity1, identity2];

            for (let i = 0; i < identities.length; i++) {
                const identity = identities[i];

                await didRegistry.write.createDID([
                    identity.account.address,
                    `{"id": "did:idbra:${identity.account.address}"}`
                ], {
                    account: identity.account
                });

                await didRegistry.write.setKYCStatus([
                    identity.account.address,
                    true
                ], {
                    account: registrar.account
                });

                // Issue 2 credentials per identity
                for (let j = 0; j < 2; j++) {
                    const credentialData = {
                        identity: identity.account.address,
                        credentialNumber: i * 2 + j,
                        type: j === 0 ? "education" : "bank"
                    };

                    const credentialHash = keccak256(stringToHex(JSON.stringify(credentialData)));
                    const credentialId = keccak256(encodePacked(
                        ["address", "uint256", "uint256"],
                        [identity.account.address, BigInt(i), BigInt(j)]
                    ));

                    await didRegistry.write.issueCredential([
                        credentialId,
                        identity.account.address,
                        credentialHash
                    ], {
                        account: issuer.account
                    });

                    activeCredentials.push({
                        id: credentialId,
                        identity: identity.account.address,
                        hash: credentialHash
                    });
                }
            }
        });

        it("Should track status of all issued credentials", async function () {
            // Verify all credentials are active
            for (const cred of activeCredentials) {
                const isRevoked = await didRegistry.read.isCredentialRevoked([cred.id]);
                assert.equal(isRevoked, false);
            }

            // Check system metrics
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[2], 4n); // totalCredentials (2 identities * 2 credentials each)
            assert.equal(metrics[3], 0n); // totalRevokedCredentials
        });

        it("Should maintain accurate status list after partial revocations", async function () {
            // Revoke first credential of identity1
            const credToRevoke = activeCredentials[0];

            await didRegistry.write.revokeCredential([
                credToRevoke.id,
                credToRevoke.identity,
                "Testing partial revocation"
            ], {
                account: issuer.account
            });

            // Verify status list accuracy
            const revokedStatus = await didRegistry.read.isCredentialRevoked([credToRevoke.id]);
            assert.equal(revokedStatus, true);

            // Verify other credentials remain active
            for (let i = 1; i < activeCredentials.length; i++) {
                const activeStatus = await didRegistry.read.isCredentialRevoked([activeCredentials[i].id]);
                assert.equal(activeStatus, false);
            }

            // Check metrics
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[2], 4n); // totalCredentials unchanged
            assert.equal(metrics[3], 1n); // totalRevokedCredentials incremented
        });

        it("Should handle bulk status operations efficiently", async function () {
            // Revoke multiple credentials
            const credentialsToRevoke = activeCredentials.slice(0, 3);

            for (const cred of credentialsToRevoke) {
                await didRegistry.write.revokeCredential([
                    cred.id,
                    cred.identity,
                    `Bulk revocation test`
                ], {
                    account: issuer.account
                });
            }

            // Verify status updates
            for (const cred of credentialsToRevoke) {
                const isRevoked = await didRegistry.read.isCredentialRevoked([cred.id]);
                assert.equal(isRevoked, true);
            }

            // Verify remaining credential is still active
            const remainingCred = activeCredentials[3];
            const isActive = await didRegistry.read.isCredentialRevoked([remainingCred.id]);
            assert.equal(isActive, false);

            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[3], 3n); // totalRevokedCredentials
        });

        it("Should provide comprehensive status list view", async function () {
            // Get all credentials for each identity
            const identity1Credentials = await didRegistry.read.getIdentityCredentials([identity1.account.address]);
            const identity2Credentials = await didRegistry.read.getIdentityCredentials([identity2.account.address]);

            assert.equal(identity1Credentials.length, 2);
            assert.equal(identity2Credentials.length, 2);

            // Verify each credential's status individually
            for (const credId of identity1Credentials) {
                const record = await didRegistry.read.getCredentialRevocation([credId]);
                assert.equal(record.revoked, false);
                assert(record.credentialHash !== "0x0000000000000000000000000000000000000000000000000000000000000000");
            }

            for (const credId of identity2Credentials) {
                const record = await didRegistry.read.getCredentialRevocation([credId]);
                assert.equal(record.revoked, false);
                assert(record.credentialHash !== "0x0000000000000000000000000000000000000000000000000000000000000000");
            }
        });
    });

    describe("5. Revocation Scenarios", function () {
        let testCredentialId: any;
        let testCredentialHash: any;

        beforeEach(async function () {
            await didRegistry.write.createDID([
                identity1.account.address,
                `{"id": "did:idbra:${identity1.account.address}"}`
            ], {
                account: identity1.account
            });

            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: registrar.account
            });

            const credentialData = {
                type: "TestCredential",
                subject: identity1.account.address,
                data: "Test credential for revocation scenarios"
            };

            testCredentialHash = keccak256(stringToHex(JSON.stringify(credentialData)));
            testCredentialId = keccak256(encodePacked(
                ["bytes32", "string"],
                [testCredentialHash, "revocation-test"]
            ));

            await didRegistry.write.issueCredential([
                testCredentialId,
                identity1.account.address,
                testCredentialHash
            ], {
                account: issuer.account
            });
        });

        it("Should revoke credential with proper reason tracking", async function () {
            const revocationReason = "Credential holder violated terms of service";

            await didRegistry.write.revokeCredential([
                testCredentialId,
                identity1.account.address,
                revocationReason
            ], {
                account: issuer.account
            });

            // Verify revocation
            const isRevoked = await didRegistry.read.isCredentialRevoked([testCredentialId]);
            assert.equal(isRevoked, true);

            // Verify revocation record details
            const revocationRecord = await didRegistry.read.getCredentialRevocation([testCredentialId]);
            assert.equal(revocationRecord.revoked, true);
            assert.equal(revocationRecord.reason, revocationReason);
            assert.equal(getAddress(revocationRecord.revoker), getAddress(issuer.account.address));
            assert.equal(revocationRecord.credentialHash, testCredentialHash);

            // Check metrics
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[3], 1n); // totalRevokedCredentials
        });

        it("Should restore revoked credential", async function () {
            // First revoke the credential
            await didRegistry.write.revokeCredential([
                testCredentialId,
                identity1.account.address,
                "Initial revocation"
            ], {
                account: issuer.account
            });

            // Verify it's revoked
            let isRevoked = await didRegistry.read.isCredentialRevoked([testCredentialId]);
            assert.equal(isRevoked, true);

            // Restore the credential
            const restorationReason = "Error in revocation - credential is valid";
            await didRegistry.write.restoreCredential([
                testCredentialId,
                identity1.account.address,
                restorationReason
            ], {
                account: issuer.account
            });

            // Verify restoration
            isRevoked = await didRegistry.read.isCredentialRevoked([testCredentialId]);
            assert.equal(isRevoked, false);

            const record = await didRegistry.read.getCredentialRevocation([testCredentialId]);
            assert.equal(record.revoked, false);
            assert.equal(record.reason, restorationReason);

            // Check metrics
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[3], 0n); // totalRevokedCredentials back to 0
        });

        it("Should handle multiple revocation-restoration cycles", async function () {
            const cycles = [
                { action: "revoke", reason: "First revocation" },
                { action: "restore", reason: "First restoration" },
                { action: "revoke", reason: "Second revocation" },
                { action: "restore", reason: "Second restoration" },
                { action: "revoke", reason: "Final revocation" }
            ];

            for (const cycle of cycles) {
                if (cycle.action === "revoke") {
                    await didRegistry.write.revokeCredential([
                        testCredentialId,
                        identity1.account.address,
                        cycle.reason
                    ], {
                        account: issuer.account
                    });

                    const isRevoked = await didRegistry.read.isCredentialRevoked([testCredentialId]);
                    assert.equal(isRevoked, true);
                } else {
                    await didRegistry.write.restoreCredential([
                        testCredentialId,
                        identity1.account.address,
                        cycle.reason
                    ], {
                        account: issuer.account
                    });

                    const isRevoked = await didRegistry.read.isCredentialRevoked([testCredentialId]);
                    assert.equal(isRevoked, false);
                }

                const record = await didRegistry.read.getCredentialRevocation([testCredentialId]);
                assert.equal(record.reason, cycle.reason);
            }

            // Final state should be revoked
            const finalStatus = await didRegistry.read.isCredentialRevoked([testCredentialId]);
            assert.equal(finalStatus, true);
        });

        it("Should prevent unauthorized revocation attempts", async function () {
            // Try to revoke without ISSUER_ROLE
            await assert.rejects(
                didRegistry.write.revokeCredential([
                    testCredentialId,
                    identity1.account.address,
                    "Unauthorized revocation attempt"
                ], {
                    account: nonAuthorized.account
                })
            );

            // Verify credential is still active
            const isRevoked = await didRegistry.read.isCredentialRevoked([testCredentialId]);
            assert.equal(isRevoked, false);
        });

        it("Should prevent double revocation", async function () {
            // Revoke once
            await didRegistry.write.revokeCredential([
                testCredentialId,
                identity1.account.address,
                "First revocation"
            ], {
                account: issuer.account
            });

            // Try to revoke again - should fail
            await assert.rejects(
                didRegistry.write.revokeCredential([
                    testCredentialId,
                    identity1.account.address,
                    "Second revocation attempt"
                ], {
                    account: issuer.account
                })
            );
        });

        it("Should prevent restoration of non-revoked credential", async function () {
            // Try to restore credential that was never revoked
            await assert.rejects(
                didRegistry.write.restoreCredential([
                    testCredentialId,
                    identity1.account.address,
                    "Attempting to restore active credential"
                ], {
                    account: issuer.account
                })
            );
        });
    });

    describe("6. Complete End-to-End Verifiable Credentials Flow", function () {
        it("Should execute complete banking onboarding with verifiable credentials", async function () {
            console.log("=== Complete Banking Onboarding with Verifiable Credentials ===");

            // 1. Customer creates DID
            const customerDIDDocument = JSON.stringify({
                "@context": ["https://www.w3.org/ns/did/v1"],
                "id": `did:idbra:${identity1.account.address}`,
                "verificationMethod": [{
                    "id": `did:idbra:${identity1.account.address}#key-1`,
                    "type": "EcdsaSecp256k1VerificationKey2019",
                    "controller": `did:idbra:${identity1.account.address}`,
                    "publicKeyHex": identity1.account.address.slice(2)
                }],
                "authentication": [`did:idbra:${identity1.account.address}#key-1`],
                "service": [{
                    "id": `did:idbra:${identity1.account.address}#credential-repository`,
                    "type": "CredentialRepository",
                    "serviceEndpoint": "https://idbra.com/credentials"
                }]
            });

            await didRegistry.write.createDID([
                identity1.account.address,
                customerDIDDocument
            ], {
                account: identity1.account
            });
            console.log("✅ Customer DID created");

            // 2. Bank verifies KYC
            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: registrar.account
            });
            console.log("✅ KYC verification completed");

            // 3. Issue identity verification credential
            const identityCredentialData = {
                "@context": ["https://www.w3.org/2018/credentials/v1"],
                "type": ["VerifiableCredential", "IdentityCredential"],
                "issuer": `did:idbra:banco-central`,
                "issuanceDate": new Date().toISOString(),
                "credentialSubject": {
                    "id": `did:idbra:${identity1.account.address}`,
                    "givenName": "João",
                    "familyName": "Silva",
                    "documentType": "CPF",
                    "documentNumber": "***.***.***-**",
                    "verificationLevel": "high",
                    "kycCompleted": true
                }
            };

            const identityHash = keccak256(stringToHex(JSON.stringify(identityCredentialData)));
            const identityCredentialId = keccak256(encodePacked(
                ["bytes32", "string", "address"],
                [identityHash, "identity", identity1.account.address]
            ));

            await didRegistry.write.issueCredential([
                identityCredentialId,
                identity1.account.address,
                identityHash
            ], {
                account: issuer.account
            });
            console.log("✅ Identity credential issued");

            // 4. Issue bank account credential
            const bankCredentialData = {
                "@context": ["https://www.w3.org/2018/credentials/v1"],
                "type": ["VerifiableCredential", "BankAccountCredential"],
                "issuer": `did:idbra:banco-do-brasil`,
                "issuanceDate": new Date().toISOString(),
                "credentialSubject": {
                    "id": `did:idbra:${identity1.account.address}`,
                    "accountNumber": "****-****-****-1234",
                    "accountType": "checking",
                    "bankName": "Banco do Brasil",
                    "accountStatus": "active",
                    "openDate": "2024-01-15"
                }
            };

            const bankHash = keccak256(stringToHex(JSON.stringify(bankCredentialData)));
            const bankCredentialId = keccak256(encodePacked(
                ["bytes32", "string", "address"],
                [bankHash, "bank-account", identity1.account.address]
            ));

            await didRegistry.write.issueCredential([
                bankCredentialId,
                identity1.account.address,
                bankHash
            ], {
                account: issuer.account
            });
            console.log("✅ Bank account credential issued");

            // 5. Issue credit score credential
            const creditCredentialData = {
                "@context": ["https://www.w3.org/2018/credentials/v1"],
                "type": ["VerifiableCredential", "CreditScoreCredential"],
                "issuer": `did:idbra:serasa`,
                "issuanceDate": new Date().toISOString(),
                "credentialSubject": {
                    "id": `did:idbra:${identity1.account.address}`,
                    "creditScore": 750,
                    "scoreRange": "600-850",
                    "riskLevel": "low",
                    "lastUpdated": new Date().toISOString()
                }
            };

            const creditHash = keccak256(stringToHex(JSON.stringify(creditCredentialData)));
            const creditCredentialId = keccak256(encodePacked(
                ["bytes32", "string", "address"],
                [creditHash, "credit-score", identity1.account.address]
            ));

            await didRegistry.write.issueCredential([
                creditCredentialId,
                identity1.account.address,
                creditHash
            ], {
                account: issuer.account
            });
            console.log("✅ Credit score credential issued");

            // 6. Verify all credentials are active and valid
            const credentials = [
                { id: identityCredentialId, name: "Identity" },
                { id: bankCredentialId, name: "Bank Account" },
                { id: creditCredentialId, name: "Credit Score" }
            ];

            for (const cred of credentials) {
                const isRevoked = await didRegistry.read.isCredentialRevoked([cred.id]);
                assert.equal(isRevoked, false, `${cred.name} credential should be active`);

                const record = await didRegistry.read.getCredentialRevocation([cred.id]);
                assert.equal(record.revoked, false);
                assert(record.credentialHash !== "0x0000000000000000000000000000000000000000000000000000000000000000");
            }
            console.log("✅ All credentials verified as active");

            // 7. Verify customer's complete profile
            const customerInfo = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(getAddress(customerInfo[0]), getAddress(identity1.account.address)); // owner
            assert.equal(customerInfo[2], true); // KYC verified
            assert.equal(customerInfo[5], 3n); // 3 credentials issued

            const customerCredentials = await didRegistry.read.getIdentityCredentials([identity1.account.address]);
            assert.equal(customerCredentials.length, 3);
            console.log("✅ Customer profile verified");

            // 8. Simulate credential verification by third party
            for (const cred of credentials) {
                const credentialRecord = await didRegistry.read.getCredentialRevocation([cred.id]);

                // Verify credential integrity
                assert.equal(credentialRecord.revoked, false);
                assert(credentialRecord.timestamp > 0n);
                assert.equal(getAddress(credentialRecord.revoker), getAddress(issuer.account.address));
            }
            console.log("✅ Third-party verification completed");

            // 9. Test credential revocation scenario (e.g., account closure)
            await didRegistry.write.revokeCredential([
                bankCredentialId,
                identity1.account.address,
                "Account closed by customer request"
            ], {
                account: issuer.account
            });
            console.log("✅ Bank account credential revoked");

            // 10. Verify revocation status
            const bankRevoked = await didRegistry.read.isCredentialRevoked([bankCredentialId]);
            assert.equal(bankRevoked, true);

            const identityRevoked = await didRegistry.read.isCredentialRevoked([identityCredentialId]);
            const creditRevoked = await didRegistry.read.isCredentialRevoked([creditCredentialId]);
            assert.equal(identityRevoked, false); // Should remain active
            assert.equal(creditRevoked, false); // Should remain active
            console.log("✅ Selective revocation verified");

            // 11. Final system metrics verification
            const metrics = await didRegistry.read.getSystemMetrics();
            assert.equal(metrics[0], 1n); // totalDIDs
            assert.equal(metrics[1], 1n); // totalVerifiedDIDs
            assert.equal(metrics[2], 3n); // totalCredentials
            assert.equal(metrics[3], 1n); // totalRevokedCredentials
            console.log("✅ Final metrics verified");

            console.log("🎉 Complete banking onboarding with verifiable credentials flow executed successfully!");
        });

        it("Should handle complex multi-party verification scenario", async function () {
            // Setup multiple parties
            const university = issuer;
            const employer = auditor;
            const government = registrar;

            // Student creates DID
            await didRegistry.write.createDID([
                identity1.account.address,
                `{"id": "did:idbra:${identity1.account.address}", "type": "student"}`
            ], {
                account: identity1.account
            });

            // Government verifies identity
            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: government.account
            });

            // University issues degree credential
            const degreeData = {
                type: "UniversityDegree",
                degree: "Bachelor of Engineering",
                university: "Federal University of Technology",
                graduationDate: "2024-07-01",
                honors: "magna cum laude"
            };
            const degreeHash = keccak256(stringToHex(JSON.stringify(degreeData)));
            const degreeCredentialId = keccak256(encodePacked(
                ["bytes32", "string"],
                [degreeHash, "university-degree"]
            ));

            await didRegistry.write.issueCredential([
                degreeCredentialId,
                identity1.account.address,
                degreeHash
            ], {
                account: university.account
            });

            // Employer issues work experience credential
            const workData = {
                type: "WorkExperience",
                position: "Software Engineer",
                company: "TechCorp Brasil",
                startDate: "2024-08-01",
                skills: ["Blockchain", "Smart Contracts", "Solidity"]
            };
            const workHash = keccak256(stringToHex(JSON.stringify(workData)));
            const workCredentialId = keccak256(encodePacked(
                ["bytes32", "string"],
                [workHash, "work-experience"]
            ));

            // Grant ISSUER_ROLE to employer for this test
            await didRegistry.write.grantRole([ISSUER_ROLE, employer.account.address], {
                account: admin.account
            });

            await didRegistry.write.issueCredential([
                workCredentialId,
                identity1.account.address,
                workHash
            ], {
                account: employer.account
            });

            // Verify all credentials are valid
            const degreeRevoked = await didRegistry.read.isCredentialRevoked([degreeCredentialId]);
            const workRevoked = await didRegistry.read.isCredentialRevoked([workCredentialId]);
            assert.equal(degreeRevoked, false);
            assert.equal(workRevoked, false);

            // Verify complete credential set
            const credentials = await didRegistry.read.getIdentityCredentials([identity1.account.address]);
            assert.equal(credentials.length, 2);

            const info = await didRegistry.read.getIdentityInfo([identity1.account.address]);
            assert.equal(info[5], 2n); // credentialCount
        });
    });

    describe("7. Security and Access Control", function () {
        beforeEach(async function () {
            await didRegistry.write.createDID([
                identity1.account.address,
                `{"id": "did:idbra:${identity1.account.address}"}`
            ], {
                account: identity1.account
            });
        });

        it("Should enforce ISSUER_ROLE for credential operations", async function () {
            const credentialHash = keccak256(stringToHex("test"));
            const credentialId = keccak256(stringToHex("test-credential"));

            // Should fail without ISSUER_ROLE
            await assert.rejects(
                didRegistry.write.issueCredential([
                    credentialId,
                    identity1.account.address,
                    credentialHash
                ], {
                    account: nonAuthorized.account
                })
            );

            // Should succeed with ISSUER_ROLE
            await didRegistry.write.issueCredential([
                credentialId,
                identity1.account.address,
                credentialHash
            ], {
                account: issuer.account
            });
        });

        it("Should enforce REGISTRAR_ROLE for KYC operations", async function () {
            // Should fail without REGISTRAR_ROLE
            await assert.rejects(
                didRegistry.write.setKYCStatus([
                    identity1.account.address,
                    true
                ], {
                    account: nonAuthorized.account
                })
            );

            // Should succeed with REGISTRAR_ROLE
            await didRegistry.write.setKYCStatus([
                identity1.account.address,
                true
            ], {
                account: registrar.account
            });
        });

        it("Should enforce emergency pause functionality", async function () {
            // Emergency pause
            await didRegistry.write.pause([], {
                account: admin.account
            });

            // Operations should fail when paused
            await assert.rejects(
                didRegistry.write.createDID([
                    identity2.account.address,
                    `{"id": "did:idbra:${identity2.account.address}"}`
                ], {
                    account: identity2.account
                })
            );

            // Unpause
            await didRegistry.write.unpause([], {
                account: admin.account
            });

            // Operations should work again
            await didRegistry.write.createDID([
                identity2.account.address,
                `{"id": "did:idbra:${identity2.account.address}"}`
            ], {
                account: identity2.account
            });
        });
    });
});
