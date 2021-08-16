package com.sphereon.factom.identity.did.parse.operations;

import com.sphereon.factom.identity.did.Constants;
import com.sphereon.factom.identity.did.OperationValue;
import com.sphereon.factom.identity.did.parse.AssertOperationRule;
import com.sphereon.factom.identity.did.parse.EntrySchemeVersionRule;
import com.sphereon.factom.identity.did.parse.AbstractChainRule;
import com.sphereon.factom.identity.did.parse.ContentDeserializationRule;
import com.sphereon.factom.identity.did.parse.ExternalIdsSizeRule;
import com.sphereon.factom.identity.did.parse.RuleException;
import org.blockchain_innovation.factom.client.api.model.Chain;
import org.factomprotocol.identity.did.model.FactomDidContent;

import java.util.Optional;

/**
 * A compound rule that checks everything related to a first entry to create a DID
 */
public class DIDV1CreationCompoundRule extends AbstractChainRule<FactomDidContent> {

    public DIDV1CreationCompoundRule(Chain chain) {
        super(chain);
    }

    @Override
    public FactomDidContent execute() throws RuleException {
        assertChain();
        assertEntry();

        new ExternalIdsSizeRule(getEntry(), Optional.of(3), Optional.empty()).execute();
        new AssertOperationRule(getEntry(), OperationValue.DID_MANAGEMENT).execute();
        new EntrySchemeVersionRule(getEntry(), Constants.FactomEntry.VERSION_1_0_0).execute();

        ContentDeserializationRule<FactomDidContent> contentDeserializationRule = new ContentDeserializationRule<>(getEntry(), FactomDidContent.class);
        FactomDidContent factomDidContent = contentDeserializationRule.execute();

        return factomDidContent;
    }
}
