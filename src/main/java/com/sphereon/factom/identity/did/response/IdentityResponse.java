package com.sphereon.factom.identity.did.response;

import org.factomprotocol.identity.did.model.IdentityEntry;
import org.factomprotocol.identity.did.model.Metadata;

public class IdentityResponse implements BlockchainResponse<IdentityEntry> {
    private IdentityEntry identity;
    private Metadata metadata;

    public IdentityResponse identity(IdentityEntry identity) {
        this.identity = identity;
        return this;
    }

    public IdentityEntry getIdentity() {
        return identity;
    }

    public IdentityResponse metadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public IdentityEntry getContent() {
        return identity;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
