package com.sphereon.factom.identity.did.response;

import org.factomprotocol.identity.did.model.Metadata;

public interface BlockchainResponse<T> {
    T getContent();
    Metadata getMetadata();
}
