package com.sphereon.factom.identity.did.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sphereon.factom.identity.did.Constants;
import org.factomprotocol.identity.did.model.Service;

import java.lang.reflect.Type;
import java.util.HashMap;

public class CustomServiceTypeAdapter implements JsonSerializer<Service>, JsonDeserializer<Service> {

    @Override
    public Service deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Type mapType = new TypeToken<HashMap<String, String>>() {}.getType();
        HashMap<String, String> serviceMap = jsonDeserializationContext.deserialize(jsonElement, mapType);
        Service service = new Service()
                .id(serviceMap.remove(Constants.DID.TERM_ID))
                .type(serviceMap.remove(Constants.DID.TERM_TYPE))
                .serviceEndpoint(serviceMap.remove(Constants.DID.TERM_SERVICE_ENDPOINT));
        if (serviceMap.get(Constants.FactomEntry.PRIORITY_REQUIREMENT) != null) {
            // priority requirement is cast from a double because GSON serializes all numbers as doubles (eg. 1 -> "1.0")
            service.priorityRequirement(Double.valueOf(serviceMap.remove(Constants.FactomEntry.PRIORITY_REQUIREMENT)).intValue());
        }
        for (String key : serviceMap.keySet()) {
            service.put(key, serviceMap.get(key));
        }
        return service;
    }

    @Override
    public JsonElement serialize(Service service, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(Constants.DID.TERM_ID, jsonSerializationContext.serialize(service.getId()));
        jsonObject.add(Constants.DID.TERM_TYPE, jsonSerializationContext.serialize(service.getType()));
        jsonObject.add(Constants.DID.TERM_SERVICE_ENDPOINT, jsonSerializationContext.serialize(service.getServiceEndpoint()));
        jsonObject.add(Constants.FactomEntry.PRIORITY_REQUIREMENT, jsonSerializationContext.serialize(service.getPriorityRequirement()));
        service.keySet().forEach(key -> {
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {

                throw new JsonSyntaxException("Service object has multiple values for key: " + key);
            }
            jsonObject.add(key, jsonSerializationContext.serialize(service.get(key)));
        });
        return jsonObject;
    }
}
