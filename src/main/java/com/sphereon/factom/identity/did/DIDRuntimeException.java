package com.sphereon.factom.identity.did;

import foundation.identity.did.parser.ParserException;
import org.blockchain_innovation.factom.client.api.errors.FactomRuntimeException;

public class DIDRuntimeException extends FactomRuntimeException {

    public DIDRuntimeException(String message) {
        super(message);
    }

    public DIDRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DIDRuntimeException(Throwable cause) {
        super(cause);
    }

    public static class ParseException extends DIDRuntimeException {
        public ParseException(ParserException cause) {
            super(cause);
        }
    }

    public static class InvalidIdentifierException extends DIDRuntimeException {
        public InvalidIdentifierException(String message) {
            super(message);
        }

        public InvalidIdentifierException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NotFoundException extends DIDRuntimeException {
        public NotFoundException(String message) {
            super(message);
        }

        public NotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
