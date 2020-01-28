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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JkubeDockerfileInterpolator {
    private static Map<String, String> delimiters;
    static {
        delimiters = new HashMap<>();
        delimiters.put("@", "@");
        delimiters.put("${", "}");
    }

    private JkubeDockerfileInterpolator() { }

    public static String interpolate(String line, Properties properties) {
        for (String property : properties.stringPropertyNames()) {
            String value = checkPropertyWithDelimiters(line, property, properties);
            if (value != null) {
                line = value;
            }
        }
        return line;
    }

    private static String checkPropertyWithDelimiters(String line, String property, Properties properties) {
        for (Map.Entry<String, String> delimiter : delimiters.entrySet()) {
            String searchPhrase = delimiter.getKey() + property + delimiter.getValue();
            if (line.contains(searchPhrase)) {
                return line.replace(searchPhrase, properties.getProperty(property));
            }
        }
        return null;
    }
}
