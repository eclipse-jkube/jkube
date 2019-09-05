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
package io.jshift.watcher.standard;

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
import io.jshift.kit.build.service.docker.BuildService;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.build.service.docker.ServiceHub;
import io.jshift.kit.build.service.docker.WatchService;
import io.jshift.kit.build.service.docker.access.DockerAccessException;
import io.jshift.kit.build.service.docker.helper.ImageNameFormatter;
import io.jshift.kit.common.util.KubernetesHelper;
import io.jshift.kit.common.util.OpenshiftHelper;
import io.jshift.kit.config.access.ClusterAccess;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.watcher.api.BaseWatcher;
import io.jshift.watcher.api.WatcherContext;

/**
 *
 */
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

        BuildService.BuildContext buildContext = getContext().getBuildContext();
        WatchService.WatchContext watchContext = getContext().getWatchContext();

        // add a image customizer
        watchContext = new WatchService.WatchContext.Builder(watchContext)
                .imageCustomizer(imageConfiguration -> buildImage(imageConfiguration)).containerRestarter(imageWatcher -> restartContainer(imageWatcher, resources))
                .build();

        ServiceHub hub = getContext().getServiceHub();
        try {
            hub.getWatchService().watch(watchContext, buildContext, configs);
        } catch (Exception ex) {
            throw new RuntimeException("Error while watching", ex);
        }
    }

    protected void buildImage(ImageConfiguration imageConfig) throws DockerAccessException {
        String imageName = imageConfig.getName();
        // lets regenerate the label
        try {
            String imagePrefix = getImagePrefix(imageName);
            imageName = imagePrefix + "%t";
            ImageNameFormatter formatter = new ImageNameFormatter(getContext().getProject(), new Date());
            imageName = formatter.format(imageName);
            imageConfig.setName(imageName);
            log.info("New image name: " + imageConfig.getName());
        } catch (Exception e) {
            log.error("Caught: " + e, e);
        }

    }

    private String getImagePrefix(String imageName) throws IllegalStateException {
        String imagePrefix = null;
        int idx = imageName.lastIndexOf(':');
        if (idx < 0) {
            throw new IllegalStateException("No ':' in the image name:  " + imageName);
        } else {
            imagePrefix = imageName.substring(0, idx + 1);
        }
        return imagePrefix;
    }

    protected void restartContainer(WatchService.ImageWatcher watcher, Set<HasMetadata> resources) throws IllegalStateException {
        ImageConfiguration imageConfig = watcher.getImageConfiguration();
        String imageName = imageConfig.getName();
        try {
            ClusterAccess clusterAccess = new ClusterAccess(getContext().getClusterConfiguration());
            KubernetesClient client = clusterAccess.createDefaultClient(log);

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
                    kubernetes.extensions().deployments().inNamespace(namespace).withName(name).replace(resource);
                }
            }
        } else if (entity instanceof ReplicaSet) {
            ReplicaSet resource = (ReplicaSet) entity;
            ReplicaSetSpec spec = resource.getSpec();
            if (spec != null) {
                if (updateImageName(entity, spec.getTemplate(), imagePrefix, imageName)) {
                    kubernetes.extensions().replicaSets().inNamespace(namespace).withName(name).replace(resource);
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
                    }
                    openshiftClient.deploymentConfigs().inNamespace(namespace).withName(name).replace(resource);
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
