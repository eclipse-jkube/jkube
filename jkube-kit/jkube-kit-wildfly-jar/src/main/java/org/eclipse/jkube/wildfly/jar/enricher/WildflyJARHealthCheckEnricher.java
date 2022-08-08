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
package org.eclipse.jkube.wildfly.jar.enricher;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;

/**
 * Enriches wildfly-jar containers with health checks if the bootable JAR has
 * been configured for cloud.
 */
public class WildflyJARHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public static final String BOOTABLE_JAR_GROUP_ID = "org.wildfly.plugins";
    public static final String BOOTABLE_JAR_ARTIFACT_ID = "wildfly-jar-maven-plugin";

    public WildflyJARHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-wildfly-jar");
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        SCHEME("scheme", "HTTP"),
        PORT("port", "9990"),
        FAILURETHRESHOLD("failureThreshold","3"),
        SUCCESSTHRESHOLD("successThreshold","1"),
        LIVENESSINITIALDELAY("livenessInitialDelay", "60"),
        READINESSINITIALDELAY("readinessInitialDelay", "10"),
        READINESSPATH("readinessPath", "/health/ready"),
        LIVENESSPATH("livenessPath", "/health/live"),
        ENFORCEPROBES("enforceProbes","false");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverWildflyJARHealthCheck(true);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverWildflyJARHealthCheck(false);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        // Add HOSTNAME
        if (PlatformMode.kubernetes.equals(platformMode)) {
            final List<ContainerBuilder> containerBuilders = new LinkedList<>();
            builder.accept(new TypedVisitor<ContainerBuilder>() {
                @Override
                public void visit(ContainerBuilder containerBuilder) {
                    containerBuilders.add(containerBuilder);
                }
            });
            for (ContainerBuilder container : containerBuilders) {
                container.addToEnv(new EnvVarBuilder()
                        .withName("HOSTNAME")
                        .withNewValueFrom()
                        .withNewFieldRef()
                        .withFieldPath("metadata.name")
                        .endFieldRef()
                        .endValueFrom()
                        .build());
            }
        }
        super.create(platformMode, builder);
    }

    private Probe discoverWildflyJARHealthCheck(boolean isReadiness) {

        if (isAvailable()) {
            Integer port = getPort();
            if (port <= 0) {
                return null;
            }
            int initialDelay = isReadiness ? getReadinessInitialDelay() : getLivenessInitialDelay();
            // scheme must be in upper case in k8s
            String scheme = getScheme().toUpperCase();
            String path = isReadiness ? getReadinessPath() : getLivenessPath();

            return new ProbeBuilder()
                    .withNewHttpGet().withNewPort(port).withPath(path).withScheme(scheme).endHttpGet()
                    .withFailureThreshold(getFailureThreshold())
                    .withSuccessThreshold(getSuccessThreshold())
                    .withInitialDelaySeconds(initialDelay).build();
        }
        return null;
    }

    private boolean isAvailable() {
        if (isProbeEnforced()) {
            return true;
        }
        JavaProject project = getContext().getProject();
        Plugin plugin = JKubeProjectUtil.getPlugin(project, BOOTABLE_JAR_GROUP_ID, BOOTABLE_JAR_ARTIFACT_ID);
        if (plugin == null) {
            return false;
        }
        Map<String, Object> config = plugin.getConfiguration();
        return config.containsKey("cloud");
    }

    protected int getFailureThreshold() {
        return Configs.asInteger(getConfig(Config.FAILURETHRESHOLD));
    }

    protected boolean isProbeEnforced() {
        return Configs.asBoolean(getConfig(Config.ENFORCEPROBES));
    }

    protected int getSuccessThreshold() {
        return Configs.asInteger(getConfig(Config.SUCCESSTHRESHOLD));
    }

    protected String getScheme() {
        return Configs.asString(getConfig(Config.SCHEME));
    }

    protected int getPort() {
        return Configs.asInt(getConfig(Config.PORT));
    }

    protected String getLivenessPath() {
        return Configs.asString(getConfig(Config.LIVENESSPATH));
    }

    protected int getLivenessInitialDelay() {
        return Configs.asInt(getConfig(Config.LIVENESSINITIALDELAY));
    }

    protected String getReadinessPath() {
        return Configs.asString(getConfig(Config.READINESSPATH));
    }

    protected int getReadinessInitialDelay() {
        return Configs.asInt(getConfig(Config.READINESSINITIALDELAY));
    }
}
