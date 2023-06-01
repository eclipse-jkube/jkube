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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jkube.kit.common.ResourceFileType;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HasMetadataComparator;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility for resource file handling
 *
 * @author roland
 */
public class ResourceUtil {

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
        final List<HasMetadata> kubernetesResources = new ArrayList<>(split(Serialization.unmarshal(manifest)));
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

    public static File save(File file, Object data) throws IOException {
        return save(file, data, ResourceFileType.fromFile(file));
    }

    public static File save(File file, Object data, ResourceFileType type) throws IOException {
        boolean hasExtension = FilenameUtils.indexOfExtension(file.getAbsolutePath()) != -1;
        File output = hasExtension ? file : type.addExtensionIfMissing(file);
        type.serialize(output, data);
        return output;
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

