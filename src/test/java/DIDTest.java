import com.google.gson.stream.JsonReader;
import com.sphereon.factom.identity.did.response.IdentityResponse;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.parser.ParserException;
import org.blockchain_innovation.factom.client.api.model.Address;
import com.sphereon.factom.identity.did.DIDVersion;
import com.sphereon.factom.identity.did.entry.CreateFactomDIDEntry;
import com.sphereon.factom.identity.did.entry.EntryValidation;
import com.sphereon.factom.identity.did.entry.FactomIdentityEntry;
import com.sphereon.factom.identity.did.entry.ResolvedFactomDIDEntry;
import com.sphereon.factom.identity.did.parse.RuleException;
import com.sphereon.factom.identity.did.request.CreateFactomDidRequest;
import com.sphereon.factom.identity.did.request.CreateKeyRequest;
import com.sphereon.factom.identity.did.request.CreateServiceRequest;
import org.factomprotocol.identity.did.model.DidKey;
import org.factomprotocol.identity.did.model.FactomDidContent;
import org.factomprotocol.identity.did.model.KeyPurpose;
import org.factomprotocol.identity.did.model.KeyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DIDTest extends AbstractIdentityTest {
    public static final String TEST_IDENTITY_CHAINID = "6aa7d4afe4932885b5b6e93accb5f4f6c14bd1827733e05e3324ae392c0b2764";
    private FactomDidContent factomDidContent;

    @BeforeEach
    public void init() throws FileNotFoundException {
        String filename = "ExampleDidContent.json";
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(filename)).getFile());
        InputStream inputStream = new FileInputStream(file);
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
        factomDidContent = GSON.fromJson(reader, FactomDidContent.class);
    }

    @Test
    public void test() throws RuleException, ParserException {
        String ES_ADDRESS = "Es4JHJ7T2E34j2Xqg84jWZRvgJ1cBtZZMseL2GxaEwJ7PigV23dh";
        String nonce = "test-" + System.currentTimeMillis();
        String chainId = new CreateFactomDIDEntry(DIDVersion.FACTOM_V1_JSON, null, nonce).getChainId();
        assertNotNull(chainId);

        CreateFactomDIDEntry createEntry = new CreateFactomDIDEntry(DIDVersion.FACTOM_V1_JSON, factomDidContent, nonce);

        String didURL = "did:factom:" + chainId;
        String targetId = DIDVersion.FACTOM_V1_JSON.getMethodSpecificId(didURL);
        String keyId = "did:factom:" + chainId + "#keys-1";
//        DIDDocument didDocument = DIDDocument.build(didReference, null, null, null);

        FactomDidContent commitAndRevealChainResponse = lowLevelIdentityClient.create(createEntry, new Address(ES_ADDRESS));

        System.err.println(commitAndRevealChainResponse);
        List<FactomIdentityEntry<?>> identityEntries = lowLevelIdentityClient.getAllEntriesByIdentifier(didURL, EntryValidation.THROW_ERROR);
        assertNotNull(identityEntries);
        FactomIdentityEntry<?> identityEntry = identityEntries.get(0);
        assertNotNull(identityEntry);
        // todo This is not a proper update for now
        // CommitAndRevealEntryResponse updateEntryResponse = lowLevelDidClient.update(didDocument, nonce, keyId, new Address(EC_SECRET_ADDRESS));
        //todo This is not a proper deactivate for now
        // CommitAndRevealEntryResponse deactivateEntryResponse = lowLevelDidClient.deactivate(didDocument, keyId, new Address(EC_SECRET_ADDRESS));
    }

    @Test
    @Disabled
    public void createFactomDID() throws MalformedURLException {
        CreateKeyRequest managementKey = new CreateKeyRequest.Builder()
                .type(KeyType.ED25519VERIFICATIONKEY)
                .keyIdentifier("management-0")
                .publicKeyBase58("H3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV")
                .priority(0)
                .build();
        CreateKeyRequest didKey = new CreateKeyRequest.Builder()
                .type(KeyType.ED25519VERIFICATIONKEY)
                .keyIdentifier("public-0")
                .publicKeyBase58("3uVAjZpfMv6gmMNam3uVAjZpfkcJCwDwnZn6MNam3uVA")
                .priorityRequirement(1)
                .purpose(Arrays.asList(KeyPurpose.AUTHENTICATION, KeyPurpose.PUBLICKEY))
                .build();
        CreateServiceRequest service = new CreateServiceRequest.Builder()
                .serviceIdentifier("cr-0")
                .type("CredentialRepositoryService")
                .serviceEndpoint(new URL("https://repository.example.com/service/8377464"))
                .priorityRequirement(0)
                .build();
        CreateFactomDidRequest createRequest = new CreateFactomDidRequest.Builder()
                .didVersion(DIDVersion.FACTOM_V1_JSON)
                .managementKeys(Collections.singletonList(managementKey))
                .didKeys(Collections.singletonList(didKey))
                .services(Collections.singletonList(service))
                .networkName("testnet")
                .nonce("test-" + System.currentTimeMillis())
                .tag("test")
                .tag("did")
                .build();
        ResolvedFactomDIDEntry<FactomDidContent> content = identityClient.create(createRequest, new Address("Es4JHJ7T2E34j2Xqg84jWZRvgJ1cBtZZMseL2GxaEwJ7PigV23dh"));
        DidKey didKeyResult = content.getContent().getDidKey().get(0);
        assertNotNull(didKeyResult);
    }

    @Test
    public void getDidDocumentFromIdentityChain() throws RuleException, ParserException, URISyntaxException {

        List<FactomIdentityEntry<?>> allEntries = lowLevelIdentityClient.getAllEntriesByIdentifier("did:factom:" + TEST_IDENTITY_CHAINID, EntryValidation.THROW_ERROR);
        assertNotNull(allEntries);

        IdentityResponse identityResponse = IDENTITY_FACTORY.toIdentity("did:factom:" + TEST_IDENTITY_CHAINID, allEntries);
        DIDDocument didDocument = IDENTITY_FACTORY.toDid("did:factom:" + TEST_IDENTITY_CHAINID, identityResponse);
        assertNotNull(didDocument);
        System.err.println(didDocument.toString());
    }


}
