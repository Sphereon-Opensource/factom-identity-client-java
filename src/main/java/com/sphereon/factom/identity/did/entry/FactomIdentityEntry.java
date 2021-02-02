package com.sphereon.factom.identity.did.entry;

import com.sphereon.factom.identity.did.DIDVersion;
import com.sphereon.factom.identity.did.OperationValue;
import com.sphereon.factom.identity.did.parse.RuleException;
import org.blockchain_innovation.factom.client.api.model.Entry;
import org.factomprotocol.identity.did.model.BlockInfo;

import java.util.List;
import java.util.Optional;

public interface FactomIdentityEntry<T> {
    List<String> getExternalIds();

    T getContent();

    OperationValue getOperationValue();

    Entry toEntry(Optional<String> chainId);

    void validate() throws RuleException;

    DIDVersion getDidVersion();

    String getChainId();

    Optional<BlockInfo> getBlockInfo();
}
