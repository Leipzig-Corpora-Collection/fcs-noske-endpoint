package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonValue;

public class FCSMetadataTest {

    static String defaultLang = "en";

    @Test
    public void testParseFromJSONString() {
        FCSMetadata result = FCSMetadata.parseFromJSONString(null, defaultLang);
        // we always receive an object
        assertNotNull(result);
        // but fields may still be null
        assertNull(result.title);

        // in case of invalid JSON, too
        result = FCSMetadata.parseFromJSONString("{", defaultLang);
        // we always receive an object
        assertNotNull(result);
        // but fields may still be null
        assertNull(result.title);

        // TODO: write some more test with actual values
        result = FCSMetadata.parseFromJSONString("{\"institution\": {\"de\": \"test\"}}", defaultLang);
        Map<String, String> expected = Collections.singletonMap("de", "test");
        assertEquals(expected, result.institution);
    }

    @Test
    public void testExtractPossibleMultilingualStrings() throws Exception {
        assertNull(FCSMetadata.extractPossibleMultilingualStrings(null, defaultLang));
        assertNull(FCSMetadata.extractPossibleMultilingualStrings(JsonValue.NULL, defaultLang));

        // simple string
        String content = "abc";
        Map<String, String> expected = Collections.singletonMap(defaultLang, content);
        Map<String, String> result = FCSMetadata.extractPossibleMultilingualStrings(Json.createValue(content),
                defaultLang);
        assertEquals(expected, result);

        expected = Collections.emptyMap();
        // empty map is empty
        result = FCSMetadata.extractPossibleMultilingualStrings(Json.createObjectBuilder().build(), defaultLang);
        assertEquals(expected, result);
        // skip empty values
        result = FCSMetadata.extractPossibleMultilingualStrings(
                Json.createObjectBuilder().add(defaultLang, JsonValue.NULL).build(), defaultLang);
        assertEquals(expected, result);

        expected = Collections.singletonMap(defaultLang, content);
        result = FCSMetadata.extractPossibleMultilingualStrings(
                Json.createObjectBuilder().add(defaultLang, content).build(), defaultLang);
        assertEquals(expected, result);

        expected = Map.of("en", "en string", "de", "de string");
        result = FCSMetadata.extractPossibleMultilingualStrings(
                Json.createObjectBuilder().add("en", "en string").add("de", "de string").build(), defaultLang);
        assertEquals(expected, result);
        result = FCSMetadata.extractPossibleMultilingualStrings(
                Json.createObjectBuilder()
                        .add("en", "en string")
                        .add("de", "de string")
                        .add("fr", JsonValue.NULL) // ignored, no key without value
                        .build(),
                defaultLang);
        assertEquals(expected, result);

        // errors
        assertThrows(Exception.class,
                () -> FCSMetadata.extractPossibleMultilingualStrings(Json.createArrayBuilder().build(), defaultLang));
    }

    @Test
    public void testExtractJsonString() throws Exception {
        assertNull(FCSMetadata.extractJsonString(null));
        assertNull(FCSMetadata.extractJsonString(JsonValue.NULL));
        assertEquals("test", FCSMetadata.extractJsonString(Json.createValue("test")));

        assertThrows(Exception.class,
                () -> FCSMetadata.extractJsonString(Json.createValue(1)));
        assertThrows(Exception.class,
                () -> FCSMetadata.extractJsonString(JsonValue.FALSE));
        assertThrows(Exception.class,
                () -> FCSMetadata.extractJsonString(Json.createObjectBuilder().build()));
        assertThrows(Exception.class,
                () -> FCSMetadata.extractJsonString(Json.createArrayBuilder().build()));
    }

}
