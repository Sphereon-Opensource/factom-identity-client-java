package com.sphereon.factom.identity.did;

import com.sphereon.factom.identity.did.entry.ResolvedFactomDIDEntry;
import com.sphereon.factom.identity.did.request.CreateFactomDidRequest;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.parser.ParserException;
import org.blockchain_innovation.factom.client.api.FactomdClient;
import org.blockchain_innovation.factom.client.api.WalletdClient;
import org.blockchain_innovation.factom.client.api.model.Address;
import org.blockchain_innovation.factom.client.api.model.response.CommitAndRevealChainResponse;
import org.blockchain_innovation.factom.client.impl.EntryApiImpl;
import org.blockchain_innovation.factom.client.impl.Networks;
import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.CreateIdentityRequestEntry;
import com.sphereon.factom.identity.did.entry.EntryValidation;
import com.sphereon.factom.identity.did.entry.FactomIdentityEntry;
import com.sphereon.factom.identity.did.parse.RuleException;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.IdentityEntry;
import org.factomprotocol.identity.did.model.IdentityResponse;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class IdentityClient {

    private final Optional<String> networkName;
    private FactomdClient factomdClient;
    private WalletdClient walletdClient;
    private LowLevelIdentityClient lowLevelIdentityClient;
    public static final IdentityFactory FACTORY = new IdentityFactory();


    private IdentityClient(Optional<String> networkName) {
        this.networkName = networkName;
    }

    public Optional<String> getNetworkName() {
        return networkName;
    }

    public DIDDocument getDidDocument(String identifier, EntryValidation entryValidation, Optional<Long> blockHeight, Optional<Long> timestamp) throws RuleException, ParserException, URISyntaxException {
        return FACTORY.toDid(identifier, getIdentityResponse(identifier, entryValidation, blockHeight, timestamp));
    }


    public IdentityResponse getIdentityResponse(String identifier, EntryValidation entryValidation, Optional<Long> blockHeight, Optional<Long> timestamp) throws RuleException, ParserException {
        List<FactomIdentityEntry<?>> allEntries = lowLevelClient().getAllEntriesByIdentifier(identifier, entryValidation, blockHeight, timestamp);
        return FACTORY.toIdentity(identifier, allEntries);
    }

    public IdentityEntry create(CreateIdentityRequestEntry createRequest, Optional<Address> ecAddress) {
        CommitAndRevealChainResponse commitAndRevealChainResponse = lowLevelClient().create(createRequest, getEcAddress(ecAddress));
        // FIXME: 20/10/2020
        return null;
    }

    public ResolvedFactomDIDEntry<FactomDidContent> create(CreateFactomDidRequest createRequest, Address ecAddress) {
        FactomDidContent factomDidContentRequest = createRequest.toFactomDidContent();
        CreateFactomDIDEntry createEntry = new CreateFactomDIDEntry(
                createRequest.getDidVersion(),
                factomDidContentRequest,
                createRequest.getNonce(),
                createRequest.getTags());
        FactomDidContent factomDidContentResult = lowLevelIdentityClient.create(createEntry, ecAddress);
        return new ResolvedFactomDIDEntry<>(
                createRequest.getDidVersion(),
                factomDidContentResult,
                createRequest.getNonce(),
                createRequest.getTags());
    }


    public LowLevelIdentityClient lowLevelClient() {
        assertConfigured();
        return lowLevelIdentityClient;
    }

    public IdentityFactory factory() {
        return FACTORY;
    }


    private Address getEcAddress(Optional<Address> address) {
        return Networks.getECAddress(getNetworkName(), address);
    }

    private void assertConfigured() {
        if (factomdClient == null || lowLevelIdentityClient == null) {
            throw new DIDRuntimeException("Please configure the identity client first before using it");
        }
    }

    protected IdentityClient configure(Properties properties) {
        if (factomdClient != null || lowLevelIdentityClient != null) {
            throw new DIDRuntimeException("You cannot reconfigure an identity client. Please create a new instance");
        }
        this.factomdClient = Networks.factomd(getNetworkName());
        this.walletdClient = Networks.walletd(getNetworkName());
        EntryApiImpl entryClient = new EntryApiImpl();
        entryClient.setFactomdClient(factomdClient);
        entryClient.setWalletdClient(walletdClient);
        this.lowLevelIdentityClient = new LowLevelIdentityClient(entryClient);
        return this;
    }


    public static class Registry {
        private static Map<String, IdentityClient> instances = new HashMap<>();

        public static boolean exists(Optional<String> id) {
            return instances.get(resolve(id)) != null;
        }

        public static IdentityClient get(Optional<String> id) {
            return instances.get(resolve(id));
        }

        public static IdentityClient put(IdentityClient identityClient) {
            IdentityClient current = Registry.get(identityClient.getNetworkName());
            if (current != null && current != identityClient) {
                throw new DIDRuntimeException("Cannot register a second identity client with id " + identityClient.getNetworkName().orElse("<null>"));
            }
            return instances.put(resolve(identityClient.getNetworkName()), identityClient);
        }

        public static IdentityClient put(IdentityClient.Builder identityClientBuilder) {
            return put(identityClientBuilder.build());
        }

        private static String resolve(Optional<String> id) {
            return id.orElse(Networks.MAINNET);
        }
    }

    public static class Builder {
        private Properties properties = new Properties();
        private File propertiesFile;
        private Optional<String> networkName = Optional.empty();
        private boolean autoRegister = true;

        public Builder networkName(String networkName) {
            this.networkName = Optional.ofNullable(networkName);
            return this;
        }


        public Builder properties(Properties properties) {
            this.properties = properties;
            return this;
        }

        public Builder properties(File propertiesFile) {
            this.propertiesFile = propertiesFile;
            return this;
        }

        public Builder properties(Path propertiesPath) {
            if (propertiesPath != null) {
                this.propertiesFile = propertiesPath.toFile();
            }
            return this;
        }

        public Builder autoRegister(boolean register) {
            this.autoRegister = register;
            return this;
        }

        public Builder properties(Map<String, String> propertiesMap) {
            propertiesMap.forEach((key, value) -> property(key, value));
            return this;
        }

        public Builder property(String key, String value) {
            properties.setProperty(key, value);
            return this;
        }


        public IdentityClient build() {
            if (propertiesFile != null) {
                Networks.init(propertiesFile);
            }
            if (properties != null) {
                Networks.init(properties);
            }

            IdentityClient identityClient = new IdentityClient(networkName).configure(properties);

            if (autoRegister && !Registry.exists(networkName)) {
                Registry.put(identityClient);
            }
            return identityClient;
        }
    }

}
