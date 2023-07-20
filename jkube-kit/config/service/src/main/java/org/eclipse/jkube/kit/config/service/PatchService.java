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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.UserConfigurationCompare;

import java.util.HashMap;
import java.util.Map;

public class PatchService {
    private final KubernetesClient kubernetesClient;

    private static final Map<String, EntityPatcher<? extends HasMetadata>> patchers;

    // Interface for patching entities
    interface EntityPatcher<T extends HasMetadata> {

        /**
         * Patch a given entity with new value and edit it on the server
         *
         * @param client kubernetes client used for patching the entity
         * @param namespace namespace where the entity lives
         * @param newEntity the new, possibly changed entity
         * @param oldEntity the original entity
         * @return the patched entity, or the old entity if nothing has changed.
         */
        T patch(KubernetesClient client, String namespace, T newEntity, T oldEntity);
    }

    static {
        patchers = new HashMap<>();
        patchers.put("ReplicationController", rcPatcher());
        patchers.put("BuildConfig", bcPatcher());
        patchers.put("ImageStream", isPatcher());
        patchers.put("PersistentVolumeClaim", pvcPatcher());
        patchers.put("Job", jobPatcher());
        patchers.put("Route", routePatcher());
    }

    public PatchService(KubernetesClient client) {
        this.kubernetesClient = client;
    }

    public <T extends HasMetadata> T compareAndPatchEntity(String namespace, T newDto, T oldDto) {
        EntityPatcher<T> dispatcher = (EntityPatcher<T>) patchers.get(newDto.getKind());
        if (dispatcher == null) {
            throw new IllegalArgumentException("Internal: No patcher for " + newDto.getKind() + " found");
        }
        /*
         * This is done in order to fix https://github.com/openshift/origin/issues/19905
         */
        newDto.getMetadata().setResourceVersion(oldDto.getMetadata().getResourceVersion());
        return dispatcher.patch(kubernetesClient, namespace, newDto, oldDto);
    }

    private static EntityPatcher<ReplicationController> rcPatcher() {
        return (KubernetesClient client, String namespace, ReplicationController newObj, ReplicationController oldObj) -> {
            if (UserConfigurationCompare.configEqual(newObj, oldObj)) {
                return oldObj;
            }

            ReplicationControllerBuilder entity =
                new ReplicationControllerBuilder(client.replicationControllers()
                      .inNamespace(namespace)
                      .withName(oldObj.getMetadata().getName())
                      .get());

            if (!UserConfigurationCompare.configEqual(newObj.getMetadata(), oldObj.getMetadata())) {
                entity.withMetadata(newObj.getMetadata());
            }

            if(!UserConfigurationCompare.configEqual(newObj.getSpec(), oldObj.getSpec())) {
                entity.withSpec(newObj.getSpec());
            }
            return client.replicationControllers()
                    .inNamespace(namespace)
                    .withName(oldObj.getMetadata().getName())
                    .edit(p -> entity.build());
        };
    }

    private static EntityPatcher<PersistentVolumeClaim> pvcPatcher() {
        return (KubernetesClient client, String namespace, PersistentVolumeClaim newObj, PersistentVolumeClaim oldObj) -> {
            if (UserConfigurationCompare.configEqual(newObj, oldObj)) {
                return oldObj;
            }
            PersistentVolumeClaimBuilder entity =
                new PersistentVolumeClaimBuilder(client.persistentVolumeClaims()
                      .inNamespace(namespace)
                      .withName(oldObj.getMetadata().getName())
                      .get());

            if (!UserConfigurationCompare.configEqual(newObj.getMetadata(), oldObj.getMetadata())) {
                entity.withMetadata(newObj.getMetadata());
            }

            if(!UserConfigurationCompare.configEqual(newObj.getSpec(), oldObj.getSpec())) {
                entity.withSpec(newObj.getSpec());
            }
            return client.persistentVolumeClaims()
                    .inNamespace(namespace)
                    .withName(oldObj.getMetadata().getName())
                    .edit(p -> entity.build());
        };
    }

