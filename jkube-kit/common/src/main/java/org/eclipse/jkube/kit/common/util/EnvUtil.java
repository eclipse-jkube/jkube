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
package org.eclipse.jkube.kit.common.util;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author roland
 * @since 24.05.17
 */
public class EnvUtil {

    public static final String MAVEN_PROPERTY_REGEXP = "\\s*\\$\\{\\s*([^}]+)\\s*}\\s*$";

    // Standard HTTP port (IANA registered). It is used only in older docker installations.
    public static final String DOCKER_HTTP_PORT = "2375";

    public static final String PROPERTY_COMBINE_POLICY_SUFFIX = "_combine";
    private static final String COMMA = ",";
    private static final String WHITESPACE = " ";
    private static final String COMMA_WHITESPACE = COMMA + WHITESPACE;

    private EnvUtil() {
    }

    // Convert docker host URL to an HTTP(s) URL
    public static String convertTcpToHttpUrl(String connect) {
        String protocol = connect.contains(":" + DOCKER_HTTP_PORT) ? "http:" : "https:";
        return connect.replaceFirst("^tcp:", protocol);
    }

    /**
     * Compare to version strings and return the larger version strings. This is used in calculating
     * the minimal required API version for this plugin. Version strings must be comparable as floating numbers.
     * The versions must be given in the format in a semantic version format (e.g. "1.23"
     * <p> If either version is <code>null</code>, the other version is returned (which can be null as well)
     *
     * @param versionA first version number
     * @param versionB second version number
     * @return the larger version number
     */
    public static String extractLargerVersion(String versionA, String versionB) {
        if (versionB == null || versionA == null) {
            return versionA == null ? versionB : versionA;
        } else {
            String[] partsA = versionA.split("\\.");
            String[] partsB = versionB.split("\\.");
            for (int i = 0; i < (partsA.length < partsB.length ? partsA.length : partsB.length); i++) {
                int pA = Integer.parseInt(partsA[i]);
                int pB = Integer.parseInt(partsB[i]);
                if (pA > pB) {
                    return versionA;
                } else if (pB > pA) {
                    return versionB;
                }
            }
            return partsA.length > partsB.length ? versionA : versionB;
        }
    }

    /**
     * Check whether the first given API version is larger or equals the second given version
     *
     * @param versionA first version to check against
     * @param versionB the second version
     * @return true if versionA is greater or equals versionB, false otherwise
     */
    public static boolean greaterOrEqualsVersion(String versionA, String versionB) {
        String largerVersion = extractLargerVersion(versionA, versionB);
        return largerVersion != null && largerVersion.equals(versionA);
    }

    private static final Function<String, String[]> SPLIT_ON_LAST_COLON = element -> {
        int colon = element.lastIndexOf(':');
        if (colon < 0) {
            return new String[]{element, element};
        } else {
            return new String[]{element.substring(0, colon), element.substring(colon + 1)};
        }
    };

