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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
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

    public static void resizeApp(NamespacedKubernetesClient kubernetes, Collection<HasMetadata> entities, int replicas, KitLogger log) {
        for (HasMetadata entity : entities) {
            String name = KubernetesHelper.getName(entity);
            ScalableResource<?> scalable = null;
            if (entity instanceof Deployment) {
                scalable = kubernetes.apps().deployments().withName(name);
            } else if (entity instanceof ReplicaSet) {
                scalable = kubernetes.apps().replicaSets().withName(name);
            } else if (entity instanceof ReplicationController) {
                scalable = kubernetes.replicationControllers().withName(name);
            } else if (entity instanceof DeploymentConfig) {
                OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kubernetes);
                if (openshiftClient == null) {
                    log.warn("Ignoring DeploymentConfig %s as not connected to an OpenShift cluster", name);
                    continue;
                }
                scalable = openshiftClient.deploymentConfigs().withName(name);
            }
            if (scalable != null) {
                log.info("Scaling " + KubernetesHelper.getKind(entity) + " " + kubernetes.getNamespace() + "/" + name + " to replicas: " + replicas);
                scalable.scale(replicas, true);
            }
        }
    }

    public static void deleteEntities(NamespacedKubernetesClient kc, Collection<HasMetadata> entities, KitLogger log) {
        List<HasMetadata> list = new ArrayList<>(entities);

        // lets delete in reverse order
        Collections.reverse(list);

        for (HasMetadata entity : list) {
            log.info("Deleting resource " + KubernetesHelper.getKind(entity) + " " + kc.getNamespace() + "/" + KubernetesHelper.getName(entity));
            kc.resource(entity).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
        }
    }

    public static void deleteOpenShiftEntities(NamespacedKubernetesClient kc, Collection<HasMetadata> entities, String s2iBuildNameSuffix, KitLogger log) {
        // For OpenShift cluster, also delete s2i buildconfig
        OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kc);
        if (openshiftClient == null) {
            return;
        }
        for (HasMetadata entity : entities) {
            if ("ImageStream".equals(KubernetesHelper.getKind(entity))) {
                ImageName imageName = new ImageName(entity.getMetadata().getName());
                String buildName = getS2IBuildName(imageName, s2iBuildNameSuffix);
                log.info("Deleting resource BuildConfig %s/%s and Builds", kc.getNamespace(), buildName);
                openshiftClient.builds().withLabel("buildconfig", buildName).delete();
                openshiftClient.buildConfigs().withName(buildName).delete();
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

    public static ResourceConfig updateResourceConfigNamespace(String namespaceProvidedViaProperty, ResourceConfig resourceConfig) {
        String resolvedNamespace = Optional.ofNullable(namespaceProvidedViaProperty)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .orElse(null);
        if (resourceConfig == null) {
            resourceConfig = ResourceConfig.builder().namespace(resolvedNamespace).build();
        } else if (resolvedNamespace != null) {
            resourceConfig = ResourceConfig.toBuilder(resourceConfig).namespace(resolvedNamespace).build();
        }
        return resourceConfig;
    }
}
