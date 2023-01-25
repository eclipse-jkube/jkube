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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.eclipse.jkube.kit.common.Configs.asBoolean;
import static org.eclipse.jkube.kit.common.Configs.asInt;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.BOOTABLE_JAR_ARTIFACT_ID;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.BOOTABLE_JAR_GROUP_ID;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.DEFAULT_LIVENESS_PATH;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.DEFAULT_READINESS_PATH;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.DEFAULT_STARTUP_PATH;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.isStartupEndpointSupported;

/**
 * Enriches wildfly-jar containers with health checks if the bootable JAR has
 * been configured for cloud.
 */
public class WildflyJARHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public WildflyJARHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-wildfly-jar");
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        SCHEME("scheme", "HTTP"),
        PORT("port", "9990"),
        FAILURE_THRESHOLD("failureThreshold","3"),
        SUCCESS_THRESHOLD("successThreshold","1"),
        LIVENESS_INITIAL_DELAY("livenessInitialDelay", "60"),
        READINESS_INITIAL_DELAY("readinessInitialDelay", "10"),
        STARTUP_INITIAL_DELAY("startupInitialDelay", "10"),
        READINESS_PATH("readinessPath", DEFAULT_READINESS_PATH),
        LIVENESS_PATH("livenessPath", DEFAULT_LIVENESS_PATH),
        STARTUP_PATH("startupPath", DEFAULT_STARTUP_PATH),
        PERIOD_SECONDS("periodSeconds", "10"),
        ENFORCE_PROBES("enforceProbes","false");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverWildflyJARHealthCheck(Config.READINESS_PATH, Config.READINESS_INITIAL_DELAY);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverWildflyJARHealthCheck(Config.LIVENESS_PATH, Config.LIVENESS_INITIAL_DELAY);
    }

    @Override
    protected Probe getStartupProbe() {
      if (isStartupEndpointSupported(getContext().getProject())) {
          return discoverWildflyJARHealthCheck(Config.STARTUP_PATH, Config.STARTUP_INITIAL_DELAY);
      }
      return null;
    }

    private Probe discoverWildflyJARHealthCheck(Config path, Config initialDelay) {
        if (isAvailable()) {
            int port = asInt(getConfig(Config.PORT));
            if (port <= 0) {
                return null;
            }
            return new ProbeBuilder()
                    .withNewHttpGet().withNewPort(port)
                    .withPath(getConfig(path))
                    .withScheme(getConfig(Config.SCHEME).toUpperCase())
                    .endHttpGet()
                    .withFailureThreshold(asInt(getConfig(Config.FAILURE_THRESHOLD)))
                    .withSuccessThreshold(asInt(getConfig(Config.SUCCESS_THRESHOLD)))
                    .withInitialDelaySeconds(asInt(getConfig(initialDelay)))
                    .withPeriodSeconds(asInt(getConfig(Config.PERIOD_SECONDS)))
                    .build();
        }
        return null;
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

    private boolean isAvailable() {
        if (asBoolean(getConfig(Config.ENFORCE_PROBES))) {
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

}
