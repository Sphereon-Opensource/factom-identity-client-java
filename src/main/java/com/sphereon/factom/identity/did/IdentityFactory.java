package com.sphereon.factom.identity.did;

import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityContentEntry;
import com.sphereon.factom.identity.did.entry.FactomIdentityEntry;
import com.sphereon.factom.identity.did.entry.ReplaceKeyIdentityChainEntry;
import com.sphereon.factom.identity.did.parse.RuleException;
import com.sphereon.factom.identity.did.response.BlockchainResponse;
import com.sphereon.factom.identity.did.response.DidResponse;
import com.sphereon.factom.identity.did.response.IdentityResponse;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.DIDURL;
import foundation.identity.did.PublicKey;
import foundation.identity.did.jsonld.DIDKeywords;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.api.ops.Encoding;
import org.factomprotocol.identity.did.model.DidKey;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.IdentityEntry;
import org.factomprotocol.identity.did.model.KeyPurpose;
import org.factomprotocol.identity.did.model.Metadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IdentityFactory {
    private static final IdAddressKeyOps ADDRESSES = new IdAddressKeyOps();

    public BlockchainResponse<?> toBlockchainResponse(String identifier, List<FactomIdentityEntry<?>> entries) throws RuleException {
        if (entries == null || entries.size() == 0) {
            throw new RuleException("Identity for %s could not be resolved", identifier);
        }
        if (entries.get(0).getOperationValue().equals(OperationValue.IDENTITY_CHAIN_CREATION)) {
            return toIdentity(identifier, entries);
        }
        else if (entries.get(0).getOperationValue().equals(OperationValue.DID_MANAGEMENT)) {
            return toDidResponse(identifier, entries);
        }
        throw new RuleException("DID Chain for %s did not start with a valid external id.", identifier);
    }


    public DIDDocument toDid(String identifier, BlockchainResponse<?> blockchainResponse) throws URISyntaxException {
        if(blockchainResponse.getContent() instanceof FactomDidContent){
            return toDid(identifier, (DidResponse) blockchainResponse);
        }
        if(blockchainResponse.getContent() instanceof IdentityEntry){
            return toDid(identifier, (IdentityResponse) blockchainResponse);
        }
        throw new DIDRuntimeException("Invalid BlockchainResponse");
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
                identityEntry = ((CreateIdentityContentEntry) entry).getContent();
                metadata.creation(entry.getBlockInfo().get());
                metadata.update(entry.getBlockInfo().get());
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
            } catch (FactomRuntimeException ignored) {
            }
        }
        if (identityEntry == null) {
            throw new RuleException("Identity chain %s did not start with an Identity Creation entry", identifier);
        }
        return new IdentityResponse()
                .identity(identityEntry)
                .metadata(metadata);
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

    public DIDDocument toDid(String identifier, IdentityResponse identityResponse) throws URISyntaxException {
        String did = identifier;
        if (!identifier.startsWith("did:")) {
            did = "did:factom:" + identifier;
        }
        DIDURL didurl = DIDVersion.FACTOM_IDENTITY_CHAIN.getDidUrl(did);
        List<Map<String, Object>> publicKeys = new LinkedList<>();
        List<String> authentications = new ArrayList<>();
        List<String> assertionMethods = new ArrayList<>();
        List<String> idPubs = identityResponse.getIdentity().getKeys();


        for (int i = 0; i < idPubs.size(); i++) {
            String idPub = idPubs.get(i);
            String controller = didurl.getDid().getDidString();
            String id = String.format("%s#key-%d", controller, i);
            byte[] keyBytes = ADDRESSES.toEd25519PublicKey(idPub).getAbyte();
            String hexKey = Encoding.HEX.encode(keyBytes);
            String b58Key = Encoding.BASE58.encode(keyBytes);
            Map<String, Object> keyAttrs = new HashMap<>();
            Map<String, Object> authAttrs = new HashMap<>();


            keyAttrs.put(DIDConstants.JSONLD_TERM_TYPE, "Ed25519VerificationKey2018");
            keyAttrs.put(DIDConstants.JSONLD_TERM_ID, id);
            keyAttrs.put(DIDKeywords.JSONLD_TERM_PUBLICKEYBASE58, b58Key);
            keyAttrs.put(DIDKeywords.JSONLD_TERM_PUBLICKEYHEX, hexKey);

            keyAttrs.put(DIDConstants.JSONLD_TERM_CONTROLLER, controller);
            authAttrs.put(DIDConstants.JSONLD_TERM_CONTROLLER, controller);
            if (i > 0 || idPubs.size() == 1) {
                authentications.add(id);
                assertionMethods.add(id);
            }
            publicKeys.add(PublicKey.fromJsonObject(keyAttrs).getJsonObject());
        }

        // We build using the LdObjects ourselves as the convenience method does not do everything and objects are not mutable anymore
        DIDDocument didDocument = DIDDocument.builder()
                .context(new URI("https://www.w3.org/ns/did/v1"))
                .id(new URI(didurl.getDid().getDidString()))
                .build();
        didDocument.setJsonObjectKeyValue(DIDKeywords.JSONLD_TERM_AUTHENTICATION, authentications);
        didDocument.setJsonObjectKeyValue(DIDKeywords.JSONLD_TERM_PUBLICKEY, publicKeys);
        didDocument.setJsonObjectKeyValue(DIDConstants.JSONLD_TERM_ASSERTION_METHOD, assertionMethods);

        return didDocument;
    }

    public DIDDocument toDid(String identifier, DidResponse didResponse) throws URISyntaxException {
        String did = identifier;
        if (!identifier.startsWith("did:")) {
            did = "did:factom:" + identifier;
        }
        DIDURL didurl = DIDVersion.FACTOM_V1_JSON.getDidUrl(did);
        List<DidKey> didKeys = Optional.ofNullable(didResponse.getFactomDidContent().getDidKey()).orElse(new ArrayList<>());
        List<String> authentications = new ArrayList<>();
        List<String> assertionMethods = new ArrayList<>();
        List<PublicKey> publicKeys = new ArrayList<>();
        for (DidKey key : didKeys) {
            URI keyId = new URI(key.getId());
            if (key.getPurpose().contains(KeyPurpose.AUTHENTICATION)) {
                authentications.add(keyId.toString());
            } else {
                // ToDo: figure out what to do with assertionMethods (probably need a new model constant for KeyPurpose)
                assertionMethods.add(keyId.toString());
            }
            PublicKey publicKey = PublicKey.builder()
                    .id(keyId)
                    .type(key.getType().getValue())
                    .publicKeyBase58(key.getPublicKeyBase58())
                    .build();
            publicKey.setJsonObjectKeyValue(DIDConstants.JSONLD_TERM_CONTROLLER, key.getController());
            publicKeys.add(publicKey);
        }
        DIDDocument didDocument = DIDDocument.builder()
                .context(new URI("https://www.w3.org/ns/did/v1"))
                .id(new URI(didurl.getDid().getDidString()))
                .publicKeys(publicKeys)
                .build();

        didDocument.setJsonObjectKeyValue(DIDConstants.JSONLD_TERM_ASSERTION_METHOD, assertionMethods);
        didDocument.setJsonObjectKeyValue(DIDKeywords.JSONLD_TERM_AUTHENTICATION, authentications);
        return didDocument;
    }
}
