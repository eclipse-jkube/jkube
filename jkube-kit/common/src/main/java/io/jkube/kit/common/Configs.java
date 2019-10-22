package io.jkube.kit.common;

import java.util.Properties;

/**
 * Helper functions for working with typesafe configs
 */
public class Configs {

    // Interfaces to use for dealing with configuration values and default values
    public interface Key {
        String name();
        String def();
    }

    public static int asInt(String value) {
        return value != null ? Integer.parseInt(value) : 0;
    }

    public static Integer asInteger(String value) {
        return value != null ? Integer.parseInt(value) : null;
    }

    public static boolean asBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    public static String asString(String value) { return value; }

    public static String getSystemPropertyWithMavenPropertyAsFallback(Properties properties, String key) {
        String val = System.getProperty(key);
        if (val == null && properties != null) {
            val = properties.getProperty(key);
        }
        return val;
    }

}
