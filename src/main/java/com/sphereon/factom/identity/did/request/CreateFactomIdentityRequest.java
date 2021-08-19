package com.sphereon.factom.identity.did.request;

import com.sphereon.factom.identity.did.DIDVersion;
import com.sphereon.factom.identity.did.IdentityFactory;
import com.sphereon.factom.identity.did.entry.CreateIdentityContentEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityRequestEntry;
import com.sphereon.factom.identity.did.mapper.IdPubMapper;
import foundation.identity.did.DIDDocument;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.impl.Networks;
import org.factomprotocol.identity.did.model.CreateIdentityRequest;
import org.factomprotocol.identity.did.model.FactomKey;
import org.factomprotocol.identity.did.model.IdentityEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.sphereon.factom.identity.did.Constants.DID.DID_FACTOM;

public class CreateFactomIdentityRequest {
    private final DIDVersion didVersion;
    private final List<FactomKey> keys;
    private final List<String> tags;
    private final String networkName;

    private CreateFactomIdentityRequest(DIDVersion didVersion, String networkName, List<FactomKey> keys, String... tags) {
        this.didVersion = didVersion;
        this.networkName = networkName;
        this.keys = keys;
        if (tags == null || tags.length == 0) {
            throw new FactomRuntimeException.AssertionException("A factom identity needs tags/externalIds");
        }
        this.tags = Arrays.stream(tags).sequential().collect(Collectors.toList());
    }

    public List<String> getTags() {
        return this.tags;
    }

    public DIDVersion getDidVersion() {
        return this.didVersion;
    }

    public CreateIdentityContentEntry toCreateIdentityContentEntry() {
        final IdentityEntry identityEntry = new IdentityEntry()
                .version(Integer.valueOf(didVersion.getProtocolVersion()))
                .keys(IdPubMapper.toIdPub(keys));
        return new CreateIdentityContentEntry(identityEntry, this.tags.toArray(new String[] {}));
    }

    public CreateIdentityRequestEntry toCreateIdentityRequestEntry() {
        final CreateIdentityRequest identityRequest = new CreateIdentityRequest()
                .version(Integer.valueOf(didVersion.getProtocolVersion()))
                .keys(keys)
                .tags(tags);
        return new CreateIdentityRequestEntry(identityRequest);
    }

    public DIDDocument toDIDDocument() {
        final CreateIdentityContentEntry createIdentityContentEntry = toCreateIdentityContentEntry();
        String chainId = createIdentityContentEntry.getChainId();
        String did = getDidURL(chainId);
        return new IdentityFactory().toDid(did, createIdentityContentEntry.getContent());
        /*return new FactomDidContent()
                .didMethodVersion(DidMethodVersion.fromValue(didVersion.getSchemaVersion()))
                .didKey(this.keys.stream().map(IdPubMapper::toDidKey)
                        .collect(Collectors.toList()))
                .managementKey(this.managementKeys.stream().map(key -> key.toManagementKey(did))
                        .collect(Collectors.toList()));*/
    }

    private String getDidURL(String chainId) {
        if (networkName == null || Networks.MAINNET.equalsIgnoreCase(networkName)) {
            return DID_FACTOM + chainId;
        }
        return DID_FACTOM + networkName + ':' + chainId;
    }

    public List<FactomKey> getKeys() {
        return keys;
    }

    public static final class Builder {
        private DIDVersion didVersion;
        private List<FactomKey> keys;
        private List<String> tags;
        private String networkName;

        public Builder() {
        }

        public Builder didVersion(DIDVersion didVersion) {
            this.didVersion = didVersion;
            return this;
        }

        public Builder keys(List<FactomKey> keys) {
            this.keys = keys;
            return this;
        }


        public Builder key(FactomKey key) {
            if (this.keys == null) {
                this.keys = new ArrayList<>(Arrays.asList(key));
            } else if (!keys.contains(key)) {
                this.keys.add(key);
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


        public Builder networkName(String networkName) {
            this.networkName = networkName;
            return this;
        }

        public CreateFactomIdentityRequest build() throws IncompleteRequestException {

            this.assertComplete();
            return new CreateFactomIdentityRequest(
                    didVersion,
                    networkName,
                    keys,
                    tags.toArray(new String[0])
            );
        }

        private void assertComplete() throws IncompleteRequestException {
            if (this.keys == null || this.keys.size() == 0) {
                throw new IncompleteRequestException("At least one key is required to create a new DID");
            }
        }
    }
}
