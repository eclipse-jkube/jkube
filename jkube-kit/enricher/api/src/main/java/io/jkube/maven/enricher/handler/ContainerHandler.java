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
package io.jkube.maven.enricher.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.access.PortMapping;
import io.jkube.kit.common.util.EnvUtil;
import io.jkube.kit.config.image.ImageName;
import io.jkube.kit.config.image.build.BuildConfiguration;
import io.jkube.kit.config.resource.GroupArtifactVersion;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.kit.config.resource.VolumeConfig;
import io.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author roland
 * @since 08/04/16
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

    List<Container> getContainers(ResourceConfig config, List<ImageConfiguration> images)  {
        List<Container> ret = new ArrayList<>();

        for (ImageConfiguration imageConfig : images) {
            if (imageConfig.getBuildConfiguration() != null) {
                Probe livenessProbe = probeHandler.getProbe(config.getLiveness());
                Probe readinessProbe = probeHandler.getProbe(config.getReadiness());

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
                    .build();
                ret.add(container);
            }
        }
        return ret;
    }

    private List<EnvVar> getEnvVars(ResourceConfig config) {
        List<EnvVar> envVars = KubernetesResourceUtil.convertToEnvVarList(config.getEnv().orElse(Collections.emptyMap()));

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


    private String getImagePullPolicy(ResourceConfig config) {
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
            props.getProperty("docker.pull.registry"),
            props.getProperty("docker.registry"));

        return new ImageName(imageConfiguration.getName()).getFullName(configuredRegistry);
    }

    private Properties getPropertiesWithSystemOverrides(Properties configurationProperties) {

        if (configurationProperties == null) {
            configurationProperties = new Properties();
        }

        configurationProperties.putAll(System.getProperties());
        return configurationProperties;
    }

    private SecurityContext createSecurityContext(ResourceConfig config) {
        return new SecurityContextBuilder()
            .withPrivileged(config.isContainerPrivileged())
            .build();
    }


    private List<VolumeMount> getVolumeMounts(ResourceConfig config) {
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

}
