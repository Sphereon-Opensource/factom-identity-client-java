package org.blockchain_innovation.factom.identiy.did;

import foundation.identity.did.DIDDocument;
import foundation.identity.did.DIDURL;
import foundation.identity.did.PublicKey;
import foundation.identity.did.jsonld.DIDKeywords;
import foundation.identity.did.parser.ParserException;
import foundation.identity.jsonld.JsonLDObject;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.api.ops.Encoding;
import org.blockchain_innovation.factom.identiy.did.entry.CreateIdentityContentEntry;
import org.blockchain_innovation.factom.identiy.did.entry.FactomIdentityEntry;
import org.blockchain_innovation.factom.identiy.did.entry.ReplaceKeyIdentityChainEntry;
import org.blockchain_innovation.factom.identiy.did.parse.RuleException;
import org.factomprotocol.identity.did.model.IdentityEntry;
import org.factomprotocol.identity.did.model.IdentityResponse;
import org.factomprotocol.identity.did.model.Metadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IdentityFactory {
    private static final IdAddressKeyOps ADDRESSES = new IdAddressKeyOps();

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
            } catch (FactomRuntimeException re) {

            }
        }
        if (identityEntry == null) {
            throw new RuleException("Identity chain %s did not start with an Identity Creation entry", identifier);
        }
        IdentityResponse identityResponse = new IdentityResponse();
        identityResponse.setIdentity(identityEntry);
        identityResponse.setMetadata(metadata);
        return identityResponse;
    }

    public DIDDocument toDid(String identifier, IdentityResponse identityResponse) throws RuleException, ParserException, URISyntaxException {
        String did = identifier;
        if (!identifier.startsWith("did:")) {
            did = "did:factom:" + identifier;
        }
        DIDURL didurl = DIDURL.fromString(did);
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
}
