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
package org.eclipse.jkube.kit.build.service.docker.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.jkube.kit.common.JKubeFileInterpolator.DEFAULT_FILTER;
import static org.eclipse.jkube.kit.common.JKubeFileInterpolator.interpolate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.JavaProject;

/**
 * Replace placeholders in an image name with certain properties found in the
 * project
 *
 * @author roland
 * @since 07/06/16
 */
public class ImageNameFormatter implements ConfigHelper.NameFormatter {

    /**
     * Property to lookup for image user which overwrites the calculated default (group).
     * Used with format modifier %g
     */
    public static final String DOCKER_IMAGE_USER = "jkube.image.user";
    /**
     * Property to lookup the replacement symbol for SemVer's '+', which is not allowed
     * in the tag component of a complete container name.
     */
    public static final String SEMVER_PLUS_SUBSTITUTION = "jkube.image.tag.semver_plus_substitution";
    /**
     * Default value when {@link ImageNameFormatter#SEMVER_PLUS_SUBSTITUTION} is undefined.
     */
    public static final String DEFAULT_SEMVER_PLUS_SUBSTITUTE = "-";


    private final FormatParameterReplacer formatParamReplacer;

    private final Date now;
    private final JavaProject project;

    public ImageNameFormatter(JavaProject project, Date now) {
        this.now = now;
        this.project = project;
        formatParamReplacer = new FormatParameterReplacer(initLookups(project));
    }

    @Override
    public String format(String name) {
        if (name == null) {
            return null;
        }

        name = interpolate(name, project.getProperties(), DEFAULT_FILTER);
        return formatParamReplacer.replace(name);
    }

    // =====================================================================================


    // Lookup classes
    private Map<String, FormatParameterReplacer.Lookup> initLookups(final JavaProject project) {
        // Sanitized group id
        final Map<String, FormatParameterReplacer.Lookup> lookups = new HashMap<>();

        lookups.put("g", new DefaultUserLookup(project));

        // Sanitized artifact id
        lookups.put("a", new DefaultNameLookup(project));

        // Various ways for adding a version
        lookups.put("v", new DefaultTagLookup(project, DefaultTagLookup.Mode.PLAIN, now));
        lookups.put("t", new DefaultTagLookup(project, DefaultTagLookup.Mode.SNAPSHOT_WITH_TIMESTAMP, now));
        lookups.put("l", new DefaultTagLookup(project, DefaultTagLookup.Mode.SNAPSHOT_LATEST, now));
        return lookups;
    }

    // ==============================================================================================

    public abstract static class AbstractLookup implements FormatParameterReplacer.Lookup {
        protected final JavaProject project;

        private AbstractLookup(JavaProject project) {
            this.project = project;
        }

        protected String getProperty(String key) {
            return project.getProperties().getProperty(key);
        }

        protected String getProperty(String key, String defaultValue) {
            return project.getProperties().getProperty(key, defaultValue);
        }
    }


    private static class DefaultUserLookup extends AbstractLookup {

        private DefaultUserLookup(JavaProject project) {
            super(project);
        }

        public String lookup() {
            String user = getProperty(DOCKER_IMAGE_USER);
            if (user != null) {
                return user;
            }
            String groupId = project.getGroupId();
            while (groupId.endsWith(".")) {
                groupId = groupId.substring(0,groupId.length() - 1);
            }
            int idx = groupId.lastIndexOf(".");
            return sanitizeName(groupId.substring(idx != -1 ? idx + 1 : 0));
        }
    }

    private static class DefaultNameLookup extends AbstractLookup {

        private DefaultNameLookup(JavaProject project) {
            super(project);
        }

        public String lookup() {
            return sanitizeName(project.getArtifactId());
        }
    }


    private static class DefaultTagLookup extends AbstractLookup {

        /**
         * Property to lookup for image name which overwrites the calculated default, which is calculated
         * on the project version and depends whether it is a snapshot project or not.
         * Used with format modifier %v
         */
        private static final String DOCKER_IMAGE_TAG = "jkube.image.tag";

        // how to resolve the version
        private final Mode mode;

        // timestamp indicating now
        private final Date now;

        private enum Mode {
            PLAIN,
            SNAPSHOT_WITH_TIMESTAMP,
            SNAPSHOT_LATEST
        }

        private DefaultTagLookup(JavaProject project, Mode mode, Date now) {
            super(project);
            this.mode = mode;
            this.now = now;
        }

        public String lookup() {
            final String userProvidedTag = getProperty(DOCKER_IMAGE_TAG);
            if (!StringUtils.isBlank(userProvidedTag)) {
                return userProvidedTag;
            }

            String plusSubstitute = getProperty(SEMVER_PLUS_SUBSTITUTION, "-").trim();
            if ("+".equals(plusSubstitute)) {
                plusSubstitute = DEFAULT_SEMVER_PLUS_SUBSTITUTE;
            }

            String tag = generateTag(plusSubstitute);
            return sanitizeTag(tag, plusSubstitute);
        }

        private String generateTag(String plusSubstitute) {
            final String version = project.getVersion();
            if (mode == Mode.PLAIN) {
                return version;
            }

            final String prerelease;
            final String buildmetadata;

            final int indexOfPlus = version.indexOf('+');
            if (indexOfPlus >= 0) {
                prerelease = version.substring(0, indexOfPlus);
                buildmetadata = plusSubstitute + version.substring(indexOfPlus + 1); // '+' is not allowed in a container tag
            } else {
                prerelease = version;
                buildmetadata = "";
            }

            if (!prerelease.endsWith("-SNAPSHOT")) {
                return version;
            }

            switch (mode) {
            case SNAPSHOT_WITH_TIMESTAMP:
                return "snapshot-" + new SimpleDateFormat("yyMMdd-HHmmss-SSSS").format(now) + buildmetadata;
            case SNAPSHOT_LATEST:
                return "latest" + buildmetadata;
            default:
                throw new IllegalStateException("mode is '" + mode.name() + "', which is not implemented.");
            }
        }

        private static String sanitizeTag(String tagName, String plusSubstitute) {
            StringBuilder ret = new StringBuilder(tagName.length());

            for (char c : tagName.toCharArray()) {
                final boolean allowedCharacter = Character.isLetterOrDigit(c) || '_' == c || '.' == c || '-' == c; // matches '\w'
                if (allowedCharacter) {
                    ret.append(c);
                } else if ('+' == c) {
                    ret.append(plusSubstitute);
                } else {
                    ret.append('-');
                }
            }

            return ret.length() <= 127 ? ret.toString() : ret.substring(0, 128);
        }
    }

    // ==========================================================================================

    // See also ImageName#doValidate()
    private static String sanitizeName(String name) {
        StringBuilder ret = new StringBuilder(name.length());
        int underscores = 0;
        boolean lastWasADot = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                underscores++;
                // Only _ in a row are allowed
                if (underscores <= 2) {
                    ret.append(c);
                }
            } else if (c == '.') {
                // Only one dot in a row is allowed
                if (!lastWasADot) {
                    ret.append(c);
                }
                lastWasADot = true;
            } else {
                underscores = 0;
                lastWasADot = false;
                if (Character.isLetter(c) || Character.isDigit(c) || c == '-') {
                    ret.append(c);
                }
            }

        }

        // All characters must be lowercase
        return ret.toString().toLowerCase();
    }
}
