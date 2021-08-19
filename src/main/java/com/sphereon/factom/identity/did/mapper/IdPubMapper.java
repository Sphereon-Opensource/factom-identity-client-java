package com.sphereon.factom.identity.did.mapper;

import org.blockchain_innovation.factom.client.api.AddressKeyConversions;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.api.model.types.AddressType;
import org.blockchain_innovation.factom.client.api.ops.Encoding;
import org.factomprotocol.identity.did.model.DidKey;
import org.factomprotocol.identity.did.model.FactomKey;
import org.factomprotocol.identity.did.model.KeyType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IdPubMapper {

    public static final AddressKeyConversions ADDRESS_KEY_CONVERSIONS = new AddressKeyConversions();
    public static final String MULTIBASE_ED25519_PREFIX = "z";

    public static String toIdPub(FactomKey factomKey) {
        if (factomKey == null) {
            return null;
        }
        switch (factomKey.getType()) {
            case ED25519VERIFICATIONKEY:
            case ED25519VERIFICATIONKEY2018:
            case ED25519VERIFICATIONKEY2020:
                final String keyValue = factomKey.getPublicValue().startsWith(MULTIBASE_ED25519_PREFIX) ? factomKey.getPublicValue().substring(1, factomKey.getPublicValue().length()) : factomKey.getPublicValue();
                return ADDRESS_KEY_CONVERSIONS.keyToAddress(keyValue, AddressType.IDENTITY_IDPUB, Encoding.BASE58);
            case IDPUB:
                return factomKey.getPublicValue();
            default:
                throw new FactomRuntimeException.AssertionException("Only ED25519 keys or idpubs supported at this time");
        }
    }

    public static List<String> toIdPub(List<FactomKey> keys) {
        if (keys == null || keys.size() == 0) {
            return Collections.EMPTY_LIST;
        }
        return keys.stream().sequential().map(IdPubMapper::toIdPub).collect(Collectors.toList());
    }

    /*public static DidKey toDidKey(FactomKey factomKey, List<KeyPurpose> purposes) {

        String b58;
        switch (factomKey.getType()) {
            case ED25519VERIFICATIONKEY:
            case ED25519VERIFICATIONKEY2018:
                b58 = MULTIBASE_ED25519_PREFIX + factomKey.getPublicValue();
                break;
            case ED25519VERIFICATIONKEY2020:
                b58 = factomKey.getPublicValue();
                break;
            case IDPUB:
                b58 = Encoding.BASE58.encode(ADDRESS_KEY_CONVERSIONS.addressToPublicKey(factomKey.getPublicValue()));
                break;
            default:
                throw new FactomRuntimeException.AssertionException("Only ED25519 and idpub keys Factom identity keys are supported");
        }
        return new DidKey().type(KeyType.ED25519VERIFICATIONKEY2020).publicKeyMultibase(b58).purpose(purposes).;
    }
*/
    public static DidKey toDidKey(String idPub) {
        final String b58 = Encoding.BASE58.encode(ADDRESS_KEY_CONVERSIONS.addressToPublicKey(idPub));
        return new DidKey().publicKeyMultibase(MULTIBASE_ED25519_PREFIX + b58).type(KeyType.ED25519VERIFICATIONKEY2020);
    }

}
