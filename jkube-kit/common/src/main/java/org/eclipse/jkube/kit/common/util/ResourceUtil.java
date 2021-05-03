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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonObject;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility for resource file handling
 *
 * @author roland
 */
public class ResourceUtil {

    private ResourceUtil() {}

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

    public static List<HasMetadata> deserializeKubernetesListOrTemplate(File manifest)
        throws IOException {

        List<HasMetadata> kubernetesResources = new ArrayList<>();
        if (manifest.isFile()) {
            String fileContentAsStr = new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(fileContentAsStr)) {
                kubernetesResources.addAll(parseKubernetesListOrTemplate(
                    ResourceFileType.fromFile(manifest).getObjectMapper(), fileContentAsStr
                ));
            }
        }
        return kubernetesResources;
    }

    private static List<HasMetadata> parseKubernetesListOrTemplate(ObjectMapper mapper, String manifestString)
        throws IOException {
        final Map<String, Object> manifest = mapper
            .readValue(manifestString, new TypeReference<Map<String, Object>>() {});
        if (manifest.get("kind").equals(new Template().getKind())) {
            return Optional.ofNullable(OpenshiftHelper.processTemplatesLocally(
                mapper.convertValue(manifest, Template.class), false))
                .map(KubernetesList::getItems)
                .orElse(Collections.emptyList());
        } else {
            return parseKubernetesList(mapper, manifest);
        }
    }

    private static List<HasMetadata> parseKubernetesList(ObjectMapper mapper, Map<String, Object> manifest) {
        final List<Map<String, Object>> items = (List<Map<String, Object>>)manifest.get("items");
        return items.stream().map(item -> {
            final GenericCustomResource fallback = mapper.convertValue(item, GenericCustomResource.class);
            try {
                // Convert Using KubernetesDeserializer or fail and return fallback generic
                return mapper.convertValue(fallback, HasMetadata.class);
            } catch(Exception ex) {
                return fallback;
            }
        })
            .collect(Collectors.toList());
    }

    public static <T extends KubernetesResource> T load(File file, Class<T> clazz) throws IOException {
        ResourceFileType type = ResourceFileType.fromFile(file);
        return load(file, clazz, type);
    }

    private static boolean isGenericCustomResourceCompatible(Class<?> clazz){
        return clazz.isAssignableFrom(GenericCustomResource.class);
    }

    public static <T extends KubernetesResource> T load(File file, Class<T> clazz, ResourceFileType resourceFileType)
        throws IOException {
        try {
            return getObjectMapper(resourceFileType).readValue(file, clazz);
        } catch(IOException ex) {
            if (isGenericCustomResourceCompatible(clazz)) {
                return clazz.cast(getObjectMapper(resourceFileType).readValue(file, GenericCustomResource.class));
            }
            throw ex;
        }
    }

    public static <T extends KubernetesResource> T load(InputStream in, Class<T> clazz, ResourceFileType resourceFileType)
        throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(in, baos);
            return getObjectMapper(resourceFileType).readValue(baos.toByteArray(), clazz);
        } catch(IOException ex) {
            if (isGenericCustomResourceCompatible(clazz)) {
                return clazz.cast(getObjectMapper(resourceFileType).readValue(baos.toByteArray(), GenericCustomResource.class));
            }
            throw ex;
        }
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

