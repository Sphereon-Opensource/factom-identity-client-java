package com.sphereon.factom.identity.did;

import foundation.identity.did.DID;
import foundation.identity.did.DIDURL;
import foundation.identity.did.parser.ParserException;

public enum DIDVersion {
    // This is a wrapper around Factom chains denoted as "IdentityChain" in the 1st external Id.
    // It does not allow full DID documents, nor DID management. It simply translates the IdentityChain into a DID
    FACTOM_IDENTITY_CHAIN("factom", "1", null),

    // 1.0.0 Factom DID specification
    FACTOM_V1_JSON("factom", "1.0.0", "0.2.0");

    private final String method;
    private final String protocolVersion;
    private final String schemaVersion;


    DIDVersion(String method, String protocolVersion, String schemaVersion) {
        this.method = method;
        this.protocolVersion = protocolVersion;
        this.schemaVersion = schemaVersion;
    }

    public void assertFactomMethod(String didUrl) {
        final String didUrlMethod = getDid(didUrl).getMethodName();
        if (!method.equals(didUrlMethod)) {
            throw new DIDRuntimeException("Method of DID URL is not supported by this version of Factom DIDs: " + didUrl);
        }
    }

    public String getMethodSpecificId(String didUrl) {
        return getDid(didUrl).getMethodSpecificId();
    }

    public DIDURL getDidUrl(String didUrl) {
        try {
            return DIDURL.fromString(didUrl);
        } catch (ParserException e) {
            throw new DIDRuntimeException.ParseException(e);
        }
    }

    public DID getDid(String didUrl) {
        return getDidUrl(didUrl).getDid();
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

}
