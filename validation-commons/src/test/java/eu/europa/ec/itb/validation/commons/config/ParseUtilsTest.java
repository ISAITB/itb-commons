package eu.europa.ec.itb.validation.commons.config;

import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ParseUtilsTest {

    static class DataHolder {
        String v1;
        List<Integer> v2;
        DataHolder(String v1, List<Integer> v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }
    
    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }
    
    @Test
    void testParseValueList() {
        var config = new MapConfiguration(Map.of(
                "key.0.subKey1", "A",
                "key.0.subKey2", "1,2",
                "key.1.subKey1", "B",
                "key.1.subKey2", "3,4"
        ));
        var result = ParseUtils.parseValueList("key", config, (values) -> new DataHolder(values.get("subKey1"), Arrays.stream(values.get("subKey2").split(",")).map(Integer::valueOf).collect(Collectors.toList())));
        assertEquals(2, result.size());
        result.sort(Comparator.comparing(v -> v.v1));
        assertEquals("A", result.get(0).v1);
        assertEquals(2, result.get(0).v2.size());
        assertEquals(1, result.get(0).v2.get(0));
        assertEquals(2, result.get(0).v2.get(1));
        assertEquals("B", result.get(1).v1);
        assertEquals(2, result.get(1).v2.size());
        assertEquals(3, result.get(1).v2.get(0));
        assertEquals(4, result.get(1).v2.get(1));
    }

    @Test
    void parseTypedValueList() {
        var config = new MapConfiguration(Map.of(
                "key.type1.0.subKey1", "A",
                "key.type1.0.subKey2", "1,2",
                "key.type2.0.subKey1", "B",
                "key.type2.0.subKey2", "3,4"
        ));
        var result = ParseUtils.parseTypedValueList("key", List.of("type1", "type2"), config, (values) -> new DataHolder(values.get("subKey1"), Arrays.stream(values.get("subKey2").split(",")).map(Integer::valueOf).collect(Collectors.toList())));
        assertEquals(2, result.size());
        assertEquals(1, result.get("type1").size());
        assertEquals("A", result.get("type1").get(0).v1);
        assertEquals(2, result.get("type1").get(0).v2.size());
        assertEquals(1, result.get("type1").get(0).v2.get(0));
        assertEquals(2, result.get("type1").get(0).v2.get(1));
        assertEquals(1, result.get("type2").size());
        assertEquals("B", result.get("type2").get(0).v1);
        assertEquals(2, result.get("type2").get(0).v2.size());
        assertEquals(3, result.get("type2").get(0).v2.get(0));
        assertEquals(4, result.get("type2").get(0).v2.get(1));
    }

    @Test
    void testParseBooleanMap() {
        var config = new MapConfiguration(Map.of(
                "key.type1", "true",
                "key.type2", "false",
                "key.type4", "bad"
        ));
        var result = ParseUtils.parseBooleanMap("key", config, List.of("type1", "type2", "type3", "type4"));
        assertEquals(4, result.size());
        assertTrue(result.get("type1"));
        assertFalse(result.get("type2"));
        assertFalse(result.get("type3"));
        assertFalse(result.get("type4"));
        result = ParseUtils.parseBooleanMap("key", config, List.of("type1", "type2", "type3", "type4"), true);
        assertEquals(4, result.size());
        assertTrue(result.get("type1"));
        assertFalse(result.get("type2"));
        assertTrue(result.get("type3"));
        assertTrue(result.get("type4"));
    }

    @Test
    void testParseCharacterMap() {
        var config = new MapConfiguration(Map.of(
                "key.type1", "A",
                "key.type2", "B"
        ));
        var result = ParseUtils.parseCharacterMap("key", config, List.of("type1", "type2", "type3"), 'X');
        assertEquals(3, result.size());
        assertEquals('A', result.get("type1"));
        assertEquals('B', result.get("type2"));
        assertEquals('X', result.get("type3"));
    }

    @Test
    void testParseEnumMap() {
        var config = new MapConfiguration(Map.of(
                "key.type1", "VALUE1",
                "key.type2", "VALUE2"
        ));
        var result = ParseUtils.parseEnumMap("key", TestEnum.class, TestEnum.VALUE3, config, List.of("type1", "type2", "type3"));
        assertEquals(3, result.size());
        assertEquals(TestEnum.VALUE1, result.get("type1"));
        assertEquals(TestEnum.VALUE2, result.get("type2"));
        assertEquals(TestEnum.VALUE3, result.get("type3"));
        result = ParseUtils.parseEnumMap("key", TestEnum.VALUE3, config, List.of("type1", "type2", "type3"), TestEnum::valueOf);
        assertEquals(3, result.size());
        assertEquals(TestEnum.VALUE1, result.get("type1"));
        assertEquals(TestEnum.VALUE2, result.get("type2"));
        assertEquals(TestEnum.VALUE3, result.get("type3"));
    }

    @Test
    void testParseObjectMap() {
        var config = new MapConfiguration(Map.of(
                "key.commonPart.key1.subKey1", "value1",
                "key.commonPart.key1.subKey2", "value2",
                "key.commonPart.key2.subKey1", "value3",
                "key.commonPart.key2.subKey2", "value4"
        ));
        var result = ParseUtils.parseObjectMap("key.commonPart", config, (key, parts) -> new String[] { parts.get("subKey1"), parts.get("subKey2") });
        assertEquals(2, result.size());
        assertNotNull(result.get("key1"));
        assertEquals(2, result.get("key1").length);
        assertEquals("value1", result.get("key1")[0]);
        assertEquals("value2", result.get("key1")[1]);
        assertNotNull(result.get("key2"));
        assertEquals(2, result.get("key2").length);
        assertEquals("value3", result.get("key2")[0]);
        assertEquals("value4", result.get("key2")[1]);
    }

    @Test
    void testParseMap() {
        var config = new MapConfiguration(Map.of(
                "key.commonPart.key1", "value1",
                "key.commonPart.key2", "value2"
        ));
        var result = ParseUtils.parseMap("key.commonPart", config);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        result = ParseUtils.parseMap("key.commonPart", config, List.of("key1", "key2"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        result = ParseUtils.parseMap("key.commonPart", config, Map.of("key3", "value3"));
        assertEquals(3, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertEquals("value3", result.get("key3"));
    }

}
