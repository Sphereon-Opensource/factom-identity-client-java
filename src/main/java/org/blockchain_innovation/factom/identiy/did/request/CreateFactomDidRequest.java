package org.blockchain_innovation.factom.identiy.did.request;

import org.blockchain_innovation.factom.identiy.did.DIDVersion;
import org.blockchain_innovation.factom.identiy.did.entry.CreateFactomDIDEntry;
import org.factomprotocol.identity.did.model.DidMethodVersion;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CreateFactomDidRequest {
    private final DIDVersion didVersion;
    private final List<CreateKeyRequest> managementKey;
    private final List<CreateKeyRequest> didKey;
    private final List<CreateServiceRequest> service;
    private final String[] tags;
    private final String nonce;
    private final String networkName;

    private CreateFactomDidRequest(DIDVersion didVersion, String networkName, List<CreateKeyRequest> managementKey, List<CreateKeyRequest> didKey, List<CreateServiceRequest> service, String nonce, String... tags) {
        this.didVersion = didVersion;
        this.networkName = networkName;
        this.managementKey = managementKey;
        this.didKey = didKey;
        this.service = service;
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

    public FactomDidContent getFactomDidContent() {
        String chainId = new CreateFactomDIDEntry(this.didVersion, null, this.nonce, this.tags).getChainId();
        String did = this.networkName == null ? "did:factom:" + chainId : "did:factom:" + this.networkName + ':' + chainId;
        return new FactomDidContent()
                .didMethodVersion(DidMethodVersion.fromValue(didVersion.getSchemaVersion()))
                .didKey(this.didKey.stream().map(key -> key.toDidKey(did))
                        .collect(Collectors.toList()))
                .managementKey(this.managementKey.stream().map(key -> key.toManagementKey(did))
                        .collect(Collectors.toList()))
                .service(this.service.stream().map(didService -> new Service()
                        .id(did + '#' + didService.getServiceIdentifier())
                        .type(didService.getType())
                        .serviceEndpoint(didService.getServiceEndpoint().toString())
                ).collect(Collectors.toList()));
    }

    public static final class Builder {
        private DIDVersion didVersion;
        private List<CreateKeyRequest> managementKey;
        private List<CreateKeyRequest> didKey;
        private List<CreateServiceRequest> service;
        private List<String> tags;
        private String nonce;
        private String networkName;

        public Builder() {
        }

        public Builder didVersion(DIDVersion didVersion) {
            this.didVersion = didVersion;
            return this;
        }

        public Builder managementKey(List<CreateKeyRequest> managementKey) {
            this.managementKey = managementKey;
            return this;
        }

        public Builder didKey(List<CreateKeyRequest> didKey) {
            this.didKey = didKey;
            return this;
        }

        public Builder service(List<CreateServiceRequest> service) {
            this.service = service;
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

        public CreateFactomDidRequest build() {
            if (this.service == null) {
                this.service = Collections.emptyList();
            }
            return new CreateFactomDidRequest(didVersion, networkName, managementKey, didKey, service, nonce, tags.toArray(new String[0]));
        }
    }
}
