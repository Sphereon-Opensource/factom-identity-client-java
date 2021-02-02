package com.sphereon.factom.identity.did.parse;

import com.sphereon.factom.identity.did.DIDVersion;
import foundation.identity.did.parser.ParserException;
import org.blockchain_innovation.factom.client.api.ops.StringUtils;

/**
 * A rule that checks whether a Factom Method Specific Id (:factom: part in the DID Url) is present
 */
public class FactomMethodSpecificIdRule extends AbstractGenericRule<String> {

    private final String didUrl;

    public FactomMethodSpecificIdRule(String didUrl) {
        this.didUrl = didUrl;
    }

    @Override
    public String execute() throws RuleException {
        if (StringUtils.isEmpty(getDidUrl())) {
            throw new RuleException("A Factom DID cannot have an empty DID scheme");
        }
        String methodSpecificId;
        try {
            methodSpecificId = DIDVersion.FACTOM_V1_JSON.getMethodSpecificId(getDidUrl());
        } catch (ParserException e){
            throw new RuleException(e);
        }
        if (StringUtils.isEmpty(methodSpecificId)) {
            throw new RuleException("Invalid Factom DID specified: %s", getDidUrl());
        }
        return methodSpecificId;
    }

    public String getDidUrl() {
        return didUrl;
    }
}
