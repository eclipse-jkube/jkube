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
package org.eclipse.jkube.kit.build.service.docker.helper;

import com.google.common.base.Strings;
import org.eclipse.jkube.kit.common.JavaProject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Replace placeholders in an image name with certain properties found in the
 * project
 *
 * @author roland
 * @since 07/06/16
 */
public class ImageNameFormatter implements ConfigHelper.NameFormatter {


    private final FormatParameterReplacer formatParamReplacer;

    private final Date now;

    public ImageNameFormatter(JavaProject project, Date now) {
        this.now = now;
        formatParamReplacer = new FormatParameterReplacer(initLookups(project));
    }

    @Override
    public String format(String name) {
        if (name == null) {
            return null;
        }

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

    public static abstract class AbstractLookup implements FormatParameterReplacer.Lookup {
        protected final JavaProject project;

        private AbstractLookup(JavaProject project) {
            this.project = project;
        }

        protected String getProperty(String key) {
            return project.getProperties().getProperty(key);
        }
    }


    private static class DefaultUserLookup extends AbstractLookup {

        /**
         * Property to lookup for image user which overwrites the calculated default (group).
         * Used with format modifier %g
         */
        private static final String DOCKER_IMAGE_USER = "docker.image.user";

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
        private static final String DOCKER_IMAGE_TAG = "docker.image.tag";

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
            String tag = getProperty(DOCKER_IMAGE_TAG);
            if (!Strings.isNullOrEmpty(tag)) {
                return tag;
            }

            tag = project.getVersion();
            if (mode != Mode.PLAIN) {
                if (tag.endsWith("-SNAPSHOT")) {
                    if (mode == Mode.SNAPSHOT_WITH_TIMESTAMP) {
                        tag = "snapshot-" + new SimpleDateFormat("yyMMdd-HHmmss-SSSS").format(now);
                    } else if (mode == Mode.SNAPSHOT_LATEST) {
                        tag = "latest";
                    }
                }
            }
            return tag;
        }
    }

    // ==========================================================================================

    // See also ImageConfiguration#doValidate()
    private static String sanitizeName(String name) {
        StringBuilder ret = new StringBuilder();
        int underscores = 0;
        boolean lastWasADot = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                underscores++;
                // Only _ in a row are allowed
                if (underscores <= 2) {
                    ret.append(c);
                }
                continue;
            }

            if (c == '.') {
                // Only one dot in a row is allowed
                if (!lastWasADot) {
                    ret.append(c);
                }
                lastWasADot = true;
                continue;
            }

            underscores = 0;
            lastWasADot = false;
            if (Character.isLetter(c) || Character.isDigit(c) || c == '-') {
                ret.append(c);
            }
        }

        // All characters must be lowercase
        return ret.toString().toLowerCase();
    }
}
