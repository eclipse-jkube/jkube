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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for dealing with dockerfiles
 * @author roland
 * @since 21/01/16
 */
public class DockerFileUtil {

    private DockerFileUtil() {}

    /**
     * Extract the base images from a dockerfile. All lines containing a <code>FROM</code> is
     * taken.
     *
     * @param dockerFile file from where to extract the base image.
     * @param properties properties to interpolate in the provided dockerFile.
     * @return LinkedList of base images name or empty collection if none is found.
     * @throws IOException if there's a problem while performin IO operations.
     */
    public static List<String> extractBaseImages(File dockerFile, Properties properties, String filter) throws IOException {
        List<String[]> fromLines = extractLines(dockerFile, "FROM", properties, filter);
        Set<String> result = new LinkedHashSet<>();
        Set<String> fromAlias = new HashSet<>();
        for (String[] fromLine :  fromLines) {
            if (fromLine.length > 1) {
                if (fromLine.length == 2) { // FROM image:tag use case
                    result.add(fromLine[1]);
                } else if (fromLine.length == 4) { // FROM image:tag AS alias use case
                    if (!fromAlias.contains(fromLine[1])) {
                        result.add(fromLine[1]);
                    }
                    fromAlias.add(fromLine[3]);
                }
            }
        }
        return new ArrayList<>(result);
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
                String lineInterpolated = JKubeDockerfileInterpolator.interpolate(line, properties, filter);
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
     * @throws IOException if there's a problem while performin IO operations.
     */
    public static String interpolate(File dockerFile, Properties properties, String filter) throws IOException {
        StringBuilder ret = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(dockerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ret.append(JKubeDockerfileInterpolator.interpolate(line, properties, filter)).append(System.lineSeparator());
            }
        }
        return ret.toString();
    }

    private static Reader getFileReaderFromDir(File file) {
        if (file.exists() && file.length() != 0) {
            try {
                return new FileReader(file);
            } catch (FileNotFoundException e) {
                // Shouldnt happen. Nevertheless ...
                throw new IllegalStateException("Cannot find " + file,e);
            }
        } else {
            return null;
        }
    }

    public static JsonObject readDockerConfig() {
        String dockerConfig = System.getenv("DOCKER_CONFIG");

        Reader reader = dockerConfig == null
                ? getFileReaderFromDir(new File(getHomeDir(),".docker/config.json"))
                : getFileReaderFromDir(new File(dockerConfig,"config.json"));
        return reader != null ? new Gson().fromJson(reader, JsonObject.class) : null;
    }

    public static String[] extractDelimiters(String filter) {
        if (filter == null ||
                filter.equalsIgnoreCase("false") ||
                filter.equalsIgnoreCase("none")) {
            return new String[0];
        }
        if (filter.contains("*")) {
            Matcher matcher = Pattern.compile("^(?<start>[^*]+)\\*(?<end>.*)$").matcher(filter);
            if (matcher.matches()) {
                return new String[] { matcher.group("start"), matcher.group("end") };
            }
        }
        return new String[] { filter, filter };
    }

    public static Map<String,?> readKubeConfig() {
        String kubeConfig = System.getenv("KUBECONFIG");

        Reader reader = kubeConfig == null
                ? getFileReaderFromDir(new File(getHomeDir(),".kube/config"))
                : getFileReaderFromDir(new File(kubeConfig));
        if (reader != null) {
            Yaml ret = new Yaml();
            return (Map<String, ?>) ret.load(reader);
        }
        return null;
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
                .build();

        return ImageConfiguration.builder()
                .name(defaultImageName)
                .build(buildConfig)
                .build();
    }

    public static ImageConfiguration addSimpleDockerfileConfig(ImageConfiguration image, File dockerfile) {
        final BuildConfiguration buildConfig = BuildConfiguration.builder()
                .dockerFile(dockerfile.getPath())
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

}
