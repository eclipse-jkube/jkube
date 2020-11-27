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
package org.eclipse.jkube.kit.config.service;

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
import org.eclipse.jkube.kit.common.DebugConstants;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getName;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getPodLabelSelector;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.withSelector;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusDescription;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusMessagePostfix;

public class DebugService {
    private final KitLogger log;
    private final PortForwardService portForwardService;
    private Pod foundPod;
    private final ApplyService applyService;
    private String debugSuspendValue;
    private String remoteDebugPort = DebugConstants.ENV_VAR_JAVA_DEBUG_PORT_DEFAULT;
    private final CountDownLatch terminateLatch = new CountDownLatch(1);

    public DebugService(KitLogger log, PortForwardService portFwdSvc, ApplyService applySvc) {
        this.log = log;
        this.portForwardService = portFwdSvc;
        this.applyService = applySvc;
    }

    public void debug(KubernetesClient kubernetes, String namespace, String fileName, Set<HasMetadata> entities, String localDebugPort, boolean debugSuspend, KitLogger podWaitLog) {
        LabelSelector firstSelector = null;
        for (HasMetadata entity : entities) {
            String name = getName(entity);
            LabelSelector selector = getLabelSelectorsFromHasMetadata(entity, kubernetes, namespace, name, debugSuspend);
            if (selector != null) {
                firstSelector = selector;
            } else {
                applyService.apply(entity, fileName);
            }
            getPodNameFromLabelSelectorAndPortForward(firstSelector, kubernetes, namespace, debugSuspend, localDebugPort, podWaitLog);
        }
    }

    private void getPodNameFromLabelSelectorAndPortForward(LabelSelector firstSelector, KubernetesClient kubernetes, String namespace, boolean debugSuspend, String localDebugPort, KitLogger podWaitLog) {
        if (firstSelector != null) {
            Map<String, String> envVars = getDebugEnvVarsMap(debugSuspend);

            String podName = waitForRunningPodWithEnvVar(kubernetes, namespace, firstSelector, envVars, podWaitLog);
            portForward(podName, namespace, localDebugPort);
        }
    }

    Map<String, String> getDebugEnvVarsMap(boolean debugSuspend) {
        Map<String, String> envVars = new TreeMap<>();
        envVars.put(DebugConstants.ENV_VAR_JAVA_DEBUG, "true");
        envVars.put(DebugConstants.ENV_VAR_JAVA_DEBUG_SUSPEND, String.valueOf(debugSuspend));
        if (this.debugSuspendValue != null) {
            envVars.put(DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION, this.debugSuspendValue);
        }
        return envVars;
    }

