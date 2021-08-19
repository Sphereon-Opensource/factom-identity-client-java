package com.sphereon.factom.identity.did;

import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.FactomIdentityEntry;
import com.sphereon.factom.identity.did.entry.ReplaceKeyIdentityChainEntry;
import com.sphereon.factom.identity.did.mapper.JwkMapper;
import com.sphereon.factom.identity.did.parse.RuleException;
import com.sphereon.factom.identity.did.response.BlockchainResponse;
import com.sphereon.factom.identity.did.response.DidResponse;
import com.sphereon.factom.identity.did.response.IdentityResponse;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.DIDURL;
import foundation.identity.did.VerificationMethod;
import io.ipfs.multibase.Multibase;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.api.log.LogFactory;
import org.blockchain_innovation.factom.client.api.log.Logger;
import org.factomprotocol.identity.did.model.DidKey;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.IdentityEntry;
import org.factomprotocol.identity.did.model.KeyPurpose;
import org.factomprotocol.identity.did.model.KeyType;
import org.factomprotocol.identity.did.model.Metadata;
import org.factomprotocol.identity.did.model.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.sphereon.factom.identity.did.Constants.DID.DID_FACTOM;

public class IdentityFactory {
    private static final IdAddressKeyOps ADDRESSES = new IdAddressKeyOps();
    private static final Logger logger = LogFactory.getLogger(IdentityFactory.class);

    public BlockchainResponse<?> toBlockchainResponse(String identifier, List<FactomIdentityEntry<?>> entries) throws RuleException {
        if (entries == null || entries.size() == 0) {
            throw new RuleException("Identity for %s could not be resolved", identifier);
        }
        if (entries.get(0).getOperationValue().equals(OperationValue.IDENTITY_CHAIN_CREATION)) {
            return toIdentity(identifier, entries);
        } else if (entries.get(0).getOperationValue().equals(OperationValue.DID_MANAGEMENT)) {
            return toDidResponse(identifier, entries);
        }
        throw new RuleException("DID Chain for %s did not start with a valid external id.", identifier);
    }


    public DIDDocument toDid(String identifier, BlockchainResponse<?> blockchainResponse) {
        if (DIDVersion.FACTOM_V1_JSON.equals(blockchainResponse.getDidVersion())) {
            return toDid(identifier, (DidResponse) blockchainResponse);
        }
        if (DIDVersion.FACTOM_IDENTITY_CHAIN.equals(blockchainResponse.getDidVersion())) {
            return toDid(identifier, ((IdentityResponse) blockchainResponse).getIdentity());
        }
        throw new DIDRuntimeException("Invalid BlockchainResponse");
    }




    public DIDDocument toDid(String identifier, IdentityEntry identityEntry) {
        final String did = did(identifier);
        final DIDURL didurl = DIDVersion.FACTOM_IDENTITY_CHAIN.getDidUrl(did);
        final List<VerificationMethod> verificationMethods = new ArrayList<>();
        final List<VerificationMethod> authentications = new ArrayList<>();
        final List<VerificationMethod> assertionMethods = new ArrayList<>();
        final List<VerificationMethod> capabilityInvocationMethods = new ArrayList<>();
        final List<VerificationMethod> keyAgreementMethods = new ArrayList<>();
        final List<String> idPubs = identityEntry.getKeys();


        for (int i = 0; i < idPubs.size(); i++) {
            final String idPub = idPubs.get(i);
            final String controller = didurl.getDid().getDidString();
            final String id = String.format("%s#key-%d", controller, i);
            final byte[] keyBytes = ADDRESSES.toEd25519PublicKey(idPub).getAbyte();
//            final String b58Key = Encoding.BASE58.encode(keyBytes);
            final String mbKey = Multibase.encode(Multibase.Base.Base58BTC, keyBytes);

            VerificationMethod verificationMethod = VerificationMethod.builder()
                    .id(URI.create(id))
                    .type("Ed25519VerificationKey2020")
//                    .publicKeyBase58(b58Key)
                    .publicKeyMultibase(mbKey)
                    .build();

            verificationMethods.add(verificationMethod);
            if (idPubs.size() == 1) {
                capabilityInvocationMethods.add(verificationMethod);
                keyAgreementMethods.add(verificationMethod);
                authentications.add(verificationMethod);
                assertionMethods.add(verificationMethod);
            } else {
                if (i < idPubs.size()) {
                    capabilityInvocationMethods.add(verificationMethod);
                }
                if (i > 0) {
                    keyAgreementMethods.add(verificationMethod);
                    authentications.add(verificationMethod);
                    assertionMethods.add(verificationMethod);
                }
            }

        }

//        final DIDDocument.Builder<? extends DIDDocument.Builder<?>> builder = DIDDocument.builder().id(URI.create(did));

        // We build using the LdObjects ourselves as the convenience method does not do everything and objects are not mutable anymore
        final DIDDocument didDocument = DIDDocument.builder()
                .id(constructURI(didurl.getDid().getDidString()))
                .verificationMethods(verificationMethods)
                .assertionMethodVerificationMethods(assertionMethods)
                .authenticationVerificationMethods(authentications)
                .capabilityInvocationVerificationMethods(capabilityInvocationMethods)
                .keyAgreementVerificationMethods(keyAgreementMethods)
                .build();
//        didDocument.setJsonObjectKeyValue(DIDKeywords.JSONLD_TERM_AUTHENTICATION, authentications);
//        didDocument.setJsonObjectKeyValue(DIDKeywords.JSONLD_TERM_PUBLICKEY, publicKeys);
//        didDocument.setJsonObjectKeyValue(DIDConstants.JSONLD_TERM_ASSERTION_METHOD, assertionMethods);

        return didDocument;
    }

