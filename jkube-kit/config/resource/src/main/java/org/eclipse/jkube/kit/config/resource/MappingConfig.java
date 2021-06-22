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
package org.eclipse.jkube.kit.config.resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingConfig {

    private String kind;

    private String filenameTypes;

    public String getKind() {
        return kind;
    }

    public String getFilenameTypes() {
        return filenameTypes;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setFilenameTypes(String filenameTypes) {
        this.filenameTypes = filenameTypes;
    }

    public String[] getFilenamesAsArray() {
        if (this.filenameTypes == null) {
            return new String[0];
        }
        return filenameTypes.split(",\\s*");
    }

    public boolean isValid() {
        return kind != null &&  filenameTypes != null && filenameTypes.length() > 0;
    }

    public static Map<String, List<String>> getKindFilenameMappings(List<MappingConfig> mappings) {
        final Map<String, List<String>> mappingKindFilename = new HashMap<>();
        if (mappings != null) {
            for (MappingConfig mappingConfig : mappings) {
                if (mappingConfig.isValid()) {
                    mappingKindFilename.put(mappingConfig.getKind(), Arrays.asList(mappingConfig.getFilenamesAsArray()));
                } else {
                    throw new IllegalArgumentException(String.format("Invalid mapping for Kind %s and Filename Types %s",
                            mappingConfig.getKind(), mappingConfig.getFilenameTypes()));
                }
            }
        }
        return mappingKindFilename;
    }
}
