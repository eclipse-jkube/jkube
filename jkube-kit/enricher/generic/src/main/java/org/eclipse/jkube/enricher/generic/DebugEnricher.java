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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jkube.maven.enricher.api.util.DebugConstants.ENV_VAR_JAVA_DEBUG;
import static org.eclipse.jkube.maven.enricher.api.util.DebugConstants.ENV_VAR_JAVA_DEBUG_PORT;
import static org.eclipse.jkube.maven.enricher.api.util.DebugConstants.ENV_VAR_JAVA_DEBUG_PORT_DEFAULT;


/**
 * Enables debug mode via a maven property
 */
public class DebugEnricher extends BaseEnricher {

    public static final String ENABLE_DEBUG_MAVEN_PROPERTY = "jkube.debug.enabled";

    public DebugEnricher(JkubeEnricherContext buildContext) {
        super(buildContext, "jkube-debug");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if (debugEnabled()) {
            int count = 0;
            List<HasMetadata> items = builder.getItems();
            if (items != null) {
                for (HasMetadata item : items) {
                    if (enableDebug(item)) {
                        count++;
                    }
                }
            }
            if (count > 0) {
                builder.withItems(items);
            }
            log.verbose("Enabled debugging on "
                + count
                + " resource(s) thanks to the "
                + ENABLE_DEBUG_MAVEN_PROPERTY
                + " property");
        } else {
            log.verbose("Debugging not enabled. To enable try setting the "
                + ENABLE_DEBUG_MAVEN_PROPERTY
                + " maven or system property to 'true'");
        }
    }

    private boolean debugEnabled() {
        String value = getContext().getConfiguration().getProperties().getProperty(ENABLE_DEBUG_MAVEN_PROPERTY);
        if (value != null && isTrueFlag(value)) {
            return true;
        }
        return isTrueFlag(System.getProperty(ENABLE_DEBUG_MAVEN_PROPERTY));
    }

    private static boolean isTrueFlag(String value) {
        return StringUtils.isNotBlank(value) && value.equals("true");
    }

    private boolean enableDebug(HasMetadata entity) {
        if (entity instanceof Deployment) {
            Deployment resource = (Deployment) entity;
            DeploymentSpec spec = resource.getSpec();
            if (spec != null) {
                return enableDebugging(entity, spec.getTemplate());
            }
        } else if (entity instanceof ReplicaSet) {
            ReplicaSet resource = (ReplicaSet) entity;
            ReplicaSetSpec spec = resource.getSpec();
            if (spec != null) {
                return enableDebugging(entity, spec.getTemplate());
            }
        } else if (entity instanceof ReplicationController) {
            ReplicationController resource = (ReplicationController) entity;
            ReplicationControllerSpec spec = resource.getSpec();
            if (spec != null) {
                return enableDebugging(entity, spec.getTemplate());
            }
        } else if (entity instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) entity;
            DeploymentConfigSpec spec = resource.getSpec();
            if (spec != null) {
                return enableDebugging(entity, spec.getTemplate());
            }
        }
        return false;
    }

    private boolean enableDebugging(HasMetadata entity, PodTemplateSpec template) {
        if (template != null) {
            PodSpec podSpec = template.getSpec();
            if (podSpec != null) {
                List<Container> containers = podSpec.getContainers();
                if (containers.size() > 0) {
                    Container container = containers.get(0);
                    List<EnvVar> env = container.getEnv();
                    if (env == null) {
                        env = new ArrayList<>();
                    }
                    String remoteDebugPort =
                        KubernetesHelper.getEnvVar(env, ENV_VAR_JAVA_DEBUG_PORT, ENV_VAR_JAVA_DEBUG_PORT_DEFAULT);
                    boolean enabled = false;
                    if (KubernetesHelper.setEnvVar(env, ENV_VAR_JAVA_DEBUG, "true")) {
                        container.setEnv(env);
                        enabled = true;
                    }
                    List<ContainerPort> ports = container.getPorts();
                    if (ports == null) {
                        ports = new ArrayList<>();
                    }
                    if (KubernetesResourceUtil.addPort(ports, remoteDebugPort, "debug", log)) {
                        container.setPorts(ports);
                        enabled = true;
                    }
                    if (enabled) {
                        log.info("Enabling debug on " + KubernetesHelper.getKind(entity) + " " + KubernetesHelper.getName(
                            entity) + " due to the property: " + ENABLE_DEBUG_MAVEN_PROPERTY);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