    LabelSelector getLabelSelectorsFromHasMetadata(HasMetadata entity, KubernetesClient kubernetes, String namespace, String name, boolean debugSuspend) {
        LabelSelector selector = null;
        if (entity instanceof Deployment) {
            selector = getLabelSelectorForDeployment(kubernetes, namespace, entity, name, selector, debugSuspend);
        } else if (entity instanceof ReplicaSet) {
            selector = getLabelSelectorForReplicaSet(kubernetes, namespace, entity, name, selector, debugSuspend);
        } else if (entity instanceof ReplicationController) {
            selector = getLabelSelectorForReplicationController(kubernetes, namespace, entity, name, selector, debugSuspend);
        } else if (entity instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) entity;
            DeploymentConfigSpec spec = resource.getSpec();
            if (spec != null) {
                if (enableDebugging(entity, spec.getTemplate(), debugSuspend)) {
                    OpenShiftClient openshiftClient = applyService.getOpenShiftClient();
                    if (openshiftClient == null) {
                        log.warn("Ignoring DeploymentConfig %s as not connected to an OpenShift cluster", name);
                        return null;
                    }
                    openshiftClient.deploymentConfigs().inNamespace(namespace).withName(name).replace(resource);
                }
                selector = getPodLabelSelector(entity);
            }
        }
        return selector;
    }

    private LabelSelector getLabelSelectorForReplicationController(KubernetesClient kubernetes, String namespace, HasMetadata entity, String name, LabelSelector selector, boolean debugSuspend) {
        ReplicationController resource = (ReplicationController) entity;
        ReplicationControllerSpec spec = resource.getSpec();
        if (spec != null) {
            if (enableDebugging(entity, spec.getTemplate(), debugSuspend)) {
                kubernetes.replicationControllers().inNamespace(namespace).withName(name).replace(resource);
            }
            selector = getPodLabelSelector(entity);
        }
        return selector;
    }

    private LabelSelector getLabelSelectorForReplicaSet(KubernetesClient kubernetes, String namespace, HasMetadata entity, String name, LabelSelector selector, boolean debugSuspend) {
        ReplicaSet resource = (ReplicaSet) entity;
        ReplicaSetSpec spec = resource.getSpec();
        if (spec != null) {
            if (enableDebugging(entity, spec.getTemplate(), debugSuspend)) {
                kubernetes.apps().replicaSets().inNamespace(namespace).withName(name).replace(resource);
            }
            selector = getPodLabelSelector(entity);
        }
        return selector;
    }

    private LabelSelector getLabelSelectorForDeployment(KubernetesClient kubernetes, String namespace, HasMetadata entity, String name, LabelSelector selector, boolean debugSuspend) {
        Deployment resource = (Deployment) entity;
        DeploymentSpec spec = resource.getSpec();
        if (spec != null) {
            if (enableDebugging(entity, spec.getTemplate(), debugSuspend)) {
                kubernetes.apps().deployments().inNamespace(namespace).withName(name).replace(resource);
            }
            selector = getPodLabelSelector(entity);
        }
        return selector;
    }

    private String waitForRunningPodWithEnvVar(final KubernetesClient kubernetes, final String namespace, LabelSelector selector, final Map<String, String> envVars, KitLogger podWaitLog) {
        //  wait for the newest pod to be ready with the given env var
        FilterWatchListDeletable<Pod, PodList, Boolean, Watch> pods = withSelector(kubernetes.pods().inNamespace(namespace), selector, log);
        log.info("Waiting for debug pod with selector " + selector + " and environment variables " + envVars);
        PodList list = pods.list();
        if (list != null) {
            Pod latestPod = KubernetesHelper.getNewestPod(list.getItems());
            if (latestPod != null && podHasEnvVars(latestPod, envVars)) {
                return getName(latestPod);
            }
        }
        PortForwardPodWatcher portForwardPodWatcher = new PortForwardPodWatcher(podWaitLog, envVars, foundPod, terminateLatch);
        pods.watch(portForwardPodWatcher);

        // now lets wait forever?
        while (portForwardPodWatcher.getTerminateLatch().getCount() > 0) {
            try {
                portForwardPodWatcher.getTerminateLatch().await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (portForwardPodWatcher.getFoundPod() != null) {
                return getName(portForwardPodWatcher.getFoundPod());
            }
        }
        throw new IllegalStateException("Could not find a running pod with environment variables " + envVars);
    }


    void portForward(String podName, String namespace, String localDebugPort) {
        try {
            portForwardService.forwardPort(podName, namespace, portToInt(remoteDebugPort, "remoteDebugPort"), portToInt(localDebugPort, "localDebugPort"));

            log.info("");
            log.info("Now you can start a Remote debug execution in your IDE by using localhost and the debug port " + localDebugPort);
            log.info("");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to start port forwarding" + e, e);
        }
    }

    private boolean enableDebugging(HasMetadata entity, PodTemplateSpec template, boolean debugSuspend) {
        if (template != null) {
            PodSpec podSpec = template.getSpec();
            if (podSpec != null) {
                List<Container> containers = podSpec.getContainers();
                boolean enabled = false;
                for (Container container : containers) {
                    enabled |= setDebugEnvVar(container, debugSuspend);
                    enabled |= addDebugContainerPort(container);
                    enabled |= handleDebugSuspendEnvVar(container, debugSuspend, entity);
                }
                if (enabled) {
                    log.info("Enabling debug on " + KubernetesHelper.getKind(entity) + " " + getName(entity));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean addDebugContainerPort(Container container) {
        List<ContainerPort> ports = container.getPorts();
        boolean enabled = false;
        if (ports == null) {
            ports = new ArrayList<>();
        }
        if (KubernetesHelper.addPort(ports, remoteDebugPort, "debug", log)) {
            container.setPorts(ports);
            enabled = true;
        }
        return enabled;
    }

    private boolean setDebugEnvVar(Container container, boolean debugSuspend) {
        List<EnvVar> env = container.getEnv();
        boolean enabled = false;
        if (env == null) {
            env = new ArrayList<>();
        }
        remoteDebugPort = KubernetesHelper.getEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_PORT, DebugConstants.ENV_VAR_JAVA_DEBUG_PORT_DEFAULT);
        if (KubernetesHelper.setEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG, "true")) {
            container.setEnv(env);
            enabled = true;
        }
        if (KubernetesHelper.setEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_SUSPEND, String.valueOf(debugSuspend))) {
            container.setEnv(env);
            enabled = true;
        }
        return enabled;
    }

    private boolean handleDebugSuspendEnvVar(Container container, boolean debugSuspend, HasMetadata entity) {
        List<EnvVar> env = container.getEnv();
        if (debugSuspend) {
            // Setting a random session value to force pod restart
            this.debugSuspendValue = String.valueOf(new SecureRandom().nextLong());
            KubernetesHelper.setEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION, this.debugSuspendValue);
            container.setEnv(env);
            if (container.getReadinessProbe() != null) {
                log.info("Readiness probe will be disabled on " + KubernetesHelper.getKind(entity) + " " + getName(entity) + " to allow attaching a remote debugger during suspension");
                container.setReadinessProbe(null);
            }
            return true;
        } else {
            if (KubernetesHelper.removeEnvVar(env, DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION)) {
                container.setEnv(env);
                return true;
            }
        }
        return false;
    }

    private int portToInt(String port, String name) {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid port value: " + name +"=" + port);
        }
    }


    static boolean podHasEnvVars(Pod pod, Map<String, String> envVars) {
        for (Map.Entry<String, String> envVar : envVars.entrySet()) {
            if (!podHasEnvVarValue(pod, envVar.getKey(), envVar.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean podHasEnvVarValue(Pod pod, String envVarName, String envVarValue) {
        PodSpec spec = pod.getSpec();
        if (spec != null) {
            List<Container> containers = spec.getContainers();
            if (containers != null && !containers.isEmpty()) {
                Container container = containers.get(0);
                List<EnvVar> env = container.getEnv();
                if (env != null) {
                    return env.stream()
                            .anyMatch(e -> e.getName().equals(envVarName) && e.getValue().equals(envVarValue));
                }
            }
        }
        return false;
    }

    public static class PortForwardPodWatcher implements Watcher<Pod> {
        private final KitLogger podWaitLog;
        private final Map<String, String> envVars;
        private final CountDownLatch terminateLatch;
        private Pod foundPod;

        public PortForwardPodWatcher(KitLogger podWaitLog, Map<String, String> envVars, Pod foundPod, CountDownLatch terminateLatch) {
            this.podWaitLog = podWaitLog;
            this.envVars = envVars;
            this.foundPod = foundPod;
            this.terminateLatch = terminateLatch;
        }

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
        public void onClose(KubernetesClientException e) { }

        private boolean isAddOrModified(Watcher.Action action) {
            return action.equals(Watcher.Action.ADDED) || action.equals(Watcher.Action.MODIFIED);
        }

        public Pod getFoundPod() {
            return this.foundPod;
        }

        public CountDownLatch getTerminateLatch() {
            return this.terminateLatch;
        }
    }
}
