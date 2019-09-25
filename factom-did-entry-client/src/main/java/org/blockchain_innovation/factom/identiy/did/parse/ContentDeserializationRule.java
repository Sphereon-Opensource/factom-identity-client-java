package org.blockchain_innovation.factom.identiy.did.parse;

import com.google.gson.JsonSyntaxException;
import org.blockchain_innovation.factom.client.api.model.Entry;
import org.blockchain_innovation.factom.client.api.ops.StringUtils;
import org.factom_protocol.identifiers.did.invoker.JSON;

/**
 * A rule that deserializes content from an entry using Gson and does some null/deserialization checks
 *
 * @param <T>
 */
public class ContentDeserializationRule<T> extends AbstractEntryRule<T> {
    private Class<T> tClass;

    public ContentDeserializationRule(Entry entry, Class<T> tClass) {
        super(entry);
        this.tClass = tClass;
    }

    @Override
    public T execute() throws RuleException {
        assertEntry();
        if (StringUtils.isEmpty(getEntry().getContent())) {
            throw new RuleException("Entry needs content for: %s", tClass.getSimpleName());
        }
        try {
            T result = JSON.createGson().create().fromJson(getEntry().getContent(), tClass);
            if (result == null) {
                throw new RuleException("Could not deserialize Entry content into a Factom Entry Content object of class %s", tClass.getSimpleName());
            }

            return result;
        } catch (JsonSyntaxException e) {
            throw new RuleException(e);
        }

    }


}
