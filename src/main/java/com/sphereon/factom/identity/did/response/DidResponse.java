package com.sphereon.factom.identity.did.response;

import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.Metadata;

public class DidResponse implements BlockchainResponse<FactomDidContent> {
    private FactomDidContent factomDidContent;
    private Metadata metadata;

    public FactomDidContent getFactomDidContent() {
        return factomDidContent;
    }

    public DidResponse factomDidContent(FactomDidContent factomDidContent) {
        this.factomDidContent = factomDidContent;
        return this;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public FactomDidContent getContent(){
        return factomDidContent;
    }

    public DidResponse metadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }
}
