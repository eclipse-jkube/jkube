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
package org.eclipse.jkube.kit.enricher.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.eclipse.jkube.kit.build.api.model.PortMapping;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.ContainerResourcesConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getQuantityFromString;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.isContainerImage;

/**
 * @author roland
 */
public class ContainerHandler {

    private final ProbeHandler probeHandler;
    private final Properties configurationProperties;
    private final GroupArtifactVersion groupArtifactVersion;

    public ContainerHandler(Properties configurationProperties, GroupArtifactVersion groupArtifactVersion, ProbeHandler probeHandler) {
        this.probeHandler = probeHandler;
        this.configurationProperties = configurationProperties;
        this.groupArtifactVersion = groupArtifactVersion;
    }

    List<Container> getContainers(ControllerResourceConfig config, List<ImageConfiguration> images)  {
        List<Container> ret = new ArrayList<>();

        for (ImageConfiguration imageConfig : images) {
            if (isContainerImage(imageConfig, config)) {
                Probe livenessProbe = probeHandler.getProbe(config.getLiveness());
                Probe readinessProbe = probeHandler.getProbe(config.getReadiness());
                Probe startupProbe = probeHandler.getProbe(config.getStartup());

                Container container = new ContainerBuilder()
                    .withName(KubernetesResourceUtil.extractContainerName(this.groupArtifactVersion, imageConfig))
                    .withImage(getImageName(imageConfig))
                    .withImagePullPolicy(getImagePullPolicy(config))
                    .withEnv(getEnvVars(config))
                    .withSecurityContext(createSecurityContext(config))
                    .withPorts(getContainerPorts(imageConfig))
                    .withVolumeMounts(getVolumeMounts(config))
                    .withLivenessProbe(livenessProbe)
                    .withReadinessProbe(readinessProbe)
                    .withStartupProbe(startupProbe)
                    .withResources(createResourcesFromConfig(config))
                    .build();
                ret.add(container);
            }
        }
        return ret;
    }

    private List<EnvVar> getEnvVars(ControllerResourceConfig config) {
        List<EnvVar> envVars = KubernetesHelper.convertToEnvVarList(config.getEnv());

        // TODO: This should go into an extra enricher so that this behaviour can be switched on / off
        envVars.removeIf(obj -> obj.getName().equals("KUBERNETES_NAMESPACE"));
        envVars.add(0,
            new EnvVarBuilder()
                .withName("KUBERNETES_NAMESPACE")
                .withNewValueFrom()
                  .withNewFieldRef()
                     .withFieldPath("metadata.namespace")
                  .endFieldRef()
                .endValueFrom()
                .build());

        return envVars;
    }

    private String getImagePullPolicy(ControllerResourceConfig config) {
        String pullPolicy = config.getImagePullPolicy();
        if (StringUtils.isBlank(pullPolicy) &&
            this.groupArtifactVersion.isSnapshot()) {
            // TODO: Is that what we want ?
            return "PullAlways";
        }
        return pullPolicy;
    }

    private String getImageName(ImageConfiguration imageConfiguration) {
        if (StringUtils.isBlank(imageConfiguration.getName())) {
            return null;
        }
        Properties props = getPropertiesWithSystemOverrides(this.configurationProperties);
        String configuredRegistry = EnvUtil.firstRegistryOf(
            imageConfiguration.getRegistry(),
            props.getProperty("jkube.docker.pull.registry"),
            props.getProperty("jkube.docker.registry"));

        return new ImageName(imageConfiguration.getName()).getFullName(configuredRegistry);
    }

    private Properties getPropertiesWithSystemOverrides(Properties configurationProperties) {

        if (configurationProperties == null) {
            configurationProperties = new Properties();
        }

        configurationProperties.putAll(System.getProperties());
        return configurationProperties;
    }

    private SecurityContext createSecurityContext(ControllerResourceConfig config) {
        return new SecurityContextBuilder()
            .withPrivileged(config.isContainerPrivileged())
            .build();
    }

    private List<VolumeMount> getVolumeMounts(ControllerResourceConfig config) {
        List<VolumeConfig> volumeConfigs = config.getVolumes();

        List<VolumeMount> ret = new ArrayList<>();
        if (volumeConfigs != null) {
            for (VolumeConfig volumeConfig : volumeConfigs) {
                List<String> mounts = volumeConfig.getMounts();
                if (mounts != null) {
                    for (String mount : mounts) {
                        ret.add(new VolumeMountBuilder()
                                    .withName(volumeConfig.getName())
                                    .withMountPath(mount)
                                    .withReadOnly(false).build());
                    }
                }
            }
        }
        return ret;
    }

    private List<ContainerPort> getContainerPorts(ImageConfiguration imageConfig) {
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
        List<String> ports = buildConfig.getPorts();
        if (!ports.isEmpty()) {
            List<ContainerPort> ret = new ArrayList<>();
            PortMapping portMapping = new PortMapping(ports, configurationProperties);
            JsonArray portSpecs = portMapping.toJson();
            for (int i = 0; i < portSpecs.size(); i ++) {
                JsonObject portSpec = portSpecs.get(i).getAsJsonObject();
                ret.add(extractContainerPort(portSpec));
            }
            return ret;
        } else {
            return null;
        }
    }

    private ContainerPort extractContainerPort(JsonObject portSpec) {
        ContainerPortBuilder portBuilder = new ContainerPortBuilder()
            .withContainerPort(portSpec.get("containerPort").getAsInt());
        if (portSpec.has("hostPort")) {
            portBuilder.withHostPort(portSpec.get("hostPort").getAsInt());
        }
        if (portSpec.has("protocol")) {
            portBuilder.withProtocol(portSpec.get("protocol").getAsString().toUpperCase());
        }
        if (portSpec.has("hostIP")) {
            portBuilder.withHostIP(portSpec.get("hostIP").getAsString());
        }
        return portBuilder.build();
    }

    private ResourceRequirements createResourcesFromConfig(ControllerResourceConfig config) {
        if (config != null && config.getContainerResources() != null) {
            ContainerResourcesConfig containerResources = config.getContainerResources();
            ResourceRequirementsBuilder resourceRequirementsBuilder = new ResourceRequirementsBuilder();
            if (containerResources.getRequests() != null && !containerResources.getRequests().isEmpty()) {
                resourceRequirementsBuilder.withRequests(getQuantityFromString(containerResources.getRequests()));
            }
            if (containerResources.getLimits() != null && !containerResources.getLimits().isEmpty()) {
                resourceRequirementsBuilder.withLimits(getQuantityFromString(containerResources.getLimits()));
            }
            return resourceRequirementsBuilder.build();
        }
        return null;
    }
}
