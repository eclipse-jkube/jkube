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
package io.jkube.maven.plugin.mojo.develop;

import com.google.common.base.Objects;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
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
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.KubernetesHelper;
import io.jkube.kit.config.service.JkubeServiceException;
import io.jkube.kit.config.service.PortForwardService;
import io.jkube.maven.enricher.api.util.DebugConstants;
import io.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import io.jkube.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import static io.jkube.kit.common.util.KubernetesHelper.getName;
import static io.jkube.kit.common.util.KubernetesHelper.getPodLabelSelector;
import static io.jkube.kit.common.util.KubernetesHelper.withSelector;
import static io.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusDescription;
import static io.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusMessagePostfix;

/**
 * Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest pod
 * from your IDE
 */
@Mojo(name = "debug", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class DebugMojo extends ApplyMojo {

    @Parameter(property = "jkube.debug.port", defaultValue = "5005")
    private String localDebugPort;

    @Parameter(property = "jkube.debug.suspend", defaultValue = "false")
    private boolean debugSuspend;

    private String remoteDebugPort = DebugConstants.ENV_VAR_JAVA_DEBUG_PORT_DEFAULT;
    private Watch podWatcher;
    private CountDownLatch terminateLatch = new CountDownLatch(1);
    private Pod foundPod;
    private KitLogger podWaitLog;
    private String debugSuspendValue;
    private PortForwardService portForwardService;

    @Override
    protected void initServices(KubernetesClient kubernetes, KitLogger log) {
        portForwardService = new PortForwardService(kubernetes, log);
    }

    protected void applyEntities(KubernetesClient kubernetes, String namespace, String fileName, Set<HasMetadata> entities) throws Exception {
        LabelSelector firstSelector = null;
        for (HasMetadata entity : entities) {
            String name = getName(entity);
            LabelSelector selector = null;
            if (entity instanceof Deployment) {
                Deployment resource = (Deployment) entity;
                DeploymentSpec spec = resource.getSpec();
                if (spec != null) {
                    if (enableDebugging(entity, spec.getTemplate())) {
                        kubernetes.extensions().deployments().inNamespace(namespace).withName(name).replace(resource);
                    }
                    selector = getPodLabelSelector(entity);
                }
            } else if (entity instanceof ReplicaSet) {
                ReplicaSet resource = (ReplicaSet) entity;
                ReplicaSetSpec spec = resource.getSpec();
                if (spec != null) {
                    if (enableDebugging(entity, spec.getTemplate())) {
                        kubernetes.extensions().replicaSets().inNamespace(namespace).withName(name).replace(resource);
                    }
                    selector = getPodLabelSelector(entity);
                }
            } else if (entity instanceof ReplicationController) {
                ReplicationController resource = (ReplicationController) entity;
                ReplicationControllerSpec spec = resource.getSpec();
                if (spec != null) {
                    if (enableDebugging(entity, spec.getTemplate())) {
                        kubernetes.replicationControllers().inNamespace(namespace).withName(name).replace(resource);
                    }
                    selector = getPodLabelSelector(entity);
                }
            } else if (entity instanceof DeploymentConfig) {
                DeploymentConfig resource = (DeploymentConfig) entity;
                DeploymentConfigSpec spec = resource.getSpec();
                if (spec != null) {
                    if (enableDebugging(entity, spec.getTemplate())) {
                        OpenShiftClient openshiftClient = applyService.getOpenShiftClient();
                        if (openshiftClient == null) {
                            log.warn("Ignoring DeploymentConfig %s as not connected to an OpenShift cluster", name);
                            continue;
                        }
                        openshiftClient.deploymentConfigs().inNamespace(namespace).withName(name).replace(resource);
                    }
                    selector = getPodLabelSelector(entity);
                }
            }
            if (selector != null) {
                firstSelector = selector;
            } else {
                applyService.apply(entity, fileName);
            }
        }
        if (firstSelector != null) {
            Map<String, String> envVars = new TreeMap<>();
            envVars.put(DebugConstants.ENV_VAR_JAVA_DEBUG, "true");
            envVars.put(DebugConstants.ENV_VAR_JAVA_DEBUG_SUSPEND, String.valueOf(this.debugSuspend));
            if (this.debugSuspendValue != null) {
                envVars.put(DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION, this.debugSuspendValue);
            }

            String podName = waitForRunningPodWithEnvVar(kubernetes, namespace, firstSelector, envVars);
            portForward(podName);
        }
    }

    private String waitForRunningPodWithEnvVar(final KubernetesClient kubernetes, final String namespace, LabelSelector selector, final Map<String, String> envVars) throws MojoExecutionException {
        //  wait for the newest pod to be ready with the given env var
        FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> pods = withSelector(kubernetes.pods().inNamespace(namespace), selector, log);
        log.info("Waiting for debug pod with selector " + selector + " and environment variables " + envVars);
        podWaitLog = createExternalProcessLogger("[[Y]][W][[Y]] ");
        PodList list = pods.list();
        if (list != null) {
            Pod latestPod = KubernetesResourceUtil.getNewestPod(list.getItems());
            if (latestPod != null && podHasEnvVars(latestPod, envVars)) {
                return getName(latestPod);
            }
        }
        podWatcher = pods.watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                podWaitLog.info(getName(pod) + " status: " + getPodStatusDescription(pod) + getPodStatusMessagePostfix(action));

                if (isAddOrModified(action) && KubernetesHelper.isPodRunning(pod) && KubernetesHelper.isPodReady(pod) &&
                        podHasEnvVars(pod, envVars)) {
                    foundPod = pod;
                    terminateLatch.countDown();
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
                // ignore

            }
        });

        // now lets wait forever?
        while (terminateLatch.getCount() > 0) {
            try {
                terminateLatch.await();
            } catch (InterruptedException e) {
                // ignore
            }
            if (foundPod != null) {
                return getName(foundPod);
            }
        }
        throw new MojoExecutionException("Could not find a running pod with environment variables " + envVars);
    }

    private boolean isAddOrModified(Watcher.Action action) {
        return action.equals(Watcher.Action.ADDED) || action.equals(Watcher.Action.MODIFIED);
    }

    private boolean podHasEnvVars(Pod pod, Map<String, String> envVars) {
        for (String envVarName : envVars.keySet()) {
            String envVarValue = envVars.get(envVarName);
            if (!podHasEnvVarValue(pod, envVarName, envVarValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean podHasEnvVarValue(Pod pod, String envVarName, String envVarValue) {
        PodSpec spec = pod.getSpec();
        if (spec != null) {
            List<Container> containers = spec.getContainers();
            if (containers != null && !containers.isEmpty()) {
                Container container = containers.get(0);
                List<EnvVar> env = container.getEnv();
                if (env != null) {
                    for (EnvVar envVar : env) {
                        if (Objects.equal(envVar.getName(), envVarName) && Objects.equal(envVar.getValue(), envVarValue)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    private void portForward(String podName) throws MojoExecutionException {
        try {
            portForwardService.forwardPort(createExternalProcessLogger("[[B]]port-forward[[B]] "), podName, portToInt(remoteDebugPort, "remoteDebugPort"), portToInt(localDebugPort, "localDebugPort"));

            log.info("");
            log.info("Now you can start a Remote debug execution in your IDE by using localhost and the debug port " + localDebugPort);
            log.info("");

        } catch (JkubeServiceException e) {
            throw new MojoExecutionException("Failed to start port forwarding" + e, e);
        }
    }

    private boolean enableDebugging(HasMetadata entity, PodTemplateSpec template) {
        if (template != null) {
            PodSpec podSpec = template.getSpec();
            if (podSpec != null) {
                List<Container> containers = podSpec.getContainers();
                boolean enabled = false;
                for (int i = 0; i < containers.size(); i++) {
                    Container container = containers.get(i);
                    List<EnvVar> env = container.getEnv();
                    if (env == null) {
                        env = new ArrayList<>();
                    }
                    remoteDebugPort = KubernetesResourceUtil.getEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_PORT, DebugConstants.ENV_VAR_JAVA_DEBUG_PORT_DEFAULT);
                    if (KubernetesResourceUtil.setEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG, "true")) {
                        container.setEnv(env);
                        enabled = true;
                    }
                    if (KubernetesResourceUtil.setEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_SUSPEND, String.valueOf(debugSuspend))) {
                        container.setEnv(env);
                        enabled = true;
                    }
                    List<ContainerPort> ports = container.getPorts();
                    if (ports == null) {
                        ports = new ArrayList<>();
                    }
                    if (KubernetesResourceUtil.addPort(ports, remoteDebugPort, "debug", log)) {
                        container.setPorts(ports);
                        enabled = true;
                    }
                    if (debugSuspend) {
                        // Setting a random session value to force pod restart
                        this.debugSuspendValue = String.valueOf(new Random().nextLong());
                        KubernetesResourceUtil.setEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION, this.debugSuspendValue);
                        container.setEnv(env);
                        if (container.getReadinessProbe() != null) {
                            log.info("Readiness probe will be disabled on " + KubernetesHelper.getKind(entity) + " " + getName(entity) + " to allow attaching a remote debugger during suspension");
                            container.setReadinessProbe(null);
                        }
                        enabled = true;
                    } else {
                        if (KubernetesResourceUtil.removeEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION)) {
                            container.setEnv(env);
                            enabled = true;
                        }
                    }
                }
                if (enabled) {
                    log.info("Enabling debug on " + KubernetesHelper.getKind(entity) + " " + getName(entity));
                    return true;
                }
            }
        }
        return false;
    }

    private int portToInt(String port, String name) throws MojoExecutionException {
        try {
            int portInt = Integer.parseInt(port);
            return portInt;
        } catch (Exception e) {
            throw new MojoExecutionException("Invalid port value: " + name +"=" + port);
        }
    }

}