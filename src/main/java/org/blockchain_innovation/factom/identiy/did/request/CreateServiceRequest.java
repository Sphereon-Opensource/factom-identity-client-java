package org.blockchain_innovation.factom.identiy.did.request;

import java.net.URL;

public class CreateServiceRequest {
    private final String serviceIdentifier;
    private final String type;
    private final URL serviceEndpoint;

    public CreateServiceRequest(String serviceIdentifier, String type, URL serviceEndpoint) {
        this.serviceIdentifier = serviceIdentifier;
        this.type = type;
        this.serviceEndpoint = serviceEndpoint;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public String getType() {
        return type;
    }

    public URL getServiceEndpoint() {
        return serviceEndpoint;
    }
}
