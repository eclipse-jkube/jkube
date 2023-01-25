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
package org.eclipse.jkube.enricher.generic;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.addNewConfigMapEntriesToExistingConfigMap;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.addNewEntryToExistingConfigMap;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.createConfigMapEntry;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jkube.kit.config.resource.ConfigMapEntry;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class ConfigMapEnricher extends BaseEnricher {

    /**
     * @deprecated Use <code>jkube.eclipse.org/cm/</code> prefix instead
     */
    @Deprecated
    protected static final String PREFIX_ANNOTATION = "maven.jkube.io/cm/";

    protected static final String CONFIGMAP_PREFIX_ANNOTATION = INTERNAL_ANNOTATION_PREFIX + "/cm/";

    public ConfigMapEnricher(JKubeEnricherContext enricherContext) {
        super(enricherContext, "jkube-configmap-file");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        addAnnotations(builder);
        addConfigMapFromResourceConfigurations(builder);
    }

    private void addAnnotations(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ConfigMapBuilder>() {

            @Override
            public void visit(ConfigMapBuilder element) {
                final Map<String, String> annotations = element.buildMetadata().getAnnotations();
                if (annotations != null) {
                    try {
                        addConfigMapFromAnnotations(annotations, element);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        });
    }

    private void addConfigMapFromAnnotations(final Map<String, String> annotations, final ConfigMapBuilder configMapBuilder)
            throws IOException {
        final Set<Map.Entry<String, String>> entries = annotations.entrySet();
        for (Iterator<Map.Entry<String, String>> it = entries.iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            final String key = entry.getKey();

            if (key.startsWith(PREFIX_ANNOTATION) || key.startsWith(CONFIGMAP_PREFIX_ANNOTATION)) {
                Path filePath = Paths.get(entry.getValue());
                addNewConfigMapEntriesToExistingConfigMap(configMapBuilder, getOutput(key), filePath);
                it.remove();
            }
        }
    }



    private String getOutput(String key) {
        if (key.startsWith(PREFIX_ANNOTATION)) {
            return key.substring(PREFIX_ANNOTATION.length());
        }
        return key.substring(CONFIGMAP_PREFIX_ANNOTATION.length());
    }

    private void addConfigMapFromResourceConfigurations(KubernetesListBuilder builder) {
        org.eclipse.jkube.kit.config.resource.ConfigMap configMapResourceConfiguration = getConfigMapFromXmlConfiguration();
        try {
            if (configMapResourceConfiguration == null) {
                return;
            }
            String configMapName = configMapResourceConfiguration.getName() == null || configMapResourceConfiguration.getName().trim().isEmpty() ? "jkubeconfig"
                    : configMapResourceConfiguration.getName().trim();
            if (checkIfItemExists(builder, configMapName)) {
                return;
            }

            io.fabric8.kubernetes.api.model.ConfigMap configMap = createConfigMapFromConfiguration(configMapResourceConfiguration, configMapName);

            if ((configMap.getData() != null && !configMap.getData().isEmpty())
                    || (configMap.getBinaryData() != null && !configMap.getBinaryData().isEmpty())) {
                builder.addToConfigMapItems(configMap);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private io.fabric8.kubernetes.api.model.ConfigMap createConfigMapFromConfiguration(org.eclipse.jkube.kit.config.resource.ConfigMap configMap, String configMapName) throws IOException {
        io.fabric8.kubernetes.api.model.ConfigMapBuilder configMapBuilder = new io.fabric8.kubernetes.api.model.ConfigMapBuilder();
        configMapBuilder.withNewMetadata().withName(configMapName).endMetadata();

        for (ConfigMapEntry configMapEntry : configMap.getEntries()) {
            String name = configMapEntry.getName();
            final String value = configMapEntry.getValue();
            if (name != null && value != null) {
                configMapBuilder.addToData(name, value);
            } else {
                final String file = configMapEntry.getFile();
                if (file != null) {
                    final Path filePath = Paths.get(file);
                    if (name == null) {
                        name = filePath.getFileName().toString();
                    }
                    Map.Entry<String, String> fileEntry = createConfigMapEntry(name, filePath);
                    addNewEntryToExistingConfigMap(configMapBuilder, fileEntry, filePath);
                }
            }
        }
        return configMapBuilder.build();
    }

    private boolean checkIfItemExists(KubernetesListBuilder builder, String name) {
        return builder.buildItems().stream()
                .filter(item -> item.getKind().equals("ConfigMap"))
                .anyMatch(item -> item.getMetadata().getName().equals(name));
    }

    private org.eclipse.jkube.kit.config.resource.ConfigMap getConfigMapFromXmlConfiguration() {
        ResourceConfig resourceConfig = getConfiguration().getResource();
        if (resourceConfig != null && resourceConfig.getConfigMap() != null) {
            return resourceConfig.getConfigMap();
        }
        return null;
    }

}
