package com.sphereon.factom.identity.did;

import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityContentEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityRequestEntry;
import com.sphereon.factom.identity.did.entry.DeactivateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.EntryValidation;
import com.sphereon.factom.identity.did.entry.FactomIdentityEntry;
import com.sphereon.factom.identity.did.entry.IdentityEntryFactory;
import com.sphereon.factom.identity.did.entry.ReplaceKeyIdentityChainEntry;
import com.sphereon.factom.identity.did.entry.UpdateFactomDIDEntry;
import com.sphereon.factom.identity.did.parse.RuleException;
import com.sphereon.factom.identity.did.parse.operations.DIDV1CreationCompoundRule;
import foundation.identity.did.parser.ParserException;
import org.blockchain_innovation.factom.client.api.EntryApi;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import org.blockchain_innovation.factom.client.api.log.LogFactory;
import org.blockchain_innovation.factom.client.api.log.Logger;
import org.blockchain_innovation.factom.client.api.model.Address;
import org.blockchain_innovation.factom.client.api.model.Chain;
import org.blockchain_innovation.factom.client.api.model.Entry;
import org.blockchain_innovation.factom.client.api.model.response.CommitAndRevealChainResponse;
import org.blockchain_innovation.factom.client.api.model.response.CommitAndRevealEntryResponse;
import org.blockchain_innovation.factom.client.api.model.response.factomd.EntryBlockResponse;
import org.blockchain_innovation.factom.client.api.model.response.factomd.EntryResponse;
import org.blockchain_innovation.factom.client.api.ops.EncodeOperations;
import org.factomprotocol.identity.did.model.BlockInfo;
import org.factomprotocol.identity.did.model.FactomDidContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LowLevelIdentityClient {
    private static final IdentityEntryFactory ENTRY_FACTORY = new IdentityEntryFactory();
    private static final EncodeOperations ENCODE = new EncodeOperations();
    private static final Logger logger = LogFactory.getLogger(LowLevelIdentityClient.class);

    private EntryApi entryApi;

    public LowLevelIdentityClient() {
    }

    public LowLevelIdentityClient(EntryApi entryApi) {
        setEntryApi(entryApi);
    }

    public LowLevelIdentityClient setEntryApi(EntryApi entryApi) {
        this.entryApi = entryApi;
        return this;
    }


    public EntryApi getEntryApi() {
        if (entryApi == null) {
            throw new FactomRuntimeException.AssertionException("DIDEntryClient needs an entry API to function. Please provide one");
        }
        return entryApi;
    }

    /**
     * Get all entries related to a FactomDID reference.
     *
     * @param identifier The Factom identifier (DID or chainId)
     * @param validate   Validate the entries
     * @return
     */
    public List<FactomIdentityEntry<?>> getAllEntriesByIdentifier(String identifier, EntryValidation validate) throws RuleException, ParserException {
        return getAllEntriesByIdentifier(identifier, validate, Optional.empty(), Optional.empty());
    }

    /**
     * Get all entries related to a Factom chainId or DID.
     *
     * @param identifier   The Factom identifier (DID or chainId)
     * @param validate     Validate the entries
     * @param maxHeight    Optional max height. Can be used to return the identity status at a certain height
     * @param maxTimestamp Optional max timestamp. Can be used to return the identity status at a certain timestamp
     * @return
     * @throws RuleException
     */
    public List<FactomIdentityEntry<?>> getAllEntriesByIdentifier(String identifier, EntryValidation validate, Optional<Long> maxHeight, Optional<Long> maxTimestamp) throws RuleException, ParserException {
        List<FactomIdentityEntry<?>> entries = new ArrayList<>();
        String chainId = getChainIdFrom(identifier);
        List<EntryBlockResponse> entryBlockResponses = getEntryApi().allEntryBlocks(chainId).join();
        for (EntryBlockResponse entryBlockResponse : entryBlockResponses) {
            EntryBlockResponse.Header header = entryBlockResponse.getHeader();

            int size = entryBlockResponse.getEntryList().size();
            for (int i = size - 1; i >= 0; i--) {
                if (entryBlockResponse.getHeader().getDirectoryBlockHeight() > maxHeight.orElse(Long.MAX_VALUE)) {
                    // blockheight is bigger than max supplied height
                    continue;
                }
                if (entryBlockResponse.getHeader().getTimestamp() > maxTimestamp.orElse(Long.MAX_VALUE)) {
                    // timestamp of entry is bigger than max supplied timestamp
                    continue;
                }
                EntryBlockResponse.Entry entryBlockEntry = entryBlockResponse.getEntryList().get(i);
                BlockInfo blockInfo = new BlockInfo()
                        .blockHeight(header.getDirectoryBlockHeight())
                        .blockTimestamp(header.getTimestamp())
                        .entryTimestamp(entryBlockEntry.getTimestamp())
                        .entryHash(entryBlockEntry.getEntryHash());

                EntryResponse entryResponse = getEntryApi().getFactomdClient().entry(entryBlockEntry.getEntryHash()).join().getResult();
                Entry entry = ENCODE.decodeHex(
                        new Entry()
                                .setChainId(entryResponse.getChainId())
                                .setExternalIds(entryResponse.getExtIds())
                                .setContent(entryResponse.getContent()));
                try {
                    entries.add(ENTRY_FACTORY.from(entry, blockInfo, validate));
                } catch (RuleException e) {
                    logger.warn("Entry in chain " + chainId + " was not parsable, with RuleException.", e);
                }

            }
        }
        if (entries.size() > 1) {
            Collections.reverse(entries);
        }
        return entries;
    }


    /**
     * Create a FactomDID chain by the provided (valid entry)
     *
     * @param createDidEntry
     * @param ecAddress      The paying EC address
     * @return
     */
    public FactomDidContent create(CreateFactomDIDEntry createDidEntry, Address ecAddress) {
        Chain chain = new Chain().setFirstEntry(createDidEntry.toEntry(Optional.empty()));
        try {
            FactomDidContent didContent = new DIDV1CreationCompoundRule(chain).execute();
            if (getEntryApi().chainExists(chain).join()) {
                throw new FactomRuntimeException.AssertionException(String.format("Factom DID chain for id '%s' already exists", createDidEntry.getChainId()));
            }
            CommitAndRevealChainResponse chainResponse = getEntryApi().commitAndRevealChain(chain, ecAddress).join();
            return didContent;
        } catch (RuleException e) {
            throw new FactomRuntimeException(e);
        }
    }

    /**
     * Create an identity chain
     *
     * @param identityChainEntry
     * @param ecAddress          The paying EC address
     * @return
     */
    public CommitAndRevealChainResponse create(CreateIdentityRequestEntry identityChainEntry, Address ecAddress) {
        Entry entry = identityChainEntry.toEntry(Optional.empty());
        Chain chain = new Chain().setFirstEntry(entry);
        if (getEntryApi().chainExists(chain).join()) {
            throw new FactomRuntimeException.AssertionException(String.format("Factom identity chain for id '%s' already exists", entry.getChainId()));
        }
        return getEntryApi().commitAndRevealChain(chain, ecAddress).join();
    }


    /**
     * Create an identity chain
     *
     * @param identityContentEntry
     * @param ecAddress            The paying EC address
     * @return
     */
    public CommitAndRevealChainResponse create(CreateIdentityContentEntry identityContentEntry, Address ecAddress, String... tags) {
        Entry entry = identityContentEntry.toEntry(Optional.empty());
        List<String> externalIds = new ArrayList<>();
        if (tags.length == 0 && (identityContentEntry.getAdditionalTags() == null || identityContentEntry.getAdditionalTags().isEmpty())) {
            throw new DIDRuntimeException("Need at least one chain name for an identity chain. None provided");
        }

        if (tags.length > 0) {
            externalIds = Arrays.asList(tags);
            if (identityContentEntry.getAdditionalTags() != null && identityContentEntry.getAdditionalTags().size() > 0) {
                throw new DIDRuntimeException("You cannot specify both tags in the model and in the create call. Need one of the two not both");
            }
        } else {
            externalIds = identityContentEntry.getExternalIds();
        }

        entry.setExternalIds(externalIds);
        Chain chain = new Chain().setFirstEntry(entry);
        if (getEntryApi().chainExists(chain).join()) {
            throw new FactomRuntimeException.AssertionException(String.format("Factom identity chain for id '%s' already exists", identityContentEntry.getChainId()));
        }
        if (getEntryApi().chainExists(chain).join()) {
            throw new DIDRuntimeException(String.format("Chain %s already exists. A created Idenity chain always needs a new chain", chain.getFirstEntry().getChainId()));
        }
        return getEntryApi().commitAndRevealChain(chain, ecAddress).join();
    }


    /**
     * Update a FactomDID chain by the provided (valid entry)
     *
     * @param updateEntry
     * @param ecAddress   The paying EC address
     * @return
     */
    public CommitAndRevealEntryResponse update(UpdateFactomDIDEntry updateEntry, Address ecAddress) {
      /*  if (getEntryApi().allEntryBlocks(chainId).join().size() == 0) {
            throw new FactomRuntimeException.AssertionException(String.format("Factom DID chain for id '%s' did not exist", chainId));
        }*/
        return getEntryApi().commitAndRevealEntry(updateEntry.toEntry(Optional.empty()), ecAddress).join();
    }


    /**
     * Update a FactomDID chain by the provided (valid entry)
     *
     * @param updateEntry
     * @param ecAddress   The paying EC address
     * @return
     */
    public CommitAndRevealEntryResponse update(ReplaceKeyIdentityChainEntry updateEntry, Address ecAddress) {
        return getEntryApi().commitAndRevealEntry(updateEntry.toEntry(Optional.empty()), ecAddress).join();
    }

    /**
     * Deactivate a FactomDID chain by the provided (valid entry)
     *
     * @param deactivateEntry
     * @param ecAddress       The paying EC address
     * @return
     */
    public CommitAndRevealEntryResponse deactivate(DeactivateFactomDIDEntry deactivateEntry, Address ecAddress) {
        return getEntryApi().commitAndRevealEntry(deactivateEntry.toEntry(Optional.empty()), ecAddress).join();
    }

    private String getChainIdFrom(String identifier) {
        String chainId = identifier;
        if (identifier == null) {
            throw new DIDRuntimeException.InvalidIdentifierException("Identifier must be non-null.");
        } else if (identifier.startsWith("did:factom:")) {
            String methodSpecificId = DIDVersion.FACTOM_V1_JSON.getMethodSpecificId(identifier);
            List<String> parts = Arrays.asList(methodSpecificId.split(":"));
            Collections.reverse(parts);
            chainId = parts.stream().filter(part -> part.length() == 64)
                    .findFirst()
                    .orElseThrow(() ->
                            new DIDRuntimeException.InvalidIdentifierException(
                                    "Could not parse chainId from identifier: " + identifier
                            )
                    );
        }
        return chainId;
    }
}
