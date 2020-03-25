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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility for resource file handling
 *
 * @author roland
 * @since 07/02/17
 */
public class ResourceUtil {

    public static boolean jsonEquals(JsonObject first, JsonObject second) {
        final ObjectMapper mapper = new ObjectMapper();

        try {
            final JsonNode tree1 = mapper.readTree(first.toString());
            final JsonNode tree2 = mapper.readTree(second.toString());
            return tree1.equals(tree2);
        } catch (IOException e) {
            return false;
        }
    }

    public static <T> List<T> loadList(File file, Class<T> clazz) throws IOException {
        return getObjectMapper(ResourceFileType.fromFile(file)).readerFor(clazz).<T>readValues(file).readAll();
    }

    public static <T> T load(File file, Class<T> clazz) throws IOException {
        ResourceFileType type = ResourceFileType.fromFile(file);
        return load(file, clazz, type);
    }

    public static <T> T load(File file, Class<T> clazz, ResourceFileType resourceFileType) throws IOException {
        return getObjectMapper(resourceFileType).readValue(file, clazz);
    }

    public static <T> T load(InputStream in, Class<T> clazz, ResourceFileType resourceFileType) throws IOException {
        return getObjectMapper(resourceFileType).readValue(in, clazz);
    }

    public static File save(File file, Object data) throws IOException {
        return save(file, data, ResourceFileType.fromFile(file));
    }

    public static File save(File file, Object data, ResourceFileType type) throws IOException {
        boolean hasExtension = FilenameUtils.indexOfExtension(file.getAbsolutePath()) != -1;
        File output = hasExtension ? file : type.addExtensionIfMissing(file);
        ensureDir(file);
        getObjectMapper(type).writeValue(output, data);
        return output;
    }


    public static String toYaml(Object resource) throws JsonProcessingException {
        return serializeAsString(resource, ResourceFileType.yaml);
    }

    public static String toJson(Object resource) throws JsonProcessingException {
        return serializeAsString(resource, ResourceFileType.json);
    }

    private static String serializeAsString(Object resource, ResourceFileType resourceFileType) throws JsonProcessingException {
        return getObjectMapper(resourceFileType).writeValueAsString(resource);
    }

    private static ObjectMapper getObjectMapper(ResourceFileType resourceFileType) {
        return resourceFileType.getObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    }

    private static void ensureDir(File file) throws IOException {
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Cannot create directory " + parentDir);
            }
        }
    }

    public static File getFinalResourceDir(File resourceDir, String environment) {
        if (resourceDir != null && StringUtils.isNotEmpty(environment)) {
            return new File(resourceDir, environment);
        }

        return resourceDir;
    }


}

