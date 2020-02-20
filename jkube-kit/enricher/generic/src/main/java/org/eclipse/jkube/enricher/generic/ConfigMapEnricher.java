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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.eclipse.jkube.kit.config.resource.ConfigMapEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonMap;

public class ConfigMapEnricher extends BaseEnricher {

    protected static final String PREFIX_ANNOTATION = "maven.jkube.io/cm/";

    public ConfigMapEnricher(JkubeEnricherContext enricherContext) {
        super(enricherContext, "jkube-configmap-file");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        addAnnotations(builder);
        addConfigMapFromXmlConfigurations(builder);
    }

    private void addAnnotations(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ConfigMapBuilder>() {

            @Override
            public void visit(ConfigMapBuilder element) {
                final Map<String, String> annotations = element.buildMetadata().getAnnotations();
                try {
                    addConfigMapFromAnnotations(annotations, element);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }

    private void addConfigMapFromAnnotations(final Map<String, String> annotations, final ConfigMapBuilder configMapBuilder) throws IOException {
        final Set<Map.Entry<String, String>> entries = annotations.entrySet();
        for (Iterator<Map.Entry<String, String>> it = entries.iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            final String key = entry.getKey();

            if (key.startsWith(PREFIX_ANNOTATION)) {
                addConfigMapEntryFromFile(configMapBuilder, getOutput(key), entry.getValue());
                it.remove();
            }
        }
    }

    private void addConfigMapEntryFromFile(final ConfigMapBuilder configMapBuilder, final String key, final String filePath) throws IOException {
        final byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            final String value = new String(bytes);
            configMapBuilder.addToData(singletonMap(key, value));
        } catch (CharacterCodingException e) {
            final String value = Base64.getEncoder().encodeToString(bytes);
            configMapBuilder.addToBinaryData(singletonMap(key, value));
        }
    }

    private String getOutput(String key) {
        return key.substring(PREFIX_ANNOTATION.length());
    }

    private void addConfigMapFromXmlConfigurations(KubernetesListBuilder builder) {
        org.eclipse.jkube.kit.config.resource.ConfigMap configMap = getConfigMapFromXmlConfiguration();
        try {
            if (configMap == null) {
                return;
            }
            String configMapName = configMap.getName() == null || configMap.getName().trim().isEmpty() ? "xmlconfig" : configMap.getName().trim();
            if (checkIfItemExists(builder, configMapName)) {
                return;
            }

            ConfigMapBuilder configMapBuilder = new ConfigMapBuilder();
            configMapBuilder.withNewMetadata().withName(configMapName).endMetadata();

            for (ConfigMapEntry configMapEntry : configMap.getEntries()) {
                String name = configMapEntry.getName();
                final String value = configMapEntry.getValue();
                if (name != null && value != null) {
                    configMapBuilder.addToData(name, value);
                } else {
                    final String file = configMapEntry.getFile();
                    if (file != null) {
                        if (name == null) {
                            name = Paths.get(file).getFileName().toString();
                        }
                        addConfigMapEntryFromFile(configMapBuilder, name, file);
                    }
                }
            }

            if (!configMapBuilder.getData().isEmpty() || !configMapBuilder.getBinaryData().isEmpty()) {
                builder.addToConfigMapItems(configMapBuilder.build());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean checkIfItemExists(KubernetesListBuilder builder, String name) {
        return builder.buildItems().stream().filter(item -> item.getKind().equals("ConfigMap")).anyMatch(item -> item.getMetadata().getName().equals(name));
    }

    private org.eclipse.jkube.kit.config.resource.ConfigMap getConfigMapFromXmlConfiguration() {
        ResourceConfig resourceConfig = getConfiguration().getResource().orElse(null);
        if (resourceConfig != null && resourceConfig.getConfigMap() != null) {
            return resourceConfig.getConfigMap();
        }
        return null;
    }

}