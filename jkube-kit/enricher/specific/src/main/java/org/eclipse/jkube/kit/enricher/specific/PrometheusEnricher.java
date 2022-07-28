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
package org.eclipse.jkube.kit.enricher.specific;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class PrometheusEnricher extends BaseEnricher {

    private static final String ANNOTATION_PROMETHEUS_PORT = "prometheus.io/port";
    private static final String ANNOTATION_PROMETHEUS_SCRAPE = "prometheus.io/scrape";
    private static final String ANNOTATION_PROMETHEUS_PATH = "prometheus.io/path";

    private static final String ENRICHER_NAME = "jkube-prometheus";
    private static final String PROMETHEUS_PORT = "9779";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        PROMETHEUS_PORT("prometheusPort", null),
        PROMETHEUS_PATH("prometheusPath", "/metrics");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public PrometheusEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                String prometheusPort = findPrometheusPort();
                if (StringUtils.isNotBlank(prometheusPort)) {
                    final Map<String, String> annotations = new HashMap<>();
                    annotations.put(ANNOTATION_PROMETHEUS_PORT, prometheusPort);
                    annotations.put(ANNOTATION_PROMETHEUS_SCRAPE, "true");
                    annotations.put(ANNOTATION_PROMETHEUS_PATH, getConfig(Config.PROMETHEUS_PATH));
                    SummaryUtil.addToEnrichers(getName());
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
        String prometheusPort = getConfig(Config.PROMETHEUS_PORT);
        if (StringUtils.isBlank(prometheusPort)) {
            for (ImageConfiguration configuration : getImages()) {
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
