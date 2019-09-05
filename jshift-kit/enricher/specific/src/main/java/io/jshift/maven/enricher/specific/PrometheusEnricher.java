/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.maven.enricher.specific;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.common.Configs;
import io.jshift.kit.common.util.MapUtil;
import io.jshift.kit.config.image.build.BuildConfiguration;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import org.apache.commons.lang3.StringUtils;


public class PrometheusEnricher extends BaseEnricher {

    static final String ANNOTATION_PROMETHEUS_PORT = "prometheus.io/port";
    static final String ANNOTATION_PROMETHEUS_SCRAPE = "prometheus.io/scrape";

    static final String ENRICHER_NAME = "jshift-prometheus";
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