    public DIDDocument toDid(String identifier, DidResponse didResponse) {
        return toDid(identifier, didResponse.getFactomDidContent());
    }

    public DIDDocument toDid(String identifier, FactomDidContent didContent) {
        final String did = did(identifier);
        final DIDURL didurl = DIDVersion.FACTOM_IDENTITY_CHAIN.getDidUrl(did);
        final List<VerificationMethod> verificationMethods = new ArrayList<>();
        final List<VerificationMethod> authentications = new ArrayList<>();
        final List<VerificationMethod> assertionMethods = new ArrayList<>();
        final List<VerificationMethod> capabilityDelegationMethods = new ArrayList<>();
        final List<VerificationMethod> capabilityInvocationMethods = new ArrayList<>();
        final List<VerificationMethod> keyAgreementMethods = new ArrayList<>();
        final List<DidKey> didKeys = Optional.ofNullable(didContent.getDidKey()).orElse(new ArrayList<>());
        final List<foundation.identity.did.Service> services = new ArrayList<>();

        for (DidKey key : didKeys) {
            final URI keyId = constructURI(key.getId());
            final String mbKey = getMultibaseKey(key);

            VerificationMethod verificationMethod = VerificationMethod.builder()
                    .id(keyId)
                    .type(key.getType() == KeyType.ED25519VERIFICATIONKEY ?
                            KeyType.ED25519VERIFICATIONKEY2018.getValue() : key.getType().getValue())
                    .publicKeyBase58(key.getPublicKeyBase58())
                    .publicKeyHex(key.getPublicKeyHex())
                    .publicKeyBase64(key.getPublicKeyBase64())
                    .publicKeyPem(key.getPublicKeyPem())
                    .publicKeyMultibase(mbKey)
                    .publicKeyJwk(key.getPublicKeyJwk() == null ? null : new JwkMapper().toMap(key.getPublicKeyJwk()))
                    .build();

            if (key.getPurpose().contains(KeyPurpose.VERIFICATIONMETHOD) || key.getPurpose().contains(KeyPurpose.PUBLICKEY)) {
                // Public key is deprecated and has been replaced by verification method in the DID spec
                verificationMethods.add(verificationMethod);
            }

            addVerificationRelations(key.getPurpose(), KeyPurpose.AUTHENTICATION, verificationMethods, authentications, verificationMethod);
            addVerificationRelations(key.getPurpose(), KeyPurpose.ASSERTIONMETHOD, verificationMethods, assertionMethods, verificationMethod);
            addVerificationRelations(key.getPurpose(), KeyPurpose.CAPABILITYDELEGATION, verificationMethods, capabilityDelegationMethods, verificationMethod);
            addVerificationRelations(key.getPurpose(), KeyPurpose.CAPABILITYINVOCATION, verificationMethods, capabilityInvocationMethods, verificationMethod);
            addVerificationRelations(key.getPurpose(), KeyPurpose.KEYAGREEMENT, verificationMethods, keyAgreementMethods, verificationMethod);


        }

        if (didContent.getService() != null) {
            for (Service service : didContent.getService()) {
                final foundation.identity.did.Service didService = foundation.identity.did.Service.builder()
                        .id(constructURI(service.getId()))
                        .serviceEndpoint(service.getServiceEndpoint())
                        .type(service.getType())
                        .build();
                services.add(didService);

            }

        }

        final DIDDocument didDocument = DIDDocument.builder()
                .id(constructURI(didurl.getDid().getDidString()))
                .verificationMethods(verificationMethods)
                .assertionMethodVerificationMethods(assertionMethods)
                .authenticationVerificationMethods(authentications)
                .capabilityDelegationVerificationMethods(capabilityDelegationMethods)
                .capabilityInvocationVerificationMethods(capabilityInvocationMethods)
                .keyAgreementVerificationMethods(keyAgreementMethods)
                .services(services)
                .build();

        // TODO: 16/08/2021 We need to put a reference if the methods are re-used outside of the verificationMethod, like we did below!
//        didDocument.setJsonObjectKeyValue(DIDConstants.JSONLD_TERM_ASSERTION_METHOD, assertionMethods);
//        didDocument.setJsonObjectKeyValue(DIDKeywords.JSONLD_TERM_AUTHENTICATION, authentications);
        return didDocument;
    }

