package com.sphereon.factom.identity.did.json;

import com.sphereon.factom.identity.did.DIDVersion;
import org.blockchain_innovation.factom.client.api.json.JsonConverter;
import org.factomprotocol.identity.did.model.DidMethodVersion;
import org.factomprotocol.identity.did.model.KeyPurpose;
import org.factomprotocol.identity.did.model.KeyType;
import org.factomprotocol.identity.did.model.Service;

public class RegisterJsonMappings {
    public static JsonConverter register(JsonConverter converter) {

        if(converter.getName().equalsIgnoreCase("GSON")){
            // TODO: Add support for Service class serialization in non-GSON converters.
            converter.addAdapter(Service.class, new CustomServiceSerializer());
        }

        if (!converter.getName().equalsIgnoreCase("JEE")) {
            // Non JEE converters don't need this specialized handling
            return converter;
        }

        converter.addAdapter(KeyType.class, new KeyTypeAdapter(KeyType.class));
        converter.addAdapter(KeyPurpose.class, new KeyPurposeAdapter(KeyPurpose.class));
        converter.addAdapter(DidMethodVersion.class, new DidMethodVersionAdapter(DidMethodVersion.class));
        converter.addAdapter(DIDVersion.class, new DIDVersionAdapter(DIDVersion.class));
        return converter;
    }

    // We need this to not use runtime info
    protected static class KeyTypeAdapter extends ValueEnumMapper<KeyType> {
        public KeyTypeAdapter(Class<KeyType> keyTypeClass) {
            super(keyTypeClass);
        }
    }

    protected static class KeyPurposeAdapter extends ValueEnumMapper<KeyPurpose> {
        public KeyPurposeAdapter(Class<KeyPurpose> keyTypeClass) {
            super(keyTypeClass);
        }
    }

    protected static class DIDVersionAdapter extends ValueEnumMapper<DIDVersion> {
        public DIDVersionAdapter(Class<DIDVersion> didVersionClass) {
            super(didVersionClass);
        }
    }

    protected static class DidMethodVersionAdapter extends ValueEnumMapper<DidMethodVersion> {
        public DidMethodVersionAdapter(Class<DidMethodVersion> methodVersionClass) {
            super(methodVersionClass);
        }
    }
}
