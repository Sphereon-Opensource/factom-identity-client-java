package org.blockchain_innovation.factom.identiy.did.request;

import org.factomprotocol.identity.did.model.DidKey;
import org.factomprotocol.identity.did.model.KeyPurpose;
import org.factomprotocol.identity.did.model.KeyType;
import org.factomprotocol.identity.did.model.ManagementKey;

import java.util.List;

public class CreateKeyRequest {
    private final KeyType type;
    private final String keyIdentifier;
    private final String publicKeyBase58;
    private final int priority;
    private final int priorityRequirement;
    private final String bip44;
    private final List<KeyPurpose> purpose;

    private CreateKeyRequest(KeyType type,
                             String keyIdentifier,
                             String publicKeyBase58,
                             int priority,
                             List<KeyPurpose> purpose,
                             int priorityRequirement,
                             String bip44) {
        this.type = type;
        this.keyIdentifier = keyIdentifier;
        this.publicKeyBase58 = publicKeyBase58;
        this.priority = priority;
        this.priorityRequirement = priorityRequirement;
        this.purpose = purpose;
        this.bip44 = bip44;
    }

    public DidKey toDidKey(String did) {
        return new DidKey()
                .id(did + '#' + keyIdentifier)
                .type(type)
                .publicKeyBase58(publicKeyBase58)
                .priorityRequirement(priorityRequirement)
                .purpose(purpose)
                .bip44(bip44)
                .controller(did);
    }

    public ManagementKey toManagementKey(String did) {
        return new ManagementKey()
                .controller(did)
                .id(did + '#' + keyIdentifier)
                .publicKeyBase58(publicKeyBase58)
                .type(type)
                .bip44(bip44)
                .priority(priority)
                .priorityRequirement(priorityRequirement);
    }

    public static final class Builder {
        private KeyType type;
        private String keyIdentifier;
        private String publicKeyBase58;
        private int priority;
        private int priorityRequirement;
        private List<KeyPurpose> purpose;
        private String bip44;

        public Builder() {
        }

        public Builder type(KeyType type) {
            this.type = type;
            return this;
        }

        public Builder keyIdentifier(String keyIdentifier) {
            this.keyIdentifier = keyIdentifier;
            return this;
        }

        public Builder publicKeyBase58(String publicKeyBase58) {
            this.publicKeyBase58 = publicKeyBase58;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder priorityRequirement(int priorityRequirement) {
            this.priorityRequirement = priorityRequirement;
            return this;
        }

        public Builder purpose(List<KeyPurpose> purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder bip44(String bip44) {
            this.bip44 = bip44;
            return this;
        }

        public CreateKeyRequest build() {
            return new CreateKeyRequest(type, keyIdentifier, publicKeyBase58, priority, purpose, priorityRequirement, bip44);
        }
    }
}
