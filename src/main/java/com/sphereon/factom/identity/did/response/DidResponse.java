package com.sphereon.factom.identity.did.response;

import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.Metadata;

public class DidResponse {
    private FactomDidContent factomDidContent;
    private Metadata metadata;

    public FactomDidContent getFactomDidContent() {
        return factomDidContent;
    }

    public DidResponse factomDidContent(FactomDidContent factomDidContent) {
        this.factomDidContent = factomDidContent;
        return this;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public DidResponse metadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }
}
