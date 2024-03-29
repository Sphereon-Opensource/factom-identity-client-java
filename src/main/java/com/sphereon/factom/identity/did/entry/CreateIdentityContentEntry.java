package com.sphereon.factom.identity.did.entry;

import com.sphereon.factom.identity.did.DIDVersion;
import org.blockchain_innovation.factom.client.api.model.Entry;
import org.factomprotocol.identity.did.model.BlockInfo;
import org.factomprotocol.identity.did.model.IdentityEntry;

import java.util.List;


public class CreateIdentityContentEntry extends ResolvedFactomDIDEntry<IdentityEntry> {
    private int version;
    private List<String> keys;

    public CreateIdentityContentEntry(IdentityEntry content, String... tags) {
        super(DIDVersion.FACTOM_IDENTITY_CHAIN, content, null, tags);
        this.version = content.getVersion();
        this.keys = content.getKeys();
        initValidationRules();
    }

    public CreateIdentityContentEntry(Entry entry, BlockInfo blockInfo) {
        super(entry, IdentityEntry.class, blockInfo);
        this.didVersion = DIDVersion.FACTOM_IDENTITY_CHAIN;
        this.version = getContent().getVersion();
        this.keys = getContent().getKeys();

        initValidationRules();
    }

    public int getVersion() {
        return version;
    }

    public List<String> getKeys() {
        return keys;
    }
}
