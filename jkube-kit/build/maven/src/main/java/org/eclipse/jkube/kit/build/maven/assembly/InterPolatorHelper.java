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
package org.eclipse.jkube.kit.build.maven.assembly;

import org.eclipse.jkube.kit.build.maven.MavenBuildContext;
import org.apache.maven.plugins.assembly.interpolation.AssemblyInterpolator;
import org.apache.maven.plugins.assembly.io.DefaultAssemblyReader;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterPolatorHelper {
    /**
     * Create an interpolator for the given maven parameters and filter configuration.
     *
     * @param params The maven parameters.
     * @param filter The filter configuration.
     * @return An interpolator for replacing maven properties.
     */
    public static FixedStringSearchInterpolator createInterpolator(MavenBuildContext params, String filter) {
        String[] delimiters = extractDelimiters(filter);
        if (delimiters == null) {
            // Don't interpolate anything
            return FixedStringSearchInterpolator.create();
        }

        DockerAssemblyConfigurationSource configSource = new DockerAssemblyConfigurationSource(params, null, null);
        // Patterned after org.apache.maven.plugins.assembly.interpolation.AssemblyExpressionEvaluator
        return AssemblyInterpolator
                .fullInterpolator(params.getProject(),
                        DefaultAssemblyReader.createProjectInterpolator(params.getProject())
                                .withExpressionMarkers(delimiters[0], delimiters[1]), configSource)
                .withExpressionMarkers(delimiters[0], delimiters[1]);
    }

    public static String[] extractDelimiters(String filter) {
        if (filter == null ||
                filter.equalsIgnoreCase("false") ||
                filter.equalsIgnoreCase("none")) {
            return null;
        }
        if (filter.contains("*")) {
            Matcher matcher = Pattern.compile("^(?<start>[^*]+)\\*(?<end>.*)$").matcher(filter);
            if (matcher.matches()) {
                return new String[] { matcher.group("start"), matcher.group("end") };
            }
        }
        return new String[] { filter, filter };
    }

    /**
     * Interpolate a docker file with the given properties and filter
     *
     * @param dockerFile docker file to interpolate
     * @param interpolator interpolator for replacing properties
     * @return The interpolated contents of the file.
     * @throws IOException
     */
    public static String interpolate(File dockerFile, FixedStringSearchInterpolator interpolator) throws IOException {
        StringBuilder ret = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(dockerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ret.append(interpolator.interpolate(line)).append(System.lineSeparator());
            }
        }
        return ret.toString();
    }

    /**
     * Extract the base images from a dockerfile. All lines containing a <code>FROM</code> is
     * taken.
     *
     * @param dockerFile file from where to extract the base image
     * @param interpolator interpolator for replacing properties
     * @return LinkedList of base images name or empty collection if none is found.
     */
    public static List<String> extractBaseImages(File dockerFile, FixedStringSearchInterpolator interpolator) throws IOException {
        List<String[]> fromLines = extractLines(dockerFile, "FROM", interpolator);
        LinkedList<String> result = new LinkedList<>();
        for (String[] fromLine :  fromLines) {
            if (fromLine.length > 1) {
                result.add(fromLine[1]);
            }
        }
        return result;
    }

    /**
     * Extract all lines containing the given keyword
     *
     * @param dockerFile dockerfile to examine
     * @param keyword keyword to extract the lines for
     * @param interpolator interpolator for replacing properties
     * @return list of matched lines or an empty list
     */
    public static List<String[]> extractLines(File dockerFile, String keyword, FixedStringSearchInterpolator interpolator) throws IOException {
        List<String[]> ret = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dockerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lineInterpolated = interpolator.interpolate(line);
                String[] lineParts = lineInterpolated.split("\\s+");
                if (lineParts.length > 0 && lineParts[0].equalsIgnoreCase(keyword)) {
                    ret.add(lineParts);
                }
            }
        }
        return ret;
    }
}
