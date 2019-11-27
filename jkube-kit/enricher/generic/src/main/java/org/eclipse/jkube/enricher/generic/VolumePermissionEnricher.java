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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;
import org.eclipse.jkube.maven.enricher.api.util.InitContainerHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author roland
 * @since 14/11/16
 */
public class VolumePermissionEnricher extends BaseEnricher {

    public static final String ENRICHER_NAME = "jkube-volume-permission";
    static final String VOLUME_STORAGE_CLASS_ANNOTATION = "volume.beta.kubernetes.io/storage-class";

    private final InitContainerHandler initContainerHandler;

    enum Config implements Configs.Key {
        permission {{ d = "777"; }},
        defaultStorageClass {{ d = null; }};

        public String def() { return d; } protected String d;
    }

    public VolumePermissionEnricher(MavenEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
        initContainerHandler = new InitContainerHandler(buildContext.getLog());
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
                        getConfig(Config.permission));
                if (!initContainerHandler.hasInitContainer(builder, ENRICHER_NAME)) {
                    initContainerHandler.appendInitContainer(builder, createPvInitContainer(podSpec));
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
                        .withImage("busybox")
                        .withImagePullPolicy("IfNotPresent")
                        .withCommand(createChmodCommandArray(mountPoints))
                        .withVolumeMounts(createMounts(mountPoints))
                        .build();
            }

            private List<String> createChmodCommandArray(Map<String, String> mountPoints) {
                List<String> ret = new ArrayList<>();
                ret.add("chmod");
                ret.add(getConfig(Config.permission));
                Set<String> uniqueNames = new LinkedHashSet<>(mountPoints.values());
                for (String name : uniqueNames) {
                    ret.add(name);
                }
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

        });

        builder.accept(new TypedVisitor<PersistentVolumeClaimBuilder>() {
            @Override
            public void visit(PersistentVolumeClaimBuilder pvcBuilder) {
                // lets ensure we have a default storage class so that PVs will get dynamically created OOTB
                if (pvcBuilder.buildMetadata() == null) {
                    pvcBuilder.withNewMetadata().endMetadata();
                }
                String storageClass = getConfig(Config.defaultStorageClass);
                if (StringUtils.isNotBlank(storageClass) && !pvcBuilder.buildMetadata().getAnnotations().containsKey(VOLUME_STORAGE_CLASS_ANNOTATION)) {
                    pvcBuilder.editMetadata().addToAnnotations(VOLUME_STORAGE_CLASS_ANNOTATION, storageClass).endMetadata();
                }
            }
        });
    }
}
