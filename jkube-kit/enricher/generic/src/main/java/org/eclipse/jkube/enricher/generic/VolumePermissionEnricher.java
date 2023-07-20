/*
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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author roland
 */
public class VolumePermissionEnricher extends BaseEnricher {

    public static final String ENRICHER_NAME = "jkube-volume-permission";

    @AllArgsConstructor
    enum Config implements Configs.Config {
        IMAGE_NAME("imageName", "busybox"),
        PERMISSION("permission", "777"),
        /**
         * @deprecated Use configuration field in PersistentVolumeClaimStorageClassEnricher
         */
        @Deprecated
        DEFAULT_STORAGE_CLASS("defaultStorageClass", null),
        /**
         * @deprecated Use configuration field in PersistentVolumeClaimStorageClassEnricher
         */
        @Deprecated
        USE_ANNOTATION("useStorageClassAnnotation", "false"),
        CPU_LIMIT("cpuLimit", null),
        CPU_REQUEST("cpuRequest", null),
        MEMORY_LIMIT("memoryLimit", null),
        MEMORY_REQUEST("memoryRequest", null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public VolumePermissionEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {

        builder.accept(new TypedVisitor<PodTemplateSpecBuilder>() {
            @Override
            public void visit(PodTemplateSpecBuilder builder) {
                PodSpec podSpec = builder.buildSpec();
                if (podSpec == null) {
                    return;
                }

                if (!checkForPvc(podSpec)) {
                    return;
                }

                List<Container> containers = podSpec.getContainers();
                if (containers == null || containers.isEmpty()) {
                    return;
                }

                log.verbose("Adding init container for changing persistent volumes access mode to %s",
                        getConfig(Config.PERMISSION));
                if (!KubernetesResourceUtil.hasInitContainer(builder, ENRICHER_NAME)) {
                    KubernetesResourceUtil.appendInitContainer(builder, createPvInitContainer(podSpec), log);
                }
            }

            private boolean checkForPvc(PodSpec podSpec) {
                List<Volume> volumes = podSpec.getVolumes();
                if (volumes != null) {
                    for (Volume volume : volumes) {
                        PersistentVolumeClaimVolumeSource persistentVolumeClaim = volume.getPersistentVolumeClaim();
                        if (persistentVolumeClaim != null) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private Container createPvInitContainer(PodSpec podSpec) {
                Map<String, String> mountPoints = extractMountPoints(podSpec);
                return new ContainerBuilder()
                        .withName(ENRICHER_NAME)
                        .withImage(getConfig(Config.IMAGE_NAME))
                        .withImagePullPolicy("IfNotPresent")
                        .withCommand(createChmodCommandArray(mountPoints))
                        .withVolumeMounts(createMounts(mountPoints))
                        .withResources(new ResourceRequirementsBuilder()
                            .withLimits(createResourcesMap(Config.CPU_LIMIT, Config.MEMORY_LIMIT))
                            .withRequests(createResourcesMap(Config.CPU_REQUEST, Config.MEMORY_REQUEST))
                            .build())
                        .build();
            }

            private List<String> createChmodCommandArray(Map<String, String> mountPoints) {
                List<String> ret = new ArrayList<>();
                ret.add("chmod");
                ret.add(getConfig(Config.PERMISSION));
                Set<String> uniqueNames = new LinkedHashSet<>(mountPoints.values());
                ret.addAll(uniqueNames);
                return ret;
            }

            private List<VolumeMount> createMounts(Map<String, String> mountPoints) {
                List<VolumeMount> ret = new ArrayList<>();
                for (Map.Entry<String, String> entry : mountPoints.entrySet()) {
                    JsonObject mount = new JsonObject();
                    mount.add("name", new JsonPrimitive(entry.getKey()));
                    mount.add("mountPath", new JsonPrimitive(entry.getValue()));

                    VolumeMount volumeMount = new VolumeMountBuilder()
                            .withName(entry.getKey())
                            .withMountPath(entry.getValue())
                            .build();
                    ret.add(volumeMount);
                }
                return ret;
            }

            private Map<String, String> extractMountPoints(PodSpec podSpec) {
                Map<String, String> nameToMount = new LinkedHashMap<>();

                List<Volume> volumes = podSpec.getVolumes();
                if (volumes != null) {
                    for (Volume volume : volumes) {
                        PersistentVolumeClaimVolumeSource persistentVolumeClaim = volume.getPersistentVolumeClaim();
                        if (persistentVolumeClaim != null) {
                            String name = volume.getName();
                            String mountPath = getMountPath(podSpec.getContainers(), name);

                            nameToMount.put(name, mountPath);
                        }
                    }
                }
                return nameToMount;
            }

            private String getMountPath(List<Container> containers, String name){
                for (Container container : containers) {
                    List<VolumeMount> volumeMounts = container.getVolumeMounts();
                    if (volumeMounts != null) {
                        for (VolumeMount volumeMount : volumeMounts) {
                            if (name.equals(volumeMount.getName())){
                                return volumeMount.getMountPath();
                            }
                        }
                    }
                }
                throw new IllegalArgumentException("No matching volume mount found for volume "+ name);
            }

            private Map<String, Quantity> createResourcesMap(Configs.Config cpu, Configs.Config memory) {
                Map<String, Quantity> resourcesMap = new HashMap<>();
                String cpuValue = getConfig(cpu);
                if (StringUtils.isNotBlank(cpuValue)) {
                    resourcesMap.put("cpu", new Quantity(cpuValue));
                }
                String memoryValue = getConfig(memory);
                if (StringUtils.isNotBlank(memoryValue)) {
                    resourcesMap.put("memory", new Quantity(memoryValue));
                }
                return resourcesMap;
            }
        });
    }

    /**
     * Get useAnnotation from enricher configuration
     * TODO: This method is kept only for backward compatibility. This should
     *       be removed in future. See <a href="https://github.com/eclipse/jkube/issues/1989">GitHub Issue</a> for more details
     *
     * @return boolean value indicating whether StorageClass annotation should be used or not
     */
    public boolean shouldUseAnnotation() {
        return Boolean.parseBoolean(getConfig(Config.USE_ANNOTATION));
    }

    /**
     * Get Default StorageClass from enricher configuration
     *
     * TODO: This method is kept only for backward compatibility. This should
     *       be removed in future. See <a href="https://github.com/eclipse/jkube/issues/1989">GitHub Issue</a> for more details
     * @return default storage class
     */
    public String getDefaultStorageClass() {
        return getConfig(Config.DEFAULT_STORAGE_CLASS);
    }
}
