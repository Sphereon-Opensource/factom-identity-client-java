package org.blockchain_innovation.factom.identiy.did.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.factomprotocol.identity.did.model.Service;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class CustomServiceSerializer implements JsonSerializer<Service> {

    @Override
    public JsonElement serialize(Service service, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("id", jsonSerializationContext.serialize(service.getId()));
        jsonObject.add("type", jsonSerializationContext.serialize(service.getType()));
        jsonObject.add("serviceEndpoint", jsonSerializationContext.serialize(service.getServiceEndpoint()));
        jsonObject.add("priorityRequirement", jsonSerializationContext.serialize(service.getPriorityRequirement()));
        service.keySet().forEach(key -> {
            jsonObject.add(key, jsonSerializationContext.serialize(service.get(key)));
        });
        return jsonObject;
    }
}