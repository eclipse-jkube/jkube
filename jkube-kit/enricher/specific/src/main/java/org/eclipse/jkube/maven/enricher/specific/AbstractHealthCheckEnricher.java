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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.EnricherContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Enriches containers with health check probes.
 */
public abstract class AbstractHealthCheckEnricher extends BaseEnricher {

    public static String ENRICH_CONTAINERS = "jkube.enricher.basic.enrichContainers";

    public static String ENRICH_ALL_CONTAINERS = "jkube.enricher.basic.enrichAllContainers";

    private enum Config implements Configs.Key {
        enrichAllContainers {{ d = "false"; }},
        enrichContainers    {{ d = null; }};

        public String def() { return d; } protected String d;
    }

    public AbstractHealthCheckEnricher(EnricherContext buildContext, String name) {
        super(buildContext, name);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if (checkIfHealthChecksDisabled(false)) {
            return;
        }

        for(ContainerBuilder container : getContainersToEnrich(builder)) {
            if (!container.hasReadinessProbe()) {
                Probe probe = getReadinessProbe(container);
                if (probe != null) {
                    log.info("Adding readiness " + describe(probe));
                    container.withReadinessProbe(probe);
                }
            }

            if (!container.hasLivenessProbe()) {
                Probe probe = getLivenessProbe(container);
                if (probe != null) {
                    log.info("Adding liveness " + describe(probe));
                    container.withLivenessProbe(probe);
                }
            }
        }
    }

    private String describe(Probe probe) {
        StringBuilder desc = new StringBuilder("probe");
        if (probe.getHttpGet() != null) {
            desc.append(" on port ");
            desc.append(probe.getHttpGet().getPort().getIntVal());
            desc.append(", path='");
            desc.append(probe.getHttpGet().getPath());
            desc.append("'");
            desc.append(", scheme='");
            desc.append(probe.getHttpGet().getScheme());
            desc.append("'");
        }
        if (probe.getInitialDelaySeconds() != null) {
            desc.append(", with initial delay ");
            desc.append(probe.getInitialDelaySeconds());
            desc.append(" seconds");
        }
        if (probe.getPeriodSeconds() != null) {
            desc.append(", with period ");
            desc.append(probe.getPeriodSeconds());
            desc.append(" seconds");
        }
        return desc.toString();
    }

    protected Boolean checkIfHealthChecksDisabled(Boolean defaultValue) {
        if (getContext().getProperty("jkube.skipHealthCheck") != null) {
            return Boolean.parseBoolean(getContext().getProperty("jkube.skipHealthCheck").toString());
        } else {
            return defaultValue;
        }
    }

    protected List<ContainerBuilder> getContainersToEnrich(KubernetesListBuilder builder) {
        final List<ContainerBuilder> containerBuilders = new LinkedList<>();
        builder.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                containerBuilders.add(containerBuilder);
            }
        });

        boolean enrichAllContainers = "true".equalsIgnoreCase(getConfig(Config.enrichAllContainers));
        String enrichContainers = getConfig(Config.enrichContainers);
        Set<String> containersToEnrich = new HashSet<>();
        if (enrichContainers != null) {
            containersToEnrich.addAll(Arrays.asList(enrichContainers.split(",")));
        }

        if (enrichAllContainers) {
            return containerBuilders;
        } else if (!containersToEnrich.isEmpty()) {
            List<ContainerBuilder> filteredContainers = new LinkedList<>();
            for (ContainerBuilder container : containerBuilders) {
                if (container.hasName() && containersToEnrich.contains(container.getName())) {
                    filteredContainers.add(container);
                }
            }
            return filteredContainers;
        } else if (containerBuilders.size() == 1) {
            return containerBuilders;
        } else {
            // Multiple unfiltered containers, enrich only the generated ones
            List<ContainerBuilder> generatedContainers = new LinkedList<>();
            List<String> fabric8GeneratedContainers = getProcessingInstructionViaKey(FABRIC8_GENERATED_CONTAINERS);
            for (ContainerBuilder container : containerBuilders) {
                if (container.hasName() && fabric8GeneratedContainers.contains(container.getName())) {
                    generatedContainers.add(container);
                }
            }
            return generatedContainers;
        }
    }

    /**
     * Override this method to create a per-container readiness probe.
     */
    protected Probe getReadinessProbe(ContainerBuilder containerBuilder) {
        // return a generic probe by default
        return getReadinessProbe();
    }

    /**
     * Override this method to create a generic readiness probe.
     */
    protected Probe getReadinessProbe() {
        return null;
    }

    /**
     * Override this method to create a per-container liveness probe.
     */
    protected Probe getLivenessProbe(ContainerBuilder containerBuilder) {
        // return a generic probe by default
        return getLivenessProbe();
    }

    /**
     * Override this method to create a generic liveness probe.
     */
    protected Probe getLivenessProbe() {
        return null;
    }

}
