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
package org.eclipse.jkube.watcher.standard;

import java.util.Date;
import java.util.List;
import java.util.Set;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.watcher.api.BaseWatcher;
import org.eclipse.jkube.watcher.api.WatcherContext;

public class DockerImageWatcher extends BaseWatcher {

    public DockerImageWatcher(WatcherContext watcherContext) {
        super(watcherContext, "docker-image");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs, Set<HasMetadata> resources, PlatformMode mode) {
        return mode == PlatformMode.kubernetes;
    }

    @Override
    public void watch(List<ImageConfiguration> configs, final Set<HasMetadata> resources, PlatformMode mode) {

        WatchService.WatchContext watchContext = getContext().getWatchContext();

        // add a image customizer
        watchContext = watchContext.toBuilder()
                .imageCustomizer(this::buildImage).containerRestarter(imageWatcher -> restartContainer(imageWatcher, resources))
                .build();

        ServiceHub hub = getContext().getJKubeServiceHub().getDockerServiceHub();
        try {
            hub.getWatchService().watch(watchContext, getContext().getBuildContext(), configs);
        } catch (Exception ex) {
            throw new RuntimeException("Error while watching", ex);
        }
    }

    protected void buildImage(ImageConfiguration imageConfig) {
        String imageName = imageConfig.getName();
        // lets regenerate the label
        try {
            String imagePrefix = getImagePrefix(imageName);
            imageName = imagePrefix + "%t";
            ImageNameFormatter formatter = new ImageNameFormatter(getContext().getBuildContext().getProject(), new Date());
            imageName = formatter.format(imageName);
            imageConfig.setName(imageName);
            log.info("New image name: " + imageConfig.getName());
        } catch (Exception e) {
            log.error("Caught: " + e, e);
        }
    }

    private String getImagePrefix(String imageName) {
        String imagePrefix = null;
        int idx = imageName.lastIndexOf(':');
        if (idx < 0) {
            throw new IllegalStateException("No ':' in the image name:  " + imageName);
        } else {
            imagePrefix = imageName.substring(0, idx + 1);
        }
        return imagePrefix;
    }

    protected void restartContainer(WatchService.ImageWatcher watcher, Set<HasMetadata> resources) {
        ImageConfiguration imageConfig = watcher.getImageConfiguration();
        String imageName = imageConfig.getName();
        ClusterAccess clusterAccess = getContext().getJKubeServiceHub().getClusterAccess();
        try (KubernetesClient client = clusterAccess.createDefaultClient()) {

            String namespace = clusterAccess.getNamespace();

            String imagePrefix = getImagePrefix(imageName);
            for (HasMetadata entity : resources) {
                updateImageName(client, namespace, entity, imagePrefix, imageName);
            }
        } catch (KubernetesClientException e) {
            KubernetesHelper.handleKubernetesClientException(e, this.log);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void updateImageName(KubernetesClient kubernetes, String namespace, HasMetadata entity, String imagePrefix, String imageName) {
        String name = KubernetesHelper.getName(entity);
        if (entity instanceof Deployment) {
            Deployment resource = (Deployment) entity;
            DeploymentSpec spec = resource.getSpec();
            if (spec != null) {
                if (updateImageName(entity, spec.getTemplate(), imagePrefix, imageName)) {
                    kubernetes.apps().deployments().inNamespace(namespace).withName(name).replace(resource);
                }
            }
        } else if (entity instanceof ReplicaSet) {
            ReplicaSet resource = (ReplicaSet) entity;
            ReplicaSetSpec spec = resource.getSpec();
            if (spec != null) {
                if (updateImageName(entity, spec.getTemplate(), imagePrefix, imageName)) {
                    kubernetes.apps().replicaSets().inNamespace(namespace).withName(name).replace(resource);
                }
            }
        } else if (entity instanceof ReplicationController) {
            ReplicationController resource = (ReplicationController) entity;
            ReplicationControllerSpec spec = resource.getSpec();
            if (spec != null) {
                if (updateImageName(entity, spec.getTemplate(), imagePrefix, imageName)) {
                    kubernetes.replicationControllers().inNamespace(namespace).withName(name).replace(resource);
                }
            }
        } else if (entity instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) entity;
            DeploymentConfigSpec spec = resource.getSpec();
            if (spec != null) {
                if (updateImageName(entity, spec.getTemplate(), imagePrefix, imageName)) {
                    OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kubernetes);
                    if (openshiftClient == null) {
                        log.warn("Ignoring DeploymentConfig %s as not connected to an OpenShift cluster", name);
                    } else {
                        openshiftClient.deploymentConfigs().inNamespace(namespace).withName(name).replace(resource);
                    }
                }
            }
        }
    }

    private boolean updateImageName(HasMetadata entity, PodTemplateSpec template, String imagePrefix, String imageName) {
        boolean answer = false;
        PodSpec spec = template.getSpec();
        if (spec != null) {
            List<Container> containers = spec.getContainers();
            if (containers != null) {
                for (Container container : containers) {
                    String image = container.getImage();
                    if (image != null && image.startsWith(imagePrefix)) {
                        container.setImage(imageName);
                        log.info("Updating " + KubernetesHelper.getKind(entity) + " " + KubernetesHelper.getName(entity) + " to use image: " + imageName);
                        answer = true;
                    }
                }
            }
        }
        return answer;
    }
}
