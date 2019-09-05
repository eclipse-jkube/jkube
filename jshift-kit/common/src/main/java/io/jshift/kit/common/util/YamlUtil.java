package io.jshift.kit.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import org.yaml.snakeyaml.Yaml;

public class YamlUtil {
    protected static Properties getPropertiesFromYamlResource(URL resource) {
        if (resource != null) {
            try (InputStream yamlStream = resource.openStream()) {
                Yaml yaml = new Yaml();
                @SuppressWarnings("unchecked")
                SortedMap<String, Object> source = yaml.loadAs(yamlStream, SortedMap.class);
                Properties properties = new Properties();
                if (source != null) {
                    try {
                        properties.putAll(getFlattenedMap(source));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format("Spring Boot configuration file %s is not formatted correctly. %s",
                                resource.toString(), e.getMessage()));
                    }
                }
                return properties;
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading Yaml resource from URL " + resource, e);
            }
        }
        return new Properties();
    }

    /**
     * Build a flattened representation of the Yaml tree. The conversion is compliant with the thorntail spring-boot rules.
     */
    private static Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object keyObject = entry.getKey();

            // If user creates a wrong application.yml then we get a runtime classcastexception
            if (!(keyObject instanceof String)) {
                throw new IllegalArgumentException(String.format("Expected to find a key of type String but %s with content %s found.",
                        keyObject.getClass(), keyObject.toString()));
            }

            String key = (String) keyObject;

            if (path !=null && path.trim().length()>0) {
                if (key.startsWith("[")) {
                    key = path + key;
                }
                else {
                    key = path + "." + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof Map) {

                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            }
            else if (value instanceof Collection) {
                Collection<Object> collection = (Collection<Object>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result,
                            Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            }
            else {
                result.put(key, (value != null ? value.toString() : ""));
            }
        }
    }

}

