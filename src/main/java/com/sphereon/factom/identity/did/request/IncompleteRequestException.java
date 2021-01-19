package com.sphereon.factom.identity.did.request;

public class IncompleteRequestException extends RuntimeException {
    public IncompleteRequestException(String message) {
        super(message);
    }
}
