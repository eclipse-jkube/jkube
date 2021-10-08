/**
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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JKubeFileInterpolator {
    public static final String DEFAULT_FILTER = "${*}";

    private JKubeFileInterpolator() { }

    /**
     * Interpolate a docker file with the given properties and filter
     *
     * @param file file to interpolate.
     * @param properties properties to interpolate in the provided dockerFile.
     * @param filter filter for parsing properties from Dockerfile
     * @return The interpolated contents of the file.
     * @throws IOException if there's a problem while performing IO operations.
     */
    public static String interpolate(File file, Properties properties, String filter) throws IOException {
        StringBuilder ret = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ret.append(JKubeFileInterpolator.interpolate(line, properties, filter != null ? filter : DEFAULT_FILTER)).append(System.lineSeparator());
            }
        }
        return ret.toString();
    }

    /**
     * Replace properties in a string
     *
     * @param line        string provided with parameters
     * @param properties  project properties
     * @param filter      filter for interpolation
     * @return            interpolated string
     */
    public static String interpolate(String line, Properties properties, String filter) {
        return interpolate(line, properties, getExpressionMarkersFromFilter(filter));
    }

    /**
     * Replace properties in a string
     *
     * @param line              string provided with parameters
     * @param properties        properties provided
     * @param expressionMarkers additional markers to use for parameterized expressions
     * @return string with properties parameters replaced
     */
    static String interpolate(String line, Properties properties, Map<String, String> expressionMarkers) {
        return checkForPropertiesInLine(line, properties, expressionMarkers);
    }

    static Map<String, String> getExpressionMarkersFromFilter(String filter) {
        Map<String, String> expressionMarkers = new HashMap<>();
        if (filter != null && !filter.isEmpty() && !filter.equalsIgnoreCase("false")) {
            if (filter.length() == 1) { // Filter is single character: @, # etc
                expressionMarkers.put(filter, filter);
            }

            if (filter.length() > 1 && filter.contains("*")) { // Filter in regex form: ${*}, ${env.*}, etc
                String[] filterParts = filter.split("\\*");
                if (filterParts.length == 2) {
                    expressionMarkers.put(filterParts[0], filterParts[1]);
                }
            }
        }

        return expressionMarkers;
    }

    private static String checkForPropertiesInLine(String line, Properties properties, Map<String, String> delimiters) {
        // Provided properties
        for (String property : properties.stringPropertyNames()) {
            String value = checkPropertyWithDelimiters(line, property, getPropertyAsMap(properties), delimiters);
            if (!StringUtils.isEmpty(value)) {
                line = value;
            }
        }

        // System Properties
        for (String property : System.getProperties().stringPropertyNames()) {
            String value = checkPropertyWithDelimiters(line, property, getPropertyAsMap(properties), delimiters);
            if (!StringUtils.isEmpty(value)) {
                line = value;
            }
        }

        // Environment variables
        Map<String, String> environmentVariables = System.getenv();
        for (Map.Entry<String, String> e : environmentVariables.entrySet()) {
            String value = checkPropertyWithDelimiters(line, e.getKey(), environmentVariables, delimiters);
            if (!StringUtils.isEmpty(value)) {
                line = value;
            }
        }

        return line;
    }

    private static String checkPropertyWithDelimiters(String line, String property, Map<String, String> properties, Map<String, String> expressionMarkers) {
        for (Map.Entry<String, String> delimiter : expressionMarkers.entrySet()) {
            String searchPhrase;

            // form expression string
            if (!property.contains(delimiter.getKey()) && !property.contains(delimiter.getValue())) {
                searchPhrase = (delimiter.getKey() + property + delimiter.getValue());
            } else {
                // Skip if property already contains delimiters
                searchPhrase = property;
            }

            // search line for expression phrase
            if (line != null && line.contains(searchPhrase)) {
                if (isExpressionCycle(properties, expressionMarkers, property)) {
                    throw new IllegalArgumentException("Expression cycle detected, aborting.");
                }

                return replaceWithEscapeStr(line, searchPhrase, properties.get(property));
            }
        }
        return StringUtils.EMPTY;
    }

    private static boolean isExpressionCycle(Map<String, String> properties, Map<String, String> expressionMarkers, String property) {
        String value = properties.get(property);
        // Check normal value
        if (properties.get(value) != null) {
            return true;
        }

        // Check value without delimiters
        if (value != null) {
            value = parsePropertyKey(value, expressionMarkers);
        }
        return properties.get(value) != null;
    }

    private static String parsePropertyKey(String property, Map<String, String> expressionMarkers) {
        for (Map.Entry<String, String> entry : expressionMarkers.entrySet()) {
            if (property.contains(entry.getKey())) {
                property = property.substring(entry.getKey().length(), property.length() - entry.getValue().length());
            }
        }
        return property;
    }

    private static Map<String, String> getPropertyAsMap(Properties properties) {
        Map<String, String> propertyAsMap = new HashMap<>();
        properties.forEach((k, v) -> propertyAsMap.put((String) k, (String) v));
        return propertyAsMap;
    }

    private static String replaceWithEscapeStr(String line, String searchPhrase, String value) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            if (line.charAt(i) == searchPhrase.charAt(0) && i + searchPhrase.length() <= line.length()) {
                String searchSubStr = line.substring(i, i + searchPhrase.length());
                if (searchPhrase.equals(searchSubStr)) {
                    // Replace with value
                    stringBuilder.append(value);
                    i += searchPhrase.length();
                    continue;
                }
            }
            stringBuilder.append(line.charAt(i));
            i++;
        }
        return stringBuilder.toString();
    }
}