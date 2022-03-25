package eu.europa.ec.itb.validation.commons.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility class to facilitate parsing of properties from configuration.
 */
public class ParseUtils {

    /**
     * Constructor to prevent instantiation.
     */
    private ParseUtils() { throw new IllegalStateException("Utility class"); }

    /**
     * Parse a map of strings.
     *
     * @param commonKey The common property key.
     * @param config The configuration properties.
     * @param defaultValues The map of default values to consider for missing entries.
     * @return The map.
     */
    public static Map<String, String> parseMap(String commonKey, Configuration config, Map<String, String> defaultValues) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> mapKeys = config.getKeys(commonKey);
        while (mapKeys.hasNext()) {
            String fullKey = mapKeys.next();
            String extensionKey = StringUtils.substringAfter(fullKey, commonKey+".");
            map.put(extensionKey, StringUtils.defaultString(config.getString(fullKey)).trim());
        }
        // Add any missing default.
        addMissingDefaultValues(map, defaultValues);
        return map;
    }

    /**
     * Parse a map of strings.
     *
     * @param key The common key part.
     * @param config The configuration properties.
     * @param subKeys The set of sub-keys after the common part to be used as the map's keys.
     * @return The map.
     */
    public static Map<String, String> parseMap(String key, Configuration config, List<String> subKeys) {
        Map<String, String> map = new HashMap<>();
        for (String subKey: subKeys) {
            String val = config.getString(key+"."+subKey, null);
            if (val != null) {
                map.put(subKey, config.getString(key+"."+subKey).trim());
            }
        }
        return map;
    }

    /**
     * Parse a map of strings.
     *
     * @param commonKey The common key part. The remaining part will be the map's keys.
     * @param config The configuration properties.
     * @return The map.
     */
    public static Map<String, String> parseMap(String commonKey, Configuration config) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> mapKeys = config.getKeys(commonKey);
        while (mapKeys.hasNext()) {
            String fullKey = mapKeys.next();
            String extensionKey = StringUtils.substringAfter(fullKey, commonKey+".");
            map.put(extensionKey, StringUtils.defaultString(config.getString(fullKey)).trim());
        }
        return map;
    }

    /**
     * Add missing values to the provided map.
     *
     * @param map The map to complete.
     * @param defaultValues The map of default values to use.
     */
    public static void addMissingDefaultValues(Map<String, String> map, Map<String, String> defaultValues) {
        if (defaultValues != null) {
            for (Map.Entry<String, String> defaultEntry: defaultValues.entrySet()) {
                if (!map.containsKey(defaultEntry.getKey())) {
                    map.put(defaultEntry.getKey(), defaultEntry.getValue());
                }
            }
        }
    }

    /**
     * Parse a list of objects using a helper function.
     *
     * @param key The common key to consider.
     * @param config The configuration properties.
     * @param fnMapper The mapper function.
     * @param <R> The object type.
     * @return The list of objects.
     */
    public static <R> List<R> parseValueList(String key, Configuration config, Function<Map<String, String>, R> fnMapper) {
        List<R> values = new ArrayList<>();
        Iterator<String> it = config.getKeys(key);
        Set<String> processedIndexes = new HashSet<>();
        while (it.hasNext()) {
            String typedKey = it.next();
            String index = typedKey.replaceAll("(" + key + ".)([0-9]{1,})(.[a-zA-Z0-9]*)", "$2");
            if (!processedIndexes.contains(index)) {
                processedIndexes.add(index);
                Map<String, String> propertiesToMap = new HashMap<>();
                Iterator<String> it2 = config.getKeys(key + "." + index);
                while (it2.hasNext()) {
                    String specificProperty = it2.next();
                    String configToMap = specificProperty.substring(specificProperty.lastIndexOf('.')+1);
                    propertiesToMap.put(configToMap, config.getString(specificProperty));
                }
                values.add(fnMapper.apply(propertiesToMap));
            }
        }
        return values;
    }

    /**
     * Parse a map of validation type to list of objects using a helper function.
     *
     * @param key The common property key.
     * @param types The validation types.
     * @param config The configuration properties.
     * @param fnMapper The mapper function.
     * @param <R> The type of object to return within the lists.
     * @return The map.
     */
    public static <R> Map<String, List<R>> parseTypedValueList(String key, List<String> types, Configuration config, Function<Map<String, String>, R> fnMapper) {
        Map<String, List<R>> configValues = new HashMap<>();
        for (String type: types) {
            configValues.put(type, parseValueList(key + "." + type, config, fnMapper));
        }
        return configValues;
    }

    /**
     * Parse a map of boolean values per validation type.
     *
     * @param key The common property key.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param defaultIfMissing The default value for missing properties.
     * @return The map.
     */
    public static Map<String, Boolean> parseBooleanMap(String key, Configuration config, List<String> types, boolean defaultIfMissing) {
        Map<String, Boolean> map = new HashMap<>();
        for (String type: types) {
            boolean value;
            try {
                value = config.getBoolean(key+"."+type, defaultIfMissing);
            } catch (Exception e) {
                value = defaultIfMissing;
            }
            map.put(type, value);
        }
        return map;
    }

    /**
     * Parse a map of validation type to characters.
     *
     * @param key The common property key.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param defaultIfMissing The default character for missing entries.
     * @return The map.
     */
    public static Map<String, Character> parseCharacterMap(String key, Configuration config, List<String> types, char defaultIfMissing) {
        Map<String, Character> map = new HashMap<>();
        for (String type: types) {
            String value;
            try {
                value = config.getString(key+"."+type, Character.toString(defaultIfMissing));
            } catch (Exception e) {
                value = Character.toString(defaultIfMissing);
            }
            map.put(type, value.toCharArray()[0]);
        }
        return map;
    }

    /**
     * Parse a map of validation type to boolean values.
     *
     * @param key The common property key.
     * @param config The configuration properties.
     * @param types The validation types.
     * @return The map (using false for missing entries).
     */
    public static Map<String, Boolean> parseBooleanMap(String key, Configuration config, List<String> types) {
        return parseBooleanMap(key, config, types, false);
    }

    /**
     * Parse a map of validation type to enum instance.
     *
     * @param key The common property key.
     * @param enumType The class of the enum.
     * @param defaultValue The default enum value for missing entries.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param <R> The class of the enum.
     * @return The map.
     */
    public static <R extends Enum<R>> Map<String, R> parseEnumMap(String key, Class<R> enumType, R defaultValue, Configuration config, List<String> types) {
        Map<String, R> map = new HashMap<>();
        for (String type: types) {
            map.put(type, R.valueOf(enumType, config.getString(key+"."+type, defaultValue.name())));
        }
        return map;
    }

    /**
     * Parse a map of validation type to arbitrary objects.
     *
     * @param commonKey The common property key.
     * @param config The configuration properties.
     * @param fnObjectBuilder The mapper function to construct objects.
     * @param <R> The type of each object.
     * @return The map.
     */
    public static <R> Map<String, R> parseObjectMap(String commonKey, Configuration config, BiFunction<String, Map<String, String>, R> fnObjectBuilder) {
        Map<String, R> map = new HashMap<>();
        Iterator<String> mapKeys = config.getKeys(commonKey);
        Map<String, Map<String, String>> collectedData = new HashMap<>();
        while (mapKeys.hasNext()) {
            String fullKey = mapKeys.next();
            String[] partsAfterCommon = StringUtils.split(StringUtils.substringAfter(fullKey, commonKey+"."), '.');
            if (partsAfterCommon != null && partsAfterCommon.length == 2) {
                Map<String, String> instanceData = collectedData.computeIfAbsent(partsAfterCommon[0], key -> new HashMap<>());
                instanceData.put(partsAfterCommon[1], config.getString(fullKey));
            }
        }
        collectedData.forEach((key, value) -> {
            R obj = fnObjectBuilder.apply(key, value);
            if (obj != null) {
                map.put(key, obj);
            }
        });
        return map;
    }

    /**
     * Parse a map of validation type to map of enums.
     *
     * @param key The common property key.
     * @param defaultValue The default enum to use for missing entries.
     * @param config The configuration properties.
     * @param types The validation types.
     * @param fnEnumBuilder The function used to construct the map of enums. The key is the part following the validation type.
     * @param <R> The type of the enums.
     * @return The map.
     */
    public static <R extends Enum<R>> Map<String, R> parseEnumMap(String key, R defaultValue, Configuration config, List<String> types, Function<String,R> fnEnumBuilder) {
        Map<String, R> map = new HashMap<>();
        for (String type: types) {
            if (config.containsKey(key+"."+type)) {
                map.put(type, fnEnumBuilder.apply(config.getString(key+"."+type)));
            } else {
                map.put(type, defaultValue);
            }
        }
        return map;
    }

}
