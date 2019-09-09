package org.blockchain_innovation.factom.identiy.did.parse.operations;

import org.blockchain_innovation.factom.client.api.model.Chain;
import org.blockchain_innovation.factom.identiy.did.entry.OperationValue;
import org.blockchain_innovation.factom.identiy.did.parse.*;
import org.factom_protocol.identifiers.did.model.FactomDidContent;

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
        new AssertOperationRule(getChain(), OperationValue.DID_MANAGEMENT).execute();
        new EntrySchemeVersionRule(getEntry(), "1.0.0").execute();

        ContentDeserializationRule<FactomDidContent> contentDeserializationRule = new ContentDeserializationRule<>(getChain(), FactomDidContent.class);
        FactomDidContent factomDidContent = contentDeserializationRule.execute();

        return factomDidContent;
    }
}
