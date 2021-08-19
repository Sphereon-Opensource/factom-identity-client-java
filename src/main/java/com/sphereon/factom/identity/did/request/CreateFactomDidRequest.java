package com.sphereon.factom.identity.did.request;

import com.sphereon.factom.identity.did.DIDVersion;
import com.sphereon.factom.identity.did.IdentityFactory;
import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import org.blockchain_innovation.factom.client.impl.Networks;
import org.factomprotocol.identity.did.model.DidMethodVersion;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.sphereon.factom.identity.did.Constants.DID.DID_FACTOM;

public class CreateFactomDidRequest {
    private final DIDVersion didVersion;
    private final List<CreateKeyRequest> managementKeys;
    private final List<CreateKeyRequest> didKeys;
    private final List<CreateServiceRequest> services;
    private final String[] tags;
    private final String nonce;
    private final String networkName;

    private CreateFactomDidRequest(DIDVersion didVersion, String networkName, List<CreateKeyRequest> managementKeys, List<CreateKeyRequest> didKey, List<CreateServiceRequest> service, String nonce, String... tags) {
        this.didVersion = didVersion;
        this.networkName = networkName;
        this.managementKeys = managementKeys;
        this.didKeys = didKey;
        this.services = service;
        this.nonce = nonce;
        this.tags = tags;
    }

    public String getNonce() {
        return this.nonce;
    }

    public String[] getTags() {
        return this.tags;
    }

    public DIDVersion getDidVersion() {
        return this.didVersion;
    }

    public FactomDidContent toFactomDidContent() {
        final CreateFactomDIDEntry createFactomDIDEntry = toCreateFactomDIDEntry();
        String chainId = createFactomDIDEntry.getChainId();
        String did = getDidURL(chainId);
        return new FactomDidContent()
                .didMethodVersion(DidMethodVersion.fromValue(didVersion.getSchemaVersion()))
                .didKey(this.didKeys.stream().map(key -> key.toDidKey(did))
                        .collect(Collectors.toList()))
                .managementKey(this.managementKeys.stream().map(key -> key.toManagementKey(did))
                        .collect(Collectors.toList()))
                .service(this.services.stream().map(didService -> didService.toService(did))
                        .collect(Collectors.toList()));
    }

    @NotNull
    private CreateFactomDIDEntry toCreateFactomDIDEntry() {
        return new CreateFactomDIDEntry(this.didVersion, null, this.nonce, this.tags);
    }

    public DIDDocument toDIDDocument() {
        return new IdentityFactory().toDid(toCreateFactomDIDEntry().getChainId(), toFactomDidContent());
    }

    private String getDidURL(String chainId) {
        if (networkName == null || Networks.MAINNET.equalsIgnoreCase(networkName)) {
            return DID_FACTOM + chainId;
        }
        return DID_FACTOM + networkName + ':' + chainId;
    }

    public static final class Builder {
        private DIDVersion didVersion;
        private List<CreateKeyRequest> managementKeys;
        private List<CreateKeyRequest> didKeys;
        private List<CreateServiceRequest> services;
        private List<String> tags;
        private String nonce;
        private String networkName;

        public Builder() {
        }

        public Builder didVersion(DIDVersion didVersion) {
            this.didVersion = didVersion;
            return this;
        }

        public Builder managementKeys(List<CreateKeyRequest> managementKeys) {
            this.managementKeys = managementKeys;
            return this;
        }

        public Builder managementKey(CreateKeyRequest managementKey) {
            if (this.managementKeys == null) {
                this.managementKeys = new ArrayList<>(Arrays.asList(managementKey));
            } else if (!managementKeys.contains(managementKey)){
                this.managementKeys.add(managementKey);
            }
            return this;
        }

        public Builder didKeys(List<CreateKeyRequest> didKeys) {
            this.didKeys = didKeys;
            return this;
        }

        public Builder didKey(CreateKeyRequest didKey) {
            if (this.didKeys == null) {
                this.didKeys = new ArrayList<>(Arrays.asList(didKey));
            } else if (!didKeys.contains(didKey)) {
                this.didKeys.add(didKey);
            }
            return this;
        }

        public Builder services(List<CreateServiceRequest> services) {
            this.services = services;
            return this;
        }

        public Builder service(CreateServiceRequest service) {
            if (this.services == null) {
                this.services = new ArrayList<>(Arrays.asList(service));
            } else if (!services.contains(service)) {
                this.services.add(service);
            }
            return this;
        }

        public Builder tag(String tag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>(Arrays.asList(tag));
            } else {
                this.tags.add(tag);
            }
            return this;
        }

        public Builder nonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder networkName(String networkName) {
            this.networkName = networkName;
            return this;
        }

        public CreateFactomDidRequest build() throws IncompleteRequestException {
            if (this.services == null) {
                this.services = Collections.emptyList();
            }
            this.assertComplete();
            return new CreateFactomDidRequest(
                    didVersion,
                    networkName,
                    managementKeys,
                    didKeys,
                    services,
                    nonce,
                    tags.toArray(new String[0])
            );
        }

        private void assertComplete() throws IncompleteRequestException {
            if (this.didKeys == null || this.didKeys.size() == 0) {
                throw new IncompleteRequestException("At least one DID key is required to create a new DID");
            }
            if (this.managementKeys == null || this.managementKeys.size() == 0) {
                throw new IncompleteRequestException("At least one management key is required to create a new DID");
            }
        }
    }
}
