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
import org.factomprotocol.identity.did.model.Service;

import java.lang.reflect.Type;
import java.util.HashMap;

public class CustomServiceTypeAdapter implements JsonSerializer<Service>, JsonDeserializer<Service> {
    @Override
    public Service deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Type mapType = new TypeToken<HashMap<String, String>>() {}.getType();
        HashMap<String, String> serviceMap = jsonDeserializationContext.deserialize(jsonElement, mapType);
        Service service = new Service()
                .id(serviceMap.remove("id"))
                .type(serviceMap.remove("type"))
                .serviceEndpoint(serviceMap.remove("serviceEndpoint"));
        if (serviceMap.get("priorityRequirement") != null) {
            service.priorityRequirement(Integer.valueOf(serviceMap.remove("priorityRequirement")));
        }
        for (String key : serviceMap.keySet()) {
            service.put(key, serviceMap.get(key));
        }
        return service;
    }

    @Override
    public JsonElement serialize(Service service, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("id", jsonSerializationContext.serialize(service.getId()));
        jsonObject.add("type", jsonSerializationContext.serialize(service.getType()));
        jsonObject.add("serviceEndpoint", jsonSerializationContext.serialize(service.getServiceEndpoint()));
        jsonObject.add("priorityRequirement", jsonSerializationContext.serialize(service.getPriorityRequirement()));
        service.keySet().forEach(key -> {
            if (jsonObject.has(key)) {
                throw new JsonSyntaxException("Service object has multiple values for key: " + key);
            }
            jsonObject.add(key, jsonSerializationContext.serialize(service.get(key)));
        });
        return jsonObject;
    }
}
