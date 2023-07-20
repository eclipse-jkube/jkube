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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PropertiesMappingParser {

    /**
     * This method reads properties file to load custom mapping between kinds and filenames.
     *
     * <pre>
     * ConfigMap=cm, configmap
     * Service=service
     * </pre>
     *
     * @param mapping
     *     stream of a properties file setting mappings between kinds and filenames.
     *
     * @return Serialization of all elements as a map
     */
    public Map<String, List<String>> parse(final InputStream mapping) {

        final Properties mappingProperties = new Properties();
        try {
            mappingProperties.load(mapping);

            final Map<String, List<String>> serializedContent = new HashMap<>();

            final Set<String> kinds = mappingProperties.stringPropertyNames();

            for (String kind : kinds) {
                final String filenames = mappingProperties.getProperty(kind);
                final String[] filenameTypes = filenames.split(",");
                final List<String> scannedFiletypes = new ArrayList<>();
                for (final String filenameType : filenameTypes) {
                    scannedFiletypes.add(filenameType.trim());
                }
                serializedContent.put(kind, scannedFiletypes);
            }

            return serializedContent;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

