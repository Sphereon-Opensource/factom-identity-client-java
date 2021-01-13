package org.blockchain_innovation.factom.identiy.did.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import org.factomprotocol.identity.did.model.Service;

import java.lang.reflect.Type;

public class CustomServiceSerializer implements JsonSerializer<Service> {

    @Override
    public JsonElement serialize(Service service, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("id", jsonSerializationContext.serialize(service.getId()));
        jsonObject.add("type", jsonSerializationContext.serialize(service.getType()));
        jsonObject.add("serviceEndpoint", jsonSerializationContext.serialize(service.getServiceEndpoint()));
        jsonObject.add("priorityRequirement", jsonSerializationContext.serialize(service.getPriorityRequirement()));
        service.keySet().forEach(key -> {
            if (jsonObject.has(key)){
                throw new JsonSyntaxException("Service object has multiple values for key: " + key);
            }
            jsonObject.add(key, jsonSerializationContext.serialize(service.get(key)));
        });
        return jsonObject;
    }
}
