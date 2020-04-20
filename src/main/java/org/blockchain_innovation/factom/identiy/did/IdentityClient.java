package org.blockchain_innovation.factom.identiy.did;

import did.DIDDocument;
import org.blockchain_innovation.factom.client.api.settings.RpcSettings;
import org.blockchain_innovation.factom.client.impl.EntryApiImpl;
import org.blockchain_innovation.factom.client.impl.FactomdClientImpl;
import org.blockchain_innovation.factom.client.impl.OfflineWalletdClientImpl;
import org.blockchain_innovation.factom.client.impl.WalletdClientImpl;
import org.blockchain_innovation.factom.client.impl.settings.RpcSettingsImpl;
import org.blockchain_innovation.factom.identiy.did.entry.EntryValidation;
import org.blockchain_innovation.factom.identiy.did.entry.FactomIdentityEntry;
import org.blockchain_innovation.factom.identiy.did.parse.RuleException;
import org.factomprotocol.identity.did.model.IdentityResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class IdentityClient {

    public enum Mode {WALLETD_SIGNING, OFFLINE_SIGNING}

    private final Optional<String> id;
    private FactomdClientImpl factomdClient;
    private LowLevelIdentityClient lowLevelIdentityClient;
    public static final IdentityFactory FACTORY = new IdentityFactory();

    private IdentityClient(Optional<String> id) {
        this.id = id;
    }

    public Optional<String> getId() {
        return id;
    }

    public DIDDocument getDidDocument(String identifier, EntryValidation entryValidation, Optional<Long> blockHeight, Optional<Long> timestamp) throws RuleException {
        return FACTORY.toDid(identifier, getIdentityResponse(identifier, entryValidation, blockHeight, timestamp));
    }


    public IdentityResponse getIdentityResponse(String identifier, EntryValidation entryValidation, Optional<Long> blockHeight, Optional<Long> timestamp) throws RuleException {
        List<FactomIdentityEntry<?>> allEntries = lowLevelClient().getAllEntriesByIdentifier(identifier, entryValidation, blockHeight, timestamp);
        return FACTORY.toIdentity(identifier, allEntries);
    }

    public LowLevelIdentityClient lowLevelClient() {
        assertConfigured();
        return lowLevelIdentityClient;
    }

    public IdentityFactory factory() {
        return FACTORY;
    }


    private void assertConfigured() {
        if (factomdClient == null || lowLevelIdentityClient == null) {
            throw new DIDRuntimeException("Please configure the identity client first before using it");
        }
    }

    protected IdentityClient configure(Mode mode, Properties properties) {
        if (factomdClient != null || lowLevelIdentityClient != null) {
            throw new DIDRuntimeException("You cannot reconfigure an identity client. Please create a new instance");
        }

        EntryApiImpl entryClient = new EntryApiImpl();

        this.factomdClient = new FactomdClientImpl();
        factomdClient.setSettings(new RpcSettingsImpl(RpcSettings.SubSystem.FACTOMD, properties));

        entryClient.setFactomdClient(factomdClient);
        if (mode == Mode.OFFLINE_SIGNING) {
            entryClient.setWalletdClient(new OfflineWalletdClientImpl());
        } else {
            WalletdClientImpl walletdClient = new WalletdClientImpl();
            walletdClient.setSettings(new RpcSettingsImpl(RpcSettings.SubSystem.WALLETD, properties));
            entryClient.setWalletdClient(walletdClient);
        }
        this.lowLevelIdentityClient = new LowLevelIdentityClient(entryClient);
        return this;
    }


    public static class Registry {
        private static Map<String, IdentityClient> instances = new HashMap<>();

        public boolean exists(Optional<String> id) {
            return instances.get(resolve(id)) != null;
        }

        public static IdentityClient get(Optional<String> id) {
            return instances.get(resolve(id));
        }

        public static IdentityClient put(IdentityClient identityClient) {
            IdentityClient current = Registry.get(identityClient.getId());
            if (current != null && current != identityClient) {
                throw new DIDRuntimeException("Cannot register a second identity client with id " + identityClient.getId().orElse("<null>"));
            }
            return instances.put(resolve(identityClient.getId()), identityClient);
        }

        public static IdentityClient put(IdentityClient.Builder identityClientBuilder) {
            return put(identityClientBuilder.build());
        }

        private static String resolve(Optional<String> id) {
            return id.orElse(null);
        }
    }

    public static class Builder {
        private Mode mode = Mode.OFFLINE_SIGNING;
        private Properties properties = new Properties();
        private Optional<String> id = Optional.empty();

        public Builder id(String id) {
            this.id = Optional.ofNullable(id);
            return this;
        }

        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder properties(Properties properties) {
            this.properties = properties;
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
            return new IdentityClient(id).configure(mode, properties);
        }
    }

}
