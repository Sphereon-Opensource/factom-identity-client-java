package com.sphereon.factom.identity.did;

import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityContentEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityRequestEntry;
import com.sphereon.factom.identity.did.entry.DeactivateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.EntryValidation;
import com.sphereon.factom.identity.did.entry.FactomIdentityEntry;
import com.sphereon.factom.identity.did.entry.IdentityEntryFactory;
import com.sphereon.factom.identity.did.entry.ReplaceKeyIdentityChainEntry;
import com.sphereon.factom.identity.did.entry.ResolvedFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.UpdateFactomDIDEntry;
import com.sphereon.factom.identity.did.parse.RuleException;
import com.sphereon.factom.identity.did.parse.operations.DIDV1CreationCompoundRule;
import com.sphereon.factom.identity.did.parse.operations.FactomIdentityChainCreationCompoundRule;
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
import org.blockchain_innovation.factom.client.api.ops.Encoding;
import org.blockchain_innovation.factom.client.api.ops.EntryOperations;
import org.factomprotocol.identity.did.model.BlockInfo;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.IdentityEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.sphereon.factom.identity.did.Constants.DID.DID_FACTOM;

public class LowLevelIdentityClient {
    private static final IdentityEntryFactory ENTRY_FACTORY = new IdentityEntryFactory();
    private static final EncodeOperations ENCODE_OPS = new EncodeOperations();
    private static final EntryOperations ENTRY_OPS = new EntryOperations();
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

        String chainId = getChainIdFrom(identifier);

        CompletableFuture<List<FactomIdentityEntry<?>>> entriesFuture = getEntryApi().allEntryBlocks(chainId).handle((entryBlockResponses1, throwable) -> {

            final List<FactomIdentityEntry<?>> entries = new ArrayList<>();

            for (EntryBlockResponse entryBlockResponse : entryBlockResponses1) {
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
                    Entry entry = ENCODE_OPS.decodeHex(
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
            return entries;
        });


        try {
            final List<FactomIdentityEntry<?>> entries = entriesFuture.exceptionally(throwable -> {
                try {
                    return getEntryApi().getFactomdClient().pendingEntries(0).thenApply(pendingResponse -> {
                                if (!pendingResponse.hasErrors() && pendingResponse.getResult().contains(chainId)) {
                                    logger.info("Chain id %s is pending ahchoring", chainId);
                                    return new ArrayList<FactomIdentityEntry<?>>();
                                } else {
                                    logger.error("Chain id %s not found", chainId);
                                    return null;
                                }
                            }
                    ).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new DIDRuntimeException(e);
                }

            }).get(5, TimeUnit.SECONDS);

            if (entries != null && entries.size() > 1) {
                Collections.reverse(entries);
            }
            return entries;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new DIDRuntimeException(e);
        }
    }


    /**
     * Create a FactomDID chain by the provided (valid entry)
     *
     * @param createDidEntry
     * @param ecAddress      The paying EC address
     * @return
     */
    public FactomDidContent create(CreateFactomDIDEntry createDidEntry, Address ecAddress) {
        final Chain chain = new Chain().setFirstEntry(createDidEntry.toEntry(Optional.empty()));
        final String chainId = getChainIdFrom(chain);
        try {
            FactomDidContent didContent = new DIDV1CreationCompoundRule(chain).execute();
            if (getEntryApi().chainExists(chain).join()) {
                throw new FactomRuntimeException.AssertionException(String.format("Factom DID chain for id '%s' already exists", chainId));
            } else if (didContent.getManagementKey().size() == 0 || !didContent.getManagementKey().stream().allMatch(managementKey -> managementKey.getId().contains(chainId))) {
                throw new FactomRuntimeException.AssertionException("Management key ids need to correspond to the chain they are part of :" + chainId);
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
    public IdentityEntry create(CreateIdentityRequestEntry identityChainEntry, Address ecAddress) {

        final Chain chain = new Chain().setFirstEntry(identityChainEntry.toEntry(Optional.empty()));
        final String chainId = getChainIdFrom(chain);
        try {
            final IdentityEntry identityEntry = new FactomIdentityChainCreationCompoundRule(chain).execute();
            if (getEntryApi().chainExists(chain).join()) {
                throw new FactomRuntimeException.AssertionException(String.format("Factom identity chain for id '%s' already exists", chainId));
            }
            getEntryApi().commitAndRevealChain(chain, ecAddress).join();
            return identityEntry;
        } catch (RuleException e) {
            throw new FactomRuntimeException(e);
        }
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


    public String getChainIdFrom(Chain chain) {
        return Encoding.HEX.encode(ENTRY_OPS.calculateChainId(chain.getFirstEntry().getExternalIds()));
    }

    public String getChainIdFrom(ResolvedFactomDIDEntry<?> factomDIDEntry) {
        final Chain chain = new Chain().setFirstEntry(factomDIDEntry.toEntry(Optional.empty()));
        return getChainIdFrom(chain);
    }

    public String getChainIdFrom(String identifier) {
        String chainId = identifier;
        if (identifier == null) {
            throw new DIDRuntimeException.InvalidIdentifierException("Identifier must be non-null.");
        } else if (identifier.startsWith(DID_FACTOM)) {
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
