package com.sphereon.factom.identity.did.response;

import com.sphereon.factom.identity.did.DIDVersion;
import org.factomprotocol.identity.did.model.Metadata;

public interface BlockchainResponse<T> {
    T getContent();
    Metadata getMetadata();
    DIDVersion getDidVersion();
}
