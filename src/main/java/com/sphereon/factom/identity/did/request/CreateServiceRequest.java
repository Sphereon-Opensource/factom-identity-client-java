package com.sphereon.factom.identity.did.request;

import org.factomprotocol.identity.did.model.Service;

import java.net.URL;

public class CreateServiceRequest {
    private final String serviceIdentifier;
    private final String type;
    private final URL serviceEndpoint;
    private final int priorityRequirement;

    private CreateServiceRequest(String serviceIdentifier, String type, URL serviceEndpoint, int priorityRequirement) {
        this.serviceIdentifier = serviceIdentifier;
        this.type = type;
        this.serviceEndpoint = serviceEndpoint;
        this.priorityRequirement = priorityRequirement;
    }

    public Service toService(String controller) {
        Service service =  new Service()
                .id(controller + '#' + serviceIdentifier)
                .type(type)
                .serviceEndpoint(serviceEndpoint.toString())
                .priorityRequirement(priorityRequirement);
        return service;
    }

    public static final class Builder {
        private String serviceIdentifier;
        private String type;
        private URL serviceEndpoint;
        private int priorityRequirement;

        public Builder() {
        }

        public Builder serviceIdentifier(String serviceIdentifier) {
            this.serviceIdentifier = serviceIdentifier;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder serviceEndpoint(URL serviceEndpoint) {
            this.serviceEndpoint = serviceEndpoint;
            return this;
        }

        public Builder priorityRequirement(int priorityRequirement) {
            this.priorityRequirement = priorityRequirement;
            return this;
        }

        public CreateServiceRequest build() {
            return new CreateServiceRequest(serviceIdentifier, type, serviceEndpoint, priorityRequirement);
        }
    }
}
