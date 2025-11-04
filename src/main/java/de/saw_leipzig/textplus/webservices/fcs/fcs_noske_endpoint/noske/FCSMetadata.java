package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;

public class FCSMetadata {
    private static final Logger LOGGER = LogManager.getLogger(FCSMetadata.class);

    public Map<String, String> title;
    public Map<String, String> description;
    public Map<String, String> institution;
    public String landingpage;

    // TODO: private constructor?

    public static FCSMetadata parseFromJSONString(String raw, String defaultLanguage) {
        FCSMetadata info = new FCSMetadata();

        if (raw != null && raw.stripLeading().startsWith("{")) {
            try {
                StringReader reader = new StringReader(raw);
                JsonParser parser = Json.createParser(reader);

                JsonParser.Event event = parser.next();
                if (!event.equals(JsonParser.Event.START_OBJECT)) {
                    throw new Exception("Expected a JsonObject with structured fields! Got '" + event + "'.");
                }

                JsonObject obj = parser.getObject();

                info.title = extractPossibleMultilingualStrings(obj.getOrDefault("title", null), defaultLanguage);
                info.description = extractPossibleMultilingualStrings(obj.getOrDefault("description", null),
                        defaultLanguage);
                info.institution = extractPossibleMultilingualStrings(obj.getOrDefault("institution", null),
                        defaultLanguage);
                info.landingpage = extractJsonString(obj.getOrDefault("landingpage", null));
            } catch (Exception ex) {
                LOGGER.warn("Unexpected JSON structure for multilingual string: {}", ex.getMessage());
            }
        }

        return info;
    }

    protected static Map<String, String> extractPossibleMultilingualStrings(JsonValue value, String defaultLanguage)
            throws Exception {
        // value does not even exist
        if (value == null) {
            return null;
        }
        // value is literal null
        if (JsonValue.ValueType.NULL.equals(value.getValueType())) {
            return null;
        }
        // value is a string, assume multi-valued but with default language
        if (JsonValue.ValueType.STRING.equals(value.getValueType())) {
            return Collections.singletonMap(defaultLanguage, ((JsonString) value).getString().trim());
        }
        // value should be an object
        if (JsonValue.ValueType.OBJECT.equals(value.getValueType())) {
            JsonObject obj = (JsonObject) value;
            Map<String, String> values = new HashMap<>(obj.size());

            for (Entry<String, JsonValue> entry : obj.entrySet()) {
                String xvalue = extractJsonString(entry.getValue());
                if (xvalue != null) {
                    values.put(entry.getKey(), xvalue);
                }
            }
            return values;
        }
        // undefined behaviour!
        throw new Exception("Undefined object structure! Got type '" + value.getValueType() + "'.");
    }

    protected static String extractJsonString(JsonValue value) throws Exception {
        // value does not even exist
        if (value == null) {
            return null;
        }
        // value is literal null
        if (JsonValue.ValueType.NULL.equals(value.getValueType())) {
            return null;
        }
        // expected string, e.g. '"string"'
        if (JsonValue.ValueType.STRING.equals(value.getValueType())) {
            return ((JsonString) value).getString();
        }
        // otherwise invalid
        throw new Exception("Expected a JSON string value but got '" + value.getValueType() + "'!");
    }

}
