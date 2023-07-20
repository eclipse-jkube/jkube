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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class KindFilenameMapperUtil {

    private KindFilenameMapperUtil() {}

    public static Map<String, List<String>> loadMappings() {

        final String location = "/META-INF/jkube/kind-filename-type-mapping-default.adoc";
        final String locationMappingProperties =
                EnvUtil.getEnvVarOrSystemProperty("jkube.mapping", "/META-INF/jkube/kind-filename-type-mapping-default.properties");

        try (
          final InputStream mappingFile = loadContent(location);
          final InputStream mappingPropertiesFile = loadContent(locationMappingProperties)
        ) {
            final AsciiDocParser asciiDocParser = new AsciiDocParser();
            final Map<String, List<String>> defaultMapping = asciiDocParser.serializeKindFilenameTable(mappingFile);

            if (mappingPropertiesFile != null) {
                PropertiesMappingParser propertiesMappingParser = new PropertiesMappingParser();
                defaultMapping.putAll(propertiesMappingParser.parse(mappingPropertiesFile));
            }

            return defaultMapping;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InputStream loadContent(String location) {
        InputStream resourceAsStream = KindFilenameMapperUtil.class.getResourceAsStream(location);

        if (resourceAsStream == null) {
            final File locationFile = new File(location);

            try {
                return new FileInputStream(locationFile);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        return resourceAsStream;
    }

}

