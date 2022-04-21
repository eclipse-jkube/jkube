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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jkube.kit.common.ResourceFileType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HasMetadataComparator;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility for resource file handling
 *
 * @author roland
 */
public class ResourceUtil {

    static {
        Serialization.UNMATCHED_FIELD_TYPE_MODULE.setRestrictToTemplates(false);
        Serialization.UNMATCHED_FIELD_TYPE_MODULE.setLogWarnings(false);
        for (ObjectMapper mapper : new ObjectMapper[]{Serialization.jsonMapper(), Serialization.yamlMapper()}) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        ((YAMLFactory)Serialization.yamlMapper().getFactory())
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
            .configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true);
    }

    private ResourceUtil() {}

    /**
     * Parse the provided file and return a List of HasMetadata resources.
     *
     * <p> If the provided resource is KubernetesList, the individual list items will be returned.
     *
     * <p> If the provided resource is a Template, the individual objects will be returned with the placeholders
     * replaced.
     *
     * <p> n.b. the returned list will be sorted using the HasMetadataComparator.
     *
     * @param manifest the File to parse.
     * @return a List of HasMetadata resources.
     * @throws IOException if there's a problem while performing IO operations on the provided File.
     */
    public static List<HasMetadata> deserializeKubernetesListOrTemplate(File manifest) throws IOException {
        if (!manifest.isFile() || !manifest.exists()) {
            return Collections.emptyList();
        }
        final List<HasMetadata> kubernetesResources = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(manifest.toPath())) {
            kubernetesResources.addAll(split(Serialization.unmarshal(fis, Collections.emptyMap())));
        }
        kubernetesResources.sort(new HasMetadataComparator());
        return kubernetesResources;
    }

    private static List<HasMetadata> split(Object resource) throws IOException {
        if (resource instanceof Collection) {
            final List<HasMetadata> collectionItems = new ArrayList<>();
            for (Object item : ((Collection<?>)resource)) {
                collectionItems.addAll(split(item));
            }
            return collectionItems;
        } else if (resource instanceof KubernetesList) {
            return ((KubernetesList) resource).getItems();
        } else if (resource instanceof Template) {
            return Optional.ofNullable(
                    OpenshiftHelper.processTemplatesLocally((Template) resource, false))
                .map(KubernetesList::getItems)
                .orElse(Collections.emptyList());
        } else if (resource instanceof HasMetadata) {
            return Collections.singletonList((HasMetadata) resource);
        }
        return Collections.emptyList();
    }

    public static <T extends KubernetesResource> T load(File file, Class<T> clazz) throws IOException {
        return Serialization.unmarshal(Files.newInputStream(file.toPath()), clazz);
    }

    public static File save(File file, Object data) throws IOException {
        return save(file, data, ResourceFileType.fromFile(file));
    }

    public static File save(File file, Object data, ResourceFileType type) throws IOException {
        boolean hasExtension = FilenameUtils.indexOfExtension(file.getAbsolutePath()) != -1;
        File output = hasExtension ? file : type.addExtensionIfMissing(file);
        FileUtil.createDirectory(file.getParentFile());
        getObjectMapper(type).writeValue(output, data);
        return output;
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

    public static List<File> getFinalResourceDirs(File resourceDir, String environmentAsCommaSeparateStr) {
        List<File> resourceDirs = new ArrayList<>();

        if (resourceDir != null && StringUtils.isNotBlank(environmentAsCommaSeparateStr)) {
            String[] environments = environmentAsCommaSeparateStr.split(",");
            for (String environment : environments) {
                resourceDirs.add(new File(resourceDir, environment.trim()));
            }
        } else if (StringUtils.isBlank(environmentAsCommaSeparateStr)) {
            resourceDirs.add(resourceDir);
        }
        return resourceDirs;
    }


}

