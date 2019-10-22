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
package io.jkube.maven.enricher.specific;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Configs;
import io.jkube.kit.common.util.MapUtil;
import io.jkube.kit.config.image.build.BuildConfiguration;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import org.apache.commons.lang3.StringUtils;


public class PrometheusEnricher extends BaseEnricher {

    static final String ANNOTATION_PROMETHEUS_PORT = "prometheus.io/port";
    static final String ANNOTATION_PROMETHEUS_SCRAPE = "prometheus.io/scrape";

    static final String ENRICHER_NAME = "jkube-prometheus";
    static final String PROMETHEUS_PORT = "9779";

    private enum Config implements Configs.Key {
        prometheusPort;

        public String def() { return d; } protected String d;
    }

    public PrometheusEnricher(MavenEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                String prometheusPort = findPrometheusPort();
                if (StringUtils.isNotBlank(prometheusPort)) {
                    log.verbose("Add prometheus.io annotations: %s=%s, %s=%s",
                            ANNOTATION_PROMETHEUS_SCRAPE, "true",
                            ANNOTATION_PROMETHEUS_PORT, prometheusPort);

                    Map<String, String> annotations = new HashMap<>();
                    MapUtil.putIfAbsent(annotations, ANNOTATION_PROMETHEUS_PORT, prometheusPort);
                    MapUtil.putIfAbsent(annotations, ANNOTATION_PROMETHEUS_SCRAPE, "true");
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
