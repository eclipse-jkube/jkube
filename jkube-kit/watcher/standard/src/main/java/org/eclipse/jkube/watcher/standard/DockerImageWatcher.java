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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.TimeUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
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

    private static final int WAIT_TIMEOUT_IN_SECONDS = 5000;

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
                .imageCustomizer(this::buildImage)
                .containerRestarter(imageWatcher -> restartContainer(imageWatcher, resources))
                .containerCommandExecutor(imageWatcher -> executeCommandInPod(imageWatcher, resources))
                .containerCopyTask(f -> copyFileToPod(f, resources))
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
        String imagePrefix;
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

    private String executeCommandInPod(WatchService.ImageWatcher imageWatcher, Set<HasMetadata> resources) {
        ClusterAccess clusterAccess = getContext().getJKubeServiceHub().getClusterAccess();
        return executeCommandInPod(imageWatcher, resources, clusterAccess, this.log, WAIT_TIMEOUT_IN_SECONDS);
    }

    private boolean copyFileToPod(File fileToUpload, Set<HasMetadata> resources) {
        ClusterAccess clusterAccess = getContext().getJKubeServiceHub().getClusterAccess();
        return copyFileToPod(fileToUpload, resources, clusterAccess, this.log, WAIT_TIMEOUT_IN_SECONDS);
    }


    static String executeCommandInPod(WatchService.ImageWatcher imageWatcher, Set<HasMetadata> resources, ClusterAccess clusterAccess, KitLogger logger, int execWaitTimeoutInMillis) {
        try (KubernetesClient client = clusterAccess.createDefaultClient()) {
            String namespace = clusterAccess.getNamespace();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ExecWatch execWatch = client.pods().inNamespace(namespace).withName(KubernetesHelper.getNewestApplicationPodName(client, namespace, resources)).writingOutput(byteArrayOutputStream)
                    .exec(imageWatcher.getPostExec().split("[\\s']"));

            // Wait for at most 5 seconds for Exec to complete
            TimeUtil.waitUntilCondition(() -> byteArrayOutputStream.size() > 0, execWaitTimeoutInMillis);
            String commandOutput = byteArrayOutputStream.toString();
            execWatch.close();
            return commandOutput;
        } catch (KubernetesClientException e) {
            KubernetesHelper.handleKubernetesClientException(e, logger);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    static boolean copyFileToPod(File fileToUpload, Set<HasMetadata> resources, ClusterAccess clusterAccess, KitLogger logger, int execWaitTimeoutInSeconds) {
        try (KubernetesClient client = clusterAccess.createDefaultClient()) {
            String namespace = clusterAccess.getNamespace();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ExecWatch execWatch = client.pods().inNamespace(namespace)
                    .withName(KubernetesHelper.getNewestApplicationPodName(client, namespace, resources))
                    .readingInput(new FileInputStream(fileToUpload))
                    .writingOutput(out)
                    .exec("tar", "-xf", "-", "-C", "/");

            // Wait for at most 5 seconds for Exec to complete
            TimeUtil.waitUntilCondition(() -> out.size() > 0, execWaitTimeoutInSeconds);
            execWatch.close();
            return true;
        } catch (KubernetesClientException e) {
            KubernetesHelper.handleKubernetesClientException(e, logger);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return false;
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
