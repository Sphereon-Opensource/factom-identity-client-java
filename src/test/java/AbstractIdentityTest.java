import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import io.accumulatenetwork.sdk.api.v2.AccumulateSyncApi;
import io.accumulatenetwork.sdk.api.v2.TransactionQueryResult;
import io.accumulatenetwork.sdk.api.v2.TransactionResult;
import io.accumulatenetwork.sdk.generated.apiv2.TransactionQueryResponse;
import io.accumulatenetwork.sdk.generated.apiv2.TxnQuery;
import io.accumulatenetwork.sdk.generated.errors.Status;
import io.accumulatenetwork.sdk.generated.protocol.AddCredits;
import io.accumulatenetwork.sdk.generated.protocol.AddCreditsResult;
import io.accumulatenetwork.sdk.generated.protocol.SignatureType;
import io.accumulatenetwork.sdk.generated.protocol.TransactionType;
import io.accumulatenetwork.sdk.protocol.TxID;
import io.accumulatenetwork.sdk.support.Retry;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import org.blockchain_innovation.accumulate.factombridge.impl.FactomdAccumulateClientImpl;
import org.blockchain_innovation.accumulate.factombridge.impl.settings.RpcSettingsImpl;
import org.blockchain_innovation.accumulate.factombridge.model.LiteAccount;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;
import com.sphereon.factom.identity.did.IdAddressKeyOps;
import com.sphereon.factom.identity.did.IdentityClient;
import com.sphereon.factom.identity.did.IdentityFactory;
import com.sphereon.factom.identity.did.LowLevelIdentityClient;
import org.blockchain_innovation.factom.client.api.settings.RpcSettings;
import org.factomprotocol.identity.did.invoker.JSON;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Properties;

public abstract class AbstractIdentityTest {
    static LiteAccount liteAccount;
    static boolean liteAccountFunded;
    protected static final IdentityFactory IDENTITY_FACTORY = new IdentityFactory();
    public static final IdAddressKeyOps ID_ADDRESS_KEY_CONVERSIONS = new IdAddressKeyOps();
    protected static final Gson GSON = JSON.createGson().create();
    protected IdentityClient identityClient;
    protected LowLevelIdentityClient lowLevelIdentityClient;
    private AccumulateSyncApi accumulate;
    protected final FactomdAccumulateClientImpl factomdClient = new FactomdAccumulateClientImpl();


    @BeforeAll
    public static void initLiteAccount() {
        liteAccount = LiteAccount.generate(SignatureType.ED25519);
    }

    @BeforeEach
    public void setup() throws IOException, URISyntaxException {

        final RpcSettingsImpl settings = new RpcSettingsImpl(RpcSettings.SubSystem.FACTOMD, getProperties(), Optional.of("testnet"));
        factomdClient.setSettings(settings);

        this.identityClient = new IdentityClient.Builder().networkName("testnet").properties(getProperties()).autoRegister(true).build();
        this.lowLevelIdentityClient = identityClient.lowLevelClient();

        accumulate = new AccumulateSyncApi(settings.getServer().getURL().toURI());
        if(!liteAccountFunded) {
            faucet();
            faucet();
            waitForAnchor();
            addCreditsToLiteAccount();
            liteAccountFunded = true;
        }

//        identityClient.lowLevelClient()
//        factomdClient.setSettings(new RpcSettingsImpl(RpcSettings.SubSystem.FACTOMD, getProperties()));
//        offlineEntryClient.setFactomdClient(factomdClient);
//        offlineEntryClient.setWalletdClient(offlineWalletdClient);
//        lowLevelIdentityClient.setEntryApi(offlineEntryClient);
    }

    protected Properties getProperties() throws IOException {
        Properties properties = new Properties();
        InputStream is = getClass().getClassLoader().getResourceAsStream("settings.properties");
        properties.load(is);
        is.close();
        return properties;
    }

    protected KeyPairGenerator getKeyPairGenerator() {
        EdDSANamedCurveSpec curveSpec = EdDSANamedCurveTable.ED_25519_CURVE_SPEC;
        KeyPairGenerator generator = new KeyPairGenerator();
        try {
            generator.initialize(curveSpec, new SecureRandom());
        } catch (InvalidAlgorithmParameterException e) {
            throw new FactomRuntimeException.AssertionException(e);
        }
        return generator;
    }

    protected KeyPair generateKeyPair() {
        return getKeyPairGenerator().generateKeyPair();
    }

    protected EdDSAPrivateKey getPrivateKey(KeyPair keyPair) {
        return (EdDSAPrivateKey) keyPair.getPrivate();
    }


    protected EdDSAPublicKey getPublicKey(KeyPair keyPair) {
        return (EdDSAPublicKey) keyPair.getPublic();
    }


    protected void faucet() {
        final TxID txId = accumulate.faucet(liteAccount.getAccount().getUrl());
        waitForTx(txId);
    }

    protected void addCreditsToLiteAccount() {
        final AddCredits addCredits = new AddCredits()
          .recipient(liteAccount.getAccount().getUrl())
          .amount(BigInteger.valueOf(2000000000L));
        final TransactionResult<AddCreditsResult> transactionResult = accumulate.addCredits(liteAccount, addCredits);
        final AddCreditsResult addCreditsResult = transactionResult.getResult();
        assertNotNull(addCreditsResult);
        assertTrue(addCreditsResult.getCredits() > 0);
        final TransactionQueryResult txQueryResult = waitForTx(transactionResult.getTxID());
        assertEquals(txQueryResult.getTxType(), TransactionType.ADD_CREDITS);
        waitForAnchor();
    }

    private TransactionQueryResult waitForTx(final TxID txId) {
        final AtomicReference<TransactionQueryResult> result = new AtomicReference<TransactionQueryResult>();
        final TxnQuery txnQuery = new TxnQuery()
          .txid(txId.getHash())
          .wait(Duration.ofMinutes(1));
        new Retry()
          .withTimeout(1, ChronoUnit.MINUTES)
          .withDelay(2, ChronoUnit.SECONDS)
          .withMessage("")
          .execute(() -> {
              final TransactionQueryResult txQueryResult = accumulate.getTx(txnQuery);
              assertNotNull(txQueryResult);
              final TransactionQueryResponse queryResponse = txQueryResult.getQueryResponse();
              if (queryResponse.getStatus().getCode() == Status.PENDING) {
                  return true;
              }
              if (!queryResponse.getType().equalsIgnoreCase("syntheticCreateIdentity")) { // TODO syntheticCreateIdentity returns CONFLICT?
                  assertEquals(Status.DELIVERED, queryResponse.getStatus().getCode());
              }
              if (queryResponse.getProduced() != null) {
                  for (TxID producedTxId : queryResponse.getProduced()) {
                      waitForTx(producedTxId);
                  }
              }
              result.set(txQueryResult);
              return false;
          });
        return result.get();
    }

    protected void waitForAnchor() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }
}
