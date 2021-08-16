package com.sphereon.factom.identity.did.mapper;

import org.blockchain_innovation.factom.client.api.json.JsonConverter;
import org.blockchain_innovation.factom.client.api.ops.StringUtils;
import org.factomprotocol.identity.did.model.JwkKey;

import java.util.HashMap;
import java.util.Map;

public class JwkMapper {
    final JsonConverter jsonConverter = JsonConverter.Provider.newInstance();

    public Map<String, Object> toMap(final JwkKey jwkKey) {
        // Easiest way would be to use something like ObjectMapper, or do JSON serialization first
        // Given we want to have as few deps as possible lets opt for a lowfi solution
        final Map<String, Object> map = new HashMap<>();


        // We handle the other properties first, to ensure our members always overwrite the map
        if (jwkKey.getOth() != null) {
            map.putAll(jwkKey.getOth());
        }

        addMapItem(map, "alg", jwkKey.getAlg());
        addMapItem(map, "crv", jwkKey.getCrv());
        addMapItem(map, "d", jwkKey.getD());
        addMapItem(map, "dp", jwkKey.getDp());
        addMapItem(map, "dq", jwkKey.getDq());
        addMapItem(map, "e", jwkKey.getE());
        addMapItem(map, "k", jwkKey.getK());
        addMapItem(map, "kty", jwkKey.getKty() == null ? null : jwkKey.getKty().getValue());
        addMapItem(map, "kid", jwkKey.getKid());
        addMapItem(map, "ops", jwkKey.getKeyOps() == null ? null : jwkKey.getKeyOps().getValue());
        addMapItem(map, "n", jwkKey.getN());
        addMapItem(map, "p", jwkKey.getP());
        addMapItem(map, "q", jwkKey.getQ());

        addMapItem(map, "qi", jwkKey.getQi());
        addMapItem(map, "use", jwkKey.getUse());
        addMapItem(map, "t", jwkKey.getT());
        addMapItem(map, "x", jwkKey.getX());
        addMapItem(map, "y", jwkKey.getY());

        return map;
    }

    private void addMapItem(final Map<String, Object> map, final String key, final Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void addMapItem(final Map<String, Object> map, final String key, final String value) {
        if (StringUtils.isNotEmpty(value)) {
            map.put(key, value);
        }
    }

}
