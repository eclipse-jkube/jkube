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
package org.eclipse.jkube.maven.enricher.specific;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.apache.commons.lang3.StringUtils;


public class PrometheusEnricher extends BaseEnricher {

    static final String ANNOTATION_PROMETHEUS_PORT = "prometheus.io/port";
    static final String ANNOTATION_PROMETHEUS_SCRAPE = "prometheus.io/scrape";
    static final String ANNOTATION_PROMETHEUS_PATH = "prometheus.io/path";

    static final String ENRICHER_NAME = "jkube-prometheus";
    static final String PROMETHEUS_PORT = "9779";

    private enum Config implements Configs.Key {
        prometheusPort,
        prometheusPath;

        public String def() { return d; } protected String d;
    }

    public PrometheusEnricher(JkubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                String prometheusPort = findPrometheusPort();
                if (StringUtils.isNotBlank(prometheusPort)) {

                    Map<String, String> annotations = new HashMap<>();
                    MapUtil.putIfAbsent(annotations, ANNOTATION_PROMETHEUS_PORT, prometheusPort);
                    MapUtil.putIfAbsent(annotations, ANNOTATION_PROMETHEUS_SCRAPE, "true");
                    String prometheusPath = getConfig(Config.prometheusPath);
                    if (StringUtils.isNotBlank(prometheusPath)) {
                        MapUtil.putIfAbsent(annotations, ANNOTATION_PROMETHEUS_PATH, prometheusPath);
                    }

                    log.verbose("Adding prometheus.io annotations: %s",
                            annotations.entrySet()
                                    .stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(", ")));
                    serviceBuilder.editMetadata().addToAnnotations(annotations).endMetadata();
                }
            }
        });
    }

    private String findPrometheusPort() {
        String prometheusPort = getConfig(Config.prometheusPort);
        if (StringUtils.isBlank(prometheusPort)) {
            for (ImageConfiguration configuration : getImages().orElse(Collections.emptyList())) {
                BuildConfiguration buildImageConfiguration = configuration.getBuildConfiguration();
                if (buildImageConfiguration != null) {
                    List<String> ports = buildImageConfiguration.getPorts();
                    if (ports != null && ports.contains(PROMETHEUS_PORT)) {
                        prometheusPort = PROMETHEUS_PORT;
                        break;
                    }
                }
            }
        }

        return prometheusPort;
    }
}