    /**
     * Splits every element in the given list on the last colon in the name and returns a list with
     * two elements: The left part before the colon and the right part after the colon. If the string
     * doesn't contain a colon, the value is used for both elements in the returned arrays.
     *
     * @param listToSplit list of strings to split
     * @return return list of 2-element arrays or an empty list if the given list is empty or null
     */
    public static List<String[]> splitOnLastColon(List<String> listToSplit) {
        if (listToSplit != null) {
            return listToSplit.stream().map(SPLIT_ON_LAST_COLON).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static final Function<String, List<String>> COMMA_SPLITTER =
            input -> Arrays.stream(input.split(COMMA))
                    .filter(StringUtils::isNotBlank)
                    .map(StringUtils::trim)
                    .collect(Collectors.toList());

    /**
     * Remove empty members of a list.
     *
     * @param input A list of String
     * @return A list of Non-Empty (length ;&gt; 0) String
     */
    public static List<String> removeEmptyEntries(List<String> input) {
        if (input == null) {
            return Collections.emptyList();
        }
        return input.stream().filter(StringUtils::isNotBlank).map(StringUtils::trim).collect(Collectors.toList());
    }

    /**
     * Split each element of an Iterable;&lt;String;&gt; at commas.
     *
     * @param input Iterable over strings.
     * @return An Iterable over string which breaks down each input element at comma boundaries
     */
    public static List<String> splitAtCommasAndTrim(Iterable<String> input) {
        if (input == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(input.spliterator(), false)
                .filter(StringUtils::isNotBlank).map(COMMA_SPLITTER).flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static String[] splitOnSpaceWithEscape(String toSplit) {
        String[] split = toSplit.split("(?<!" + Pattern.quote("\\") + ")\\s+");
        String[] res = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            res[i] = split[i].replaceAll("\\\\ ", " ");
        }
        return res;
    }


    /**
     * Join a list of objects to a string with a given separator by calling Object.toString() on the elements.
     *
     * @param list      to join
     * @param separator separator to use
     * @return the joined string.
     */
    public static String stringJoin(List list, String separator) {
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (Object o : list) {
            if (!first) {
                ret.append(separator);
            }
            ret.append(o);
            first = false;
        }
        return ret.toString();
    }

    /**
     * Extract part of given properties as a map. The given prefix is used to find the properties,
     * the rest of the property name is used as key for the map.
     *
     * <p> NOTE: If key is "._combine" ({@link #PROPERTY_COMBINE_POLICY_SUFFIX}) it is ignored! This is reserved for combine policy tweaking.
     *
     * @param prefix     prefix which specifies the part which should be extracted as map
     * @param properties properties to extract from
     * @return the extracted map or null if no such map exists
     */
    public static Map<String, String> extractFromPropertiesAsMap(String prefix, Properties properties) {
        Map<String, String> ret = new HashMap<>();
        Enumeration names = properties.propertyNames();
        String prefixP = prefix + ".";
        while (names.hasMoreElements()) {
            String propName = (String) names.nextElement();
            if (propMatchesPrefix(prefixP, propName)) {
                String mapKey = propName.substring(prefixP.length());
                if (PROPERTY_COMBINE_POLICY_SUFFIX.equals(mapKey)) {
                    continue;
                }

                ret.put(mapKey, properties.getProperty(propName));
            }
        }
        return ret.size() > 0 ? ret : null;
    }

    /**
     * Extract from given properties a list of string values. The prefix is used to determine the subset of the
     * given properties from which the list should be extracted, the rest is used as a numeric index. If the rest
     * is not numeric, the order is not determined (all those props are appended to the end of the list)
     *
     * <p> NOTE: If suffix/index is "._combine" ({@link #PROPERTY_COMBINE_POLICY_SUFFIX}) it is ignored!
     * This is reserved for combine policy tweaking.
     *
     * @param prefix     for selecting the properties from which the list should be extracted
     * @param properties properties from which to extract from
     * @return parsed list or null if no element with prefixes exists
     */
    public static List<String> extractFromPropertiesAsList(String prefix, Properties properties) {
        TreeMap<Integer, String> orderedMap = new TreeMap<>();
        List<String> rest = new ArrayList<>();
        Enumeration names = properties.propertyNames();
        String prefixP = prefix + ".";
        while (names.hasMoreElements()) {
            String key = (String) names.nextElement();
            if (propMatchesPrefix(prefixP, key)) {
                String index = key.substring(prefixP.length());

                if (PROPERTY_COMBINE_POLICY_SUFFIX.equals(index)) {
                    continue;
                }

                String value = properties.getProperty(key);
                try {
                    Integer nrIndex = Integer.parseInt(index);
                    orderedMap.put(nrIndex, value);
                } catch (NumberFormatException exp) {
                    rest.add(value);
                }
            }
        }
        List<String> ret = new ArrayList<>(orderedMap.values());
        ret.addAll(rest);
        return !ret.isEmpty() ? ret : null;
    }

    /**
     * Extract from a Maven property which is in the form ${name} the name.
     *
     * @param propName property name to extract
     * @return the pure name or null if this is not a property name
     */
    public static String extractMavenPropertyName(String propName) {
        Matcher matcher = Pattern.compile(MAVEN_PROPERTY_REGEXP).matcher(propName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    /**
     * Fix path on Windows machines, i.e. convert 'c:\...\' to '/c/..../'
     *
     * @param path path to fix
     * @return the fixed path
     */
    public static String fixupPath(String path) {
        // Hack-fix for mounting on Windows where the ${projectDir} variable and other
        // contain backslashes and what not. Related to #188
        Pattern pattern = Pattern.compile("^(?i)([A-Z]):(.*)$");
        Matcher matcher = pattern.matcher(path);
        if (matcher.matches()) {
            String result = "/" + matcher.group(1).toLowerCase() + matcher.group(2);
            return result.replace("\\", "/");
        }
        return path;
    }

    /**
     * Calculate the duration between now and the given time
     *
     * <p> Taken mostly from http://stackoverflow.com/a/5062810/207604 . Kudos to @dblevins
     *
     * @param start starting time (in milliseconds)
     * @return time in seconds
     */
    public static String formatDurationTill(long start) {
        long duration = System.currentTimeMillis() - start;
        StringBuilder res = new StringBuilder();

        TimeUnit current = HOURS;

        while (duration > 0) {
            long temp = current.convert(duration, MILLISECONDS);

            if (temp > 0) {
                duration -= current.toMillis(temp);
                res.append(temp).append(" ").append(current.name().toLowerCase());
                if (temp < 2) res.deleteCharAt(res.length() - 1);
                res.append(COMMA_WHITESPACE);
            }
            if (current == SECONDS) {
                break;
            }
            current = TimeUnit.values()[current.ordinal() - 1];
        }
        if (res.lastIndexOf(COMMA_WHITESPACE) < 0) {
            return duration + " " + MILLISECONDS.name().toLowerCase();
        }
        res.deleteCharAt(res.length() - 2);
        int i = res.lastIndexOf(COMMA_WHITESPACE);
        if (i > 0) {
            res.deleteCharAt(i);
            res.insert(i, " and");
        }

        return res.toString();
    }

    // ======================================================================================================

    private static boolean propMatchesPrefix(String prefix, String key) {
        return key.startsWith(prefix) && key.length() >= prefix.length();
    }

    /**
     * Return the first non-null registry given. Use the env var DOCKER_REGISTRY as final fallback
     *
     * @param checkFirst list of registries to check
     * @return registry found or null if none.
     */
    public static String firstRegistryOf(String... checkFirst) {
        for (String registry : checkFirst) {
            if (registry != null) {
                return registry;
            }
        }
        // Check environment as last resort
        return System.getenv("DOCKER_REGISTRY");
    }

    // sometimes registries might be specified with https? schema, sometimes not
    public static String ensureRegistryHttpUrl(String registry) {
        if (registry.toLowerCase().startsWith("http")) {
            return registry;
        }
        // Default to https:// schema
        return "https://" + registry;
    }

    public static File prepareAbsoluteOutputDirPath(String outputDirectory, String projectBaseDir, String dir, String path) {
        return prepareAbsolutePath(projectBaseDir, new File(outputDirectory, dir).toString(), path);
    }

    public static File prepareAbsoluteSourceDirPath(String sourceDirectory, String projectBaseDir, String path) {
        return prepareAbsolutePath(projectBaseDir, sourceDirectory, path);
    }

    private static File prepareAbsolutePath(String projectBaseDir, String directory, String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(new File(projectBaseDir, directory), path);
    }

    /**
     * Create a timestamp file holding time in epoch seconds.
     *
     * @param tsFile the File to store the timestamp in.
     * @param buildDate the Date of the timestamp.
     * @throws IOException if the timestamp cannot be created.
     */
    public static void storeTimestamp(File tsFile, Date buildDate) throws IOException {
      try {
        Files.deleteIfExists(tsFile.toPath());
        final File dir = tsFile.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
          throw new IOException("Cannot create directory " + dir);
        }
        Files.write(tsFile.toPath(), Long.toString(buildDate.getTime()).getBytes(StandardCharsets.US_ASCII));
      } catch (IOException e) {
        throw new IOException("Cannot create " + tsFile + " for storing time " + buildDate.getTime(), e);
      }
    }

    public static Date loadTimestamp(File tsFile) throws IOException {
        try {
            if (tsFile.exists()) {
                final String ts = new String(Files.readAllBytes(tsFile.toPath()), StandardCharsets.US_ASCII);
                return new Date(Long.parseLong(ts));
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new IOException("Cannot read timestamp " + tsFile, e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static String getEnvVarOrSystemProperty(String varName, String defaultValue) {
        return getEnvVarOrSystemProperty(varName, varName, defaultValue);
    }

    public static String getEnvVarOrSystemProperty(String envVarName, String systemProperty, String defaultValue) {
        String ret = System.getenv(envVarName);
        if (StringUtils.isNotBlank(ret)){
            return ret;
        }
        return System.getProperty(systemProperty, defaultValue);
    }

}


