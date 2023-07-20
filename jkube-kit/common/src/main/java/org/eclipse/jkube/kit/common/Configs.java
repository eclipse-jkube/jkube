/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common;

import java.util.Optional;
import java.util.Properties;

/**
 * Helper functions for working with typesafe configs
 */
public class Configs {

    // Interfaces to use for dealing with configuration values and default values
    public interface Config {
        String name();
        default String getKey() {
            return name();
        }
        default String getDefaultValue() {
            return null;
        }
    }

    /**
     * Returns an int corresponding to the parsed provided value.
     *
     * @param value string to parse.
     * @return parsed int or 0 if value is null.
     */
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

    public static String getFromSystemPropertyWithPropertiesAsFallback(Properties properties, String key) {
        return System.getProperty(key, Optional.ofNullable(properties).map(p -> p.getProperty(key)).orElse(null));
    }

}
