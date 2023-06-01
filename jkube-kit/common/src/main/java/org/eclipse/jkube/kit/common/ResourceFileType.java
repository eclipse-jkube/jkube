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
package org.eclipse.jkube.kit.common;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.Serialization;

/**
 * Type of resources supported
 *
 * @author roland
 * @since 07/04/16
 */
public enum ResourceFileType {

    json("json","json", Serialization::saveJson),

    yaml("yml","yml", Serialization::saveYaml);

    private final String extension;
    private final String artifactType;
    private final Serializer serializer;

    ResourceFileType(String extension, String artifactType, Serializer serializer) {
        this.extension = extension;
        this.artifactType = artifactType;
        this.serializer = serializer;
    }

    public File addExtensionIfMissing(File file) {
        String path = file.getAbsolutePath();
        if (!path.endsWith("." + extension)) {
            return new File(path + "." + extension);
        } else {
            return file;
        }
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void serialize(File file, Object object) throws IOException {
        FileUtil.createDirectory(file.getParentFile());
        serializer.serialize(file, object);
    }

    public static ResourceFileType fromExtension(String ext) {
        try {
            return ResourceFileType.valueOf(ext);
        } catch (IllegalArgumentException exp) {
            // Try extensions, too:
            for (ResourceFileType type : ResourceFileType.values()) {
                if (type.extension.equals(ext)) {
                    return type;
                }
            }
            throw exp;
        }
    }

    public static ResourceFileType fromFile(File file) {
        String ext = FilenameUtils.getExtension(file.getPath());
        if (StringUtils.isNotBlank(ext)) {
            return fromExtension(ext);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported extension '%s' for file %s. Must be one of %s", ext, file, Arrays.asList(values())));
        }
    }

    @FunctionalInterface
    public interface Serializer {
        void serialize(File file, Object object) throws IOException;
    }
}

