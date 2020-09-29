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

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.config.resource.ConfigMapEntry;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class ConfigMapEnricher extends BaseEnricher {

    protected static final String PREFIX_ANNOTATION = "maven.jkube.io/cm/";

    public ConfigMapEnricher(JKubeEnricherContext enricherContext) {
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

            if (key.startsWith(PREFIX_ANNOTATION)) {
                addConfigMapEntryFromDirOrFile(configMapBuilder, getOutput(key), entry.getValue());
                it.remove();
            }
        }
    }

    private void addConfigMapEntryFromDirOrFile(final ConfigMapBuilder configMapBuilder, final String key,
            final String dirOrFilePath) throws IOException {
        final Path path = Paths.get(dirOrFilePath);

        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> files = Files.list(path)) {
                files.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).forEach(file -> {
                    try {
                        addConfigMapEntryFromFile(configMapBuilder, file.getFileName().toString(), file);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                });
            }
        } else {
            addConfigMapEntryFromFile(configMapBuilder, key, path);
        }
    }

    private void addConfigMapEntryFromFile(final ConfigMapBuilder configMapBuilder, final String key, final Path file)
            throws IOException {
        final byte[] bytes = Files.readAllBytes(file);
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
            String configMapName = configMap.getName() == null || configMap.getName().trim().isEmpty() ? "xmlconfig"
                    : configMap.getName().trim();
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
                        addConfigMapEntryFromDirOrFile(configMapBuilder, name, file);
                    }
                }
            }

            if (configMapBuilder.getData() != null && !configMapBuilder.getData().isEmpty()
                    || configMapBuilder.getBinaryData() != null && !configMapBuilder.getBinaryData().isEmpty()) {
                builder.addToConfigMapItems(configMapBuilder.build());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
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
