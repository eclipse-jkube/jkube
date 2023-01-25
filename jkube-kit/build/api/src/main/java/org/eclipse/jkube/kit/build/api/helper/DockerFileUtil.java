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
package org.eclipse.jkube.kit.build.api.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jkube.kit.common.JKubeFileInterpolator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for dealing with dockerfiles
 * @author roland
 * @since 21/01/16
 */
public class DockerFileUtil {
    private static final String ARG_PATTERN_REGEX = "\\$([\\w|\\-\\.]+)|\\$\\{([\\w\\-|\\.]+)\\}";

    private static final Pattern argPattern = Pattern.compile(ARG_PATTERN_REGEX);

    private DockerFileUtil() {}

    /**
     * Extract the base images from a dockerfile. All lines containing a <code>FROM</code> is
     * taken.
     *
     * @param dockerFile file from where to extract the base image.
     * @param properties properties to interpolate in the provided dockerFile.
     * @param filter string representing filter parameters for properties in dockerfile
     * @param argsFromBuildConfig Docker Build args received from Build Configuration
     * @return LinkedList of base images name or empty collection if none is found.
     * @throws IOException if there's a problem while performing IO operations.
     */
    public static List<String> extractBaseImages(File dockerFile, Properties properties, String filter, Map<String, String> argsFromBuildConfig) throws IOException {
        List<String[]> fromLines = extractLines(dockerFile, "FROM", properties, resolveDockerfileFilter(filter));
        Map<String, String> args = extractArgs(dockerFile, properties, filter, argsFromBuildConfig);
        Set<String> result = new LinkedHashSet<>();
        Set<String> fromAlias = new HashSet<>();
        for (String[] fromLine :  fromLines) {
            if (fromLine.length == 2) { // FROM image:tag use case
                result.add(resolveImageTagFromArgs(fromLine[1], args));
            } else if (fromLine.length == 4) { // FROM image:tag AS alias use case
                if (!fromAlias.contains(fromLine[1])) {
                    result.add(resolveImageTagFromArgs(fromLine[1], args));
                }
                fromAlias.add(resolveImageTagFromArgs(fromLine[3], args));
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Extract Args from dockerfile. All lines containing ARG is taken.
     *
     * @param dockerfile Docker File
     * @param properties properties
     * @param filter interpolation filter
     * @param argsFromBuildConfig Docker build args from Build Configuration
     * @return HashMap of arguments or empty collection if none is found
     */
    public static Map<String, String> extractArgs(File dockerfile, Properties properties, String filter, Map<String, String> argsFromBuildConfig) throws IOException {
        return extractArgsFromLines(extractLines(dockerfile, "ARG", properties, filter), argsFromBuildConfig);
    }

    /**
     * Extract all lines containing the given keyword
     *
     * @param dockerFile dockerfile to examine.
     * @param keyword keyword to extract the lines for.
     * @param properties properties to interpolate in the provided dockerFile.
     * @param filter string representing filter parameters for properties in dockerfile
     * @return list of matched lines or an empty list.
     */
    public static List<String[]> extractLines(File dockerFile, String keyword, Properties properties, String filter) throws IOException {
        List<String[]> ret = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dockerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lineInterpolated = JKubeFileInterpolator.interpolate(line, properties, filter);
                String[] lineParts = lineInterpolated.split("\\s+");
                if (lineParts.length > 0 && lineParts[0].equalsIgnoreCase(keyword)) {
                    ret.add(lineParts);
                }
            }
        }
        return ret;
    }

    /**
     * Interpolate a docker file with the given properties and filter
     *
     * @param dockerFile docker file to interpolate.
     * @param properties properties to interpolate in the provided dockerFile.
     * @param filter filter for parsing properties from Dockerfile
     * @return The interpolated contents of the file.
     * @throws IOException if there's a problem while performing IO operations.
     */
    public static String interpolate(File dockerFile, Properties properties, String filter) throws IOException {
      return JKubeFileInterpolator.interpolate(dockerFile, properties, filter);
    }

    private static Reader getFileReaderFromDir(File file) {
        if (file.exists() && file.length() != 0) {
            try {
                return new FileReader(file);
            } catch (FileNotFoundException e) {
                // Shouldn't happen. Nevertheless ...
                throw new IllegalStateException("Cannot find " + file,e);
            }
        } else {
            return null;
        }
    }

    /**
     * Helper method for extractArgs(exposed for test)
     *
     * @param argLines list of string arrays containing lines with words
     * @param argsFromBuildConfig Docker build args from Build Configuration
     * @return map of parsed arguments
     */
    protected static Map<String, String> extractArgsFromLines(List<String[]> argLines, Map<String, String> argsFromBuildConfig) {
        Map<String, String> result = new HashMap<>();
        for (String[] argLine : argLines) {
            if (argLine.length > 1) {
                updateMapWithArgValue(result, argsFromBuildConfig, argLine[1]);
            }
        }
        return result;
    }

    static String resolveImageTagFromArgs(String imageTagString, Map<String, String> args) {
        String resolvedImageString = imageTagString;
        Set<String> foundArgs = findAllArgs(imageTagString);
        for (String foundArg : foundArgs) {
            if (args.containsKey(foundArg)) {
                resolvedImageString = resolvedImageString.replaceFirst(String.format("\\$\\{*%s\\}*", foundArg),
                    args.get(foundArg));
            }
        }
        return resolvedImageString;
    }

    static Set<String> findAllArgs(String imageTagString) {
        Matcher m = argPattern.matcher(imageTagString);
        Set<String> args = new HashSet<>();
        while(m.find()){
            if(m.group(1)!=null){
                args.add(m.group(1));
            }else if(m.group(2)!=null){
                args.add(m.group(2));
            }
        }
        return args;
    }

    public static JsonObject readDockerConfig() {
        String dockerConfig = System.getenv("DOCKER_CONFIG");

        Reader reader = dockerConfig == null
                ? getFileReaderFromDir(new File(getHomeDir(),".docker/config.json"))
                : getFileReaderFromDir(new File(dockerConfig,"config.json"));
        return reader != null ? new Gson().fromJson(reader, JsonObject.class) : null;
    }

    public static boolean isSimpleDockerFileMode(File projectBaseDirectory) {
        if (projectBaseDirectory != null) {
            return getTopLevelDockerfile(projectBaseDirectory).exists();
        }
        return false;
    }

    public static File getTopLevelDockerfile(File projectBaseDirectory) {
        return new File(projectBaseDirectory, "Dockerfile");
    }

    public static ImageConfiguration createSimpleDockerfileConfig(File dockerFile, String defaultImageName) {
        if (defaultImageName == null) {
            // Default name group/artifact:version (or 'latest' if SNAPSHOT)
            defaultImageName = "%g/%a:%l";
        }

        final BuildConfiguration buildConfig = BuildConfiguration.builder()
                .dockerFile(dockerFile.getPath())
                .ports(extractPorts(dockerFile))
                .build();

        return ImageConfiguration.builder()
                .name(defaultImageName)
                .build(buildConfig)
                .build();
    }

    public static ImageConfiguration addSimpleDockerfileConfig(ImageConfiguration image, File dockerfile) {
        final BuildConfiguration buildConfig = BuildConfiguration.builder()
                .dockerFile(dockerfile.getPath())
                .ports(extractPorts(dockerfile))
                .build();
        return image.toBuilder().build(buildConfig).build();
    }

    private static File getHomeDir() {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            homeDir = System.getenv("HOME");
        }
        return new File(homeDir);
    }

    private static void updateMapWithArgValue(Map<String, String> result, Map<String, String> args, String argString) {
        if (argString.contains("=") || argString.contains(":")) {
            String[] argStringParts = argString.split("[=:]");
            String argStringKey = argStringParts[0];
            String argStringValue = determineFinalArgValue(argString, argStringParts, args);
            if (argStringValue.startsWith("\"") || argStringValue.startsWith("'")) {
                // Replaces surrounding quotes
                argStringValue = argStringValue.replaceAll("(^[\"'])|([\"']$)", "");
            } else {
                validateArgValue(argStringValue);
            }
            result.put(argStringKey, argStringValue);
        } else {
            validateArgValue(argString);
            result.putAll(fetchArgsFromBuildConfiguration(argString, args));
        }
    }

    private static String determineFinalArgValue(String argString, String[] argStringParts, Map<String, String> args) {
        String argStringValue = argString.substring(argStringParts[0].length() + 1);
        if(args == null || args.get(argStringParts[0]) == null){
            return argStringValue;
        }
        return args.getOrDefault(argStringParts[0], argStringValue);
    }

    private static Map<String, String> fetchArgsFromBuildConfiguration(String argString, Map<String, String> args) {
        Map<String, String> argFromBuildConfig = new HashMap<>();
        if (args != null) {
            argFromBuildConfig.put(argString, args.getOrDefault(argString, ""));
        }
        return argFromBuildConfig;
    }

    private static void validateArgValue(String argStringParam) {
        String[] argStringParts = argStringParam.split("\\s+");
        if (argStringParts.length > 1) {
            throw new IllegalArgumentException("Dockerfile parse error: ARG requires exactly one argument. Provided : " + argStringParam);
        }
    }

    static List<String> extractPorts(File dockerFile) {
        Properties properties = new Properties();
        try {
            return extractPorts(extractLines(dockerFile, "EXPOSE", properties, null));
        } catch (IOException ioException) {
            throw new IllegalArgumentException("Error in reading Dockerfile", ioException);
        }
    }

    static List<String> extractPorts(List<String[]> dockerLinesContainingExpose) {
        Set<String> ports = new HashSet<>();
        dockerLinesContainingExpose.forEach(line -> Arrays.stream(line)
                .skip(1)
                .filter(Objects::nonNull)
                .filter(StringUtils::isNotBlank)
                .forEach(ports::add));
        return new ArrayList<>(ports);
    }

    static String resolveDockerfileFilter(String filter) {
        return filter != null ? filter : BuildConfiguration.DEFAULT_FILTER;
    }
}
