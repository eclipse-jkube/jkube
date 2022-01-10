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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageName;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.Scaleable;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

/**
 * Utility class for executing common tasks using the Kubernetes client
 *
 * @author nicola
 */
public class KubernetesClientUtil {

    private KubernetesClientUtil() { }

    public static void resizeApp(KubernetesClient kubernetes, String namespace, Collection<HasMetadata> entities, int replicas, KitLogger log) {
        for (HasMetadata entity : entities) {
            String name = KubernetesHelper.getName(entity);
            Scaleable<?> scalable = null;
            if (entity instanceof Deployment) {
                scalable = kubernetes.apps().deployments().inNamespace(namespace).withName(name);
            } else if (entity instanceof ReplicaSet) {
                scalable = kubernetes.apps().replicaSets().inNamespace(namespace).withName(name);
            } else if (entity instanceof ReplicationController) {
                scalable = kubernetes.replicationControllers().inNamespace(namespace).withName(name);
            } else if (entity instanceof DeploymentConfig) {
                OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kubernetes);
                if (openshiftClient == null) {
                    log.warn("Ignoring DeploymentConfig %s as not connected to an OpenShift cluster", name);
                    continue;
                }
                scalable = openshiftClient.deploymentConfigs().inNamespace(namespace).withName(name);
            }
            if (scalable != null) {
                log.info("Scaling " + KubernetesHelper.getKind(entity) + " " + namespace + "/" + name + " to replicas: " + replicas);
                scalable.scale(replicas, true);
            }
        }
    }

    public static void deleteEntities(KubernetesClient kubernetes, String namespace, Collection<HasMetadata> entities, KitLogger log) {
        List<HasMetadata> list = new ArrayList<>(entities);

        // lets delete in reverse order
        Collections.reverse(list);

        for (HasMetadata entity : list) {
            log.info("Deleting resource " + KubernetesHelper.getKind(entity) + " " + namespace + "/" + KubernetesHelper.getName(entity));
            kubernetes.resource(entity).inNamespace(namespace).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
        }
    }

    public static void deleteOpenShiftEntities(KubernetesClient kubernetes, String namespace, Collection<HasMetadata> entities, String s2iBuildNameSuffix, KitLogger log) {
        // For OpenShift cluster, also delete s2i buildconfig
        OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kubernetes);
        if (openshiftClient == null) {
            return;
        }
        for (HasMetadata entity : entities) {
            if ("ImageStream".equals(KubernetesHelper.getKind(entity))) {
                ImageName imageName = new ImageName(entity.getMetadata().getName());
                String buildName = getS2IBuildName(imageName, s2iBuildNameSuffix);
                log.info("Deleting resource BuildConfig %s/%s and Builds", namespace, buildName);
                openshiftClient.builds().inNamespace(namespace).withLabel("buildconfig", buildName).delete();
                openshiftClient.buildConfigs().inNamespace(namespace).withName(buildName).delete();
            }
        }
    }

    private static String getS2IBuildName(ImageName imageName, String s2iBuildNameSuffix) {
        return imageName.getSimpleName() + s2iBuildNameSuffix;
    }

    public static String getPodStatusDescription(Pod pod) {
        return KubernetesHelper.getPodPhase(pod) + " " + getPodCondition(pod);
    }

    public static String getPodStatusMessagePostfix(Watcher.Action action) {
        String message = "";
        switch (action) {
            case DELETED:
                message = ": Pod Deleted";
                break;
            case ERROR:
                message = ": Error";
                break;
        }
        return message;
    }

    protected static String getPodCondition(Pod pod) {
        PodStatus podStatus = pod.getStatus();
        if (podStatus == null) {
            return "";
        }
        List<PodCondition> conditions = podStatus.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }


        for (PodCondition condition : conditions) {
            String type = condition.getType();
            if (StringUtils.isNotBlank(type)) {
                if ("ready".equalsIgnoreCase(type)) {
                    String statusText = condition.getStatus();
                    if (StringUtils.isNotBlank(statusText)) {
                        if (Boolean.parseBoolean(statusText)) {
                            return type;
                        }
                    }
                }
            }
        }
        return "";
    }

    public static GenericKubernetesResource doGetCustomResource(KubernetesClient kubernetesClient, GenericKubernetesResource resource, String namespace) {
        try {
            return kubernetesClient.genericKubernetesResources(resource.getApiVersion(), resource.getKind()).inNamespace(namespace).withName(resource.getMetadata().getName()).get();
        } catch (Exception exception) { // Not found exception
            return null;
        }
    }

    public static void doDeleteAndWait(KubernetesClient kubernetesClient, GenericKubernetesResource resource,
                                       String namespace, long seconds) {
        final Resource<GenericKubernetesResource> crClient = kubernetesClient.genericKubernetesResources(resource.getApiVersion(), resource.getKind())
                .inNamespace(namespace)
                .withName(resource.getMetadata().getName());
        crClient.delete();
        crClient.waitUntilCondition(Objects::isNull, seconds, TimeUnit.SECONDS);
    }

    public static String applicableNamespace(HasMetadata resource, String namespace, ResourceConfig resourceConfig, ClusterAccess clusterAccess) {
        return applicableNamespace(resource, namespace, resolveFallbackNamespace(resourceConfig, clusterAccess));
    }

    public static String applicableNamespace(HasMetadata resource, String namespace, String fallbackNamespace) {
        if (StringUtils.isNotBlank(namespace)) {
            return namespace;
        }
        if (resource != null && StringUtils.isNotBlank(KubernetesHelper.getNamespace(resource))) {
            return KubernetesHelper.getNamespace(resource);
        }
        return StringUtils.isNotBlank(fallbackNamespace) ? fallbackNamespace : KubernetesHelper.getDefaultNamespace();
    }

    public static String resolveFallbackNamespace(ResourceConfig resourceConfig, ClusterAccess clusterAccess) {
        return Optional.ofNullable(resourceConfig)
            .map(ResourceConfig::getNamespace)
            .orElse(Optional.ofNullable(clusterAccess)
                .map(ClusterAccess::getNamespace)
                .orElse(null));
    }
}