    private static EntityPatcher<Job> jobPatcher() {
        return (KubernetesClient client, String namespace, Job newObj, Job oldObj) -> {
            if (UserConfigurationCompare.configEqual(newObj, oldObj)) {
                return oldObj;
            }

            JobBuilder entity = new JobBuilder(client.batch().v1().jobs().withName(oldObj.getMetadata().getName()).get());

            if (!UserConfigurationCompare.configEqual(newObj.getMetadata(), oldObj.getMetadata())) {
                entity.withMetadata(newObj.getMetadata());
            }

            if (!UserConfigurationCompare.configEqual(newObj.getSpec().getSelector(), oldObj.getSpec().getSelector())) {
                entity.editSpec().withSelector(newObj.getSpec().getSelector());
            }

            if (!UserConfigurationCompare.configEqual(newObj.getSpec().getTemplate(), oldObj.getSpec().getSelector())) {
                entity.editSpec().withTemplate(newObj.getSpec().getTemplate());
            }

            return client.batch().v1().jobs().withName(oldObj.getMetadata().getName()).edit(p -> entity.build());
        };
    }

    // ================================================================================
    // OpenShift objects:
    // =================

    private static EntityPatcher<BuildConfig> bcPatcher() {
        return (KubernetesClient client, String namespace, BuildConfig newObj, BuildConfig oldObj) -> {
            if (UserConfigurationCompare.configEqual(newObj, oldObj)) {
                return oldObj;
            }
            OpenShiftClient openShiftClient = OpenshiftHelper.asOpenShiftClient(client);
            if (openShiftClient == null) {
                throw new IllegalArgumentException("BuildConfig can only be patched when connected to an OpenShift cluster");
            }
            BuildConfigBuilder entity =
                new BuildConfigBuilder(openShiftClient.buildConfigs()
                      .inNamespace(namespace)
                      .withName(oldObj.getMetadata().getName()).get());

            if (!UserConfigurationCompare.configEqual(newObj.getMetadata(), oldObj.getMetadata())) {
                entity.withMetadata(newObj.getMetadata());
            }

            if(!UserConfigurationCompare.configEqual(newObj.getSpec(), oldObj.getSpec())) {
                entity.withSpec(newObj.getSpec());
            }
            return openShiftClient.buildConfigs()
                    .inNamespace(namespace)
                    .withName(oldObj.getMetadata().getName())
                    .edit(p -> entity.build());
        };
    }

    private static EntityPatcher<ImageStream> isPatcher() {
        return (KubernetesClient client, String namespace, ImageStream newObj, ImageStream oldObj) -> {
            if (UserConfigurationCompare.configEqual(newObj, oldObj)) {
                return oldObj;
            }
            OpenShiftClient openShiftClient = OpenshiftHelper.asOpenShiftClient(client);
            if (openShiftClient == null) {
                throw new IllegalArgumentException("ImageStream can only be patched when connected to an OpenShift cluster");
            }
            ImageStreamBuilder entity =
                new ImageStreamBuilder(openShiftClient.imageStreams()
                      .inNamespace(namespace)
                      .withName(oldObj.getMetadata().getName())
                      .get());

            if (!UserConfigurationCompare.configEqual(newObj.getMetadata(), oldObj.getMetadata())) {
                entity.withMetadata(newObj.getMetadata());
            }

            if(!UserConfigurationCompare.configEqual(newObj.getSpec(), oldObj.getSpec())) {
                entity.withSpec(newObj.getSpec());
            }
            return openShiftClient.imageStreams()
                    .inNamespace(namespace)
                    .withName(oldObj.getMetadata().getName())
                    .edit(p -> entity.build());
        };
    }

    private static EntityPatcher<Route> routePatcher() {
        return (client, namespace, newObj, oldObj) -> {
            if (UserConfigurationCompare.configEqual(newObj, oldObj)) {
                return oldObj;
            }
            OpenShiftClient openShiftClient = OpenshiftHelper.asOpenShiftClient(client);
            if (openShiftClient == null) {
                throw new IllegalArgumentException("Route can only be patched when connected to an OpenShift cluster");
            }

            RouteBuilder entity = new RouteBuilder(openShiftClient.routes()
                    .inNamespace(namespace)
                    .withName(oldObj.getMetadata().getName())
                    .get());

            if (!UserConfigurationCompare.configEqual(newObj.getMetadata(), oldObj.getMetadata())) {
                entity.withMetadata(newObj.getMetadata());
            }

            if(!UserConfigurationCompare.configEqual(newObj.getSpec(), oldObj.getSpec())) {
                entity.withSpec(newObj.getSpec());
            }

            return openShiftClient.routes()
                    .inNamespace(namespace)
                    .withName(oldObj.getMetadata().getName())
                    .edit(p -> entity.build());
        };
    }

}
