package com.sphereon.factom.identity.did.request;

import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.api.ops.StringUtils;
import org.factomprotocol.identity.did.model.DidKey;
import org.factomprotocol.identity.did.model.JwkKey;
import org.factomprotocol.identity.did.model.KeyPurpose;
import org.factomprotocol.identity.did.model.KeyType;
import org.factomprotocol.identity.did.model.ManagementKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateKeyRequest {
    private final KeyType type;
    private final String keyIdentifier;
    private final String publicKeyMultibase;
    private final String publicKeyBase58;
    private final String publicKeyHex;
    private final JwkKey publicKeyJwk;
    private final int priority;
    private final int priorityRequirement;
    private final String bip44;
    private final List<KeyPurpose> purpose;

    private CreateKeyRequest(Builder builder) {
        this.type = builder.type;
        this.keyIdentifier = builder.keyIdentifier;
        this.publicKeyBase58 = builder.publicKeyBase58;
        this.publicKeyMultibase = builder.publicKeyMultibase;
        this.publicKeyHex = builder.publicKeyHex;
        this.publicKeyJwk = builder.publicKeyJwk;
        this.priority = builder.priority;
        this.priorityRequirement = builder.priorityRequirement;
        this.purpose = builder.purpose;
        this.bip44 = builder.bip44;
    }

    public DidKey toDidKey(String controller) {
        return new DidKey()
                .id(controller + '#' + keyIdentifier)
                .type(type)
                .publicKeyBase58(publicKeyBase58)
                .publicKeyMultibase(publicKeyMultibase)
                .publicKeyHex(publicKeyHex)
                .publicKeyJwk(publicKeyJwk)
                .priorityRequirement(priorityRequirement)
                .purpose(purpose)
                .bip44(bip44)
                .controller(controller);
    }

    public ManagementKey toManagementKey(String controller) {
        return new ManagementKey()
                .controller(controller)
                .id(controller + '#' + keyIdentifier)
                .publicKeyBase58(publicKeyBase58)
                .publicKeyMultibase(publicKeyMultibase)
                .publicKeyHex(publicKeyHex)
                .publicKeyJwk(publicKeyJwk)
                .type(type)
                .bip44(bip44)
                .priority(priority)
                .priorityRequirement(priorityRequirement);
    }

    public static final class Builder {
        private KeyType type;
        private String keyIdentifier;
        private String publicKeyBase58;
        private String publicKeyMultibase;
        private String publicKeyHex;
        private JwkKey publicKeyJwk;
        private int priority;
        private int priorityRequirement;
        private List<KeyPurpose> purpose;
        private String bip44;

        public Builder() {
        }

        public Builder type(final KeyType type) {
            this.type = type;
            return this;
        }

        public Builder keyIdentifier(final String keyIdentifier) {
            this.keyIdentifier = keyIdentifier;
            return this;
        }

        public Builder publicKeyBase58(final String publicKeyBase58) {
            assertZeroKeys();
            this.publicKeyBase58 = publicKeyBase58;
            return this;
        }

        public Builder publicKeyHex(final String publicKeyHex) {
            assertZeroKeys();
            this.publicKeyHex = publicKeyHex;
            return this;
        }

        public Builder publicKeyMultibase(final String publicKeyMultibase) {
            assertZeroKeys();
            this.publicKeyMultibase = publicKeyMultibase;
            return this;
        }

        public Builder publicKeyJwk(final JwkKey publicKeyJwk) {
            assertZeroKeys();
            this.publicKeyJwk = publicKeyJwk;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder priorityRequirement(int priorityRequirement) {
            if (priorityRequirement < 0) {
                throw new FactomRuntimeException.AssertionException("Priority requirement cannot be negative. Value: " + priorityRequirement);
            }
            this.priorityRequirement = priorityRequirement;
            return this;
        }

        public Builder purpose(KeyPurpose purpose) {
            if (purpose == null) {
                this.purpose = new ArrayList<>();
            }
            this.purpose.add(purpose);
            return this;
        }

        public Builder purposes(List<KeyPurpose> purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder bip44(String bip44) {
            this.bip44 = bip44;
            return this;
        }

        public CreateKeyRequest build() {
            return new CreateKeyRequest(this);
        }

        private void assertZeroKeys() {
            final long count = countKeys(Arrays.asList(publicKeyBase58, publicKeyBase58, publicKeyMultibase), publicKeyJwk);
            if (count > 0) {
                throw new FactomRuntimeException.AssertionException("You can only have 1 key encoding at a time according to the DID spec, current count: " + count);
            }
        }

        private long countKeys(List<String> keys, JwkKey jwkKey) {
            long count = keys.stream().filter(s -> StringUtils.isNotEmpty(s)).count();
            if (jwkKey != null) {
                count++;
            }
            return count;
        }
    }
}