    public DidResponse toDidResponse(String identifier, List<FactomIdentityEntry<?>> entries) throws RuleException {
        if (entries == null || entries.size() == 0) {
            throw new RuleException("Identity for %s could not be resolved", identifier);
        }
        Metadata metadata = new Metadata();
        FactomDidContent factomDidContent = null;
        for (FactomIdentityEntry<?> entry : entries) {
            // handle first entry
            if (factomDidContent == null) {
                if (entry.getOperationValue() != OperationValue.DID_MANAGEMENT) {
                    throw new RuleException("Identity chain %s did not start with a Create Factom DID entry", identifier);
                }
                factomDidContent = ((CreateFactomDIDEntry) entry).getContent();
                metadata.creation(entry.getBlockInfo().get());
                metadata.update(entry.getBlockInfo().get());
            }
            //ToDo: handle update/deactivate entries
        }
        if (factomDidContent == null) {
            throw new RuleException("DID chain %s did not start with an Create Factom DID entry", identifier);
        }
        return new DidResponse()
                .factomDidContent(factomDidContent)
                .metadata(metadata);
    }

    public IdentityResponse toIdentity(String identifier, List<FactomIdentityEntry<?>> entries) throws RuleException {
        if (entries == null || entries.size() == 0) {
            throw new RuleException("Identity for %s could not be resolved", identifier);
        }
        Metadata metadata = new Metadata();
        IdentityEntry identityEntry = null;
        for (FactomIdentityEntry<?> entry : entries) {
            if (identityEntry == null) {
                if (entry.getOperationValue() != OperationValue.IDENTITY_CHAIN_CREATION) {
                    throw new RuleException("Identity chain %s did not start with an Identity Creation entry", identifier);
                }
                try {
                    identityEntry = (IdentityEntry) entry.getContent();
                } catch (ClassCastException cce) {
                    throw new RuleException("Identity chain %s did not start with an Identity Creation entry", identifier);
                }
                if (entry.getBlockInfo().isPresent()) {
                    metadata.creation(entry.getBlockInfo().get());
                    metadata.update(entry.getBlockInfo().get());
                }
                continue;
            } else if (entry.getOperationValue() != OperationValue.IDENTITY_CHAIN_REPLACE_KEY) {
                continue;
            }
            ReplaceKeyIdentityChainEntry replaceKeyEntry = (ReplaceKeyIdentityChainEntry) entry;
            if (!ADDRESSES.verifyKeyReplacementSignature(replaceKeyEntry)) {
                continue;
            }
            try {
                List<String> newKeys = ADDRESSES.createNewKeyReplacementList(identityEntry.getKeys(), replaceKeyEntry.getOldKey(), replaceKeyEntry.getNewKey(), replaceKeyEntry.getSignerKey());
                identityEntry.setKeys(newKeys);
                metadata.update(replaceKeyEntry.getBlockInfo().get());
            } catch (FactomRuntimeException e) {
                logger.warn("Identity chain replacement entry could not be processed for chain: " + replaceKeyEntry.getChainId(), e);
            }
        }
        if (identityEntry == null) {
            throw new RuleException("Identity chain %s did not start with an Identity Creation entry", identifier);
        }
        return new IdentityResponse()
                .identity(identityEntry)
                .metadata(metadata);
    }


    private String getMultibaseKey(DidKey key) {
        String mbKey = null;
        if (key.getPublicKeyMultibase() != null) {
            // We decode to ensure it is in proper format
            Multibase.decode(key.getPublicKeyMultibase());
            mbKey = key.getPublicKeyMultibase();
        }
        return mbKey;
    }

    private URI constructURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new FactomRuntimeException.AssertionException(e);
        }
    }

    private void addVerificationRelations(List<KeyPurpose> keyPurposes, KeyPurpose applicableKeyPurpose, List<VerificationMethod> allVerificationMethods, List<VerificationMethod> specificVerificationMethods, VerificationMethod currentVerificationMethod) {
        if (keyPurposes.contains(applicableKeyPurpose)) {
            specificVerificationMethods.add(currentVerificationMethod);
        }
    }
    

    private String did(String identifier) {
        String did = identifier;
        if (!identifier.startsWith("did:")) {
            did = DID_FACTOM + identifier;
        }
        return did;
    }
}
