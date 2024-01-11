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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.withSelector;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.deleteEntities;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.deleteOpenShiftEntities;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusDescription;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusMessagePostfix;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.resizeApp;

/**
 * Prints to the console the output of the pods.
 */
public class PodLogService {

    public static final String OPERATION_UNDEPLOY = "undeploy";
    public static final String OPERATION_STOP = "stop";

    private final PodLogServiceContext context;
    private final KitLogger log;

    private Watch podWatcher;
    private LogWatch logWatcher;
    private final Map<String, Pod> addedPods = new ConcurrentHashMap<>();
    private final CountDownLatch terminateLatch = new CountDownLatch(1);
    private String watchingPodName;
    private CountDownLatch logWatchTerminateLatch;

    public PodLogService(PodLogServiceContext context) {
        this.context = context;
        this.log = context.getLog();
    }

    public void tailAppPodsLogs(final KubernetesClient kubernetes, final String namespace, final Collection<HasMetadata> entities,
                                boolean watchAddedPodsOnly, String onExitOperation, boolean followLog,
                                Date ignorePodsOlderThan, boolean waitInCurrentThread) {

        final NamespacedKubernetesClient nsKubernetesClient;
        if (namespace != null) {
            nsKubernetesClient = kubernetes.adapt(NamespacedKubernetesClient.class).inNamespace(namespace);
        } else {
            nsKubernetesClient = kubernetes.adapt(NamespacedKubernetesClient.class);
        }
        LabelSelector selector = KubernetesHelper.extractPodLabelSelector(entities);

        if (selector != null || StringUtils.isNotBlank(context.getPodName())) {
            String ctrlCMessage = "stop tailing the log";
            if (StringUtils.isNotBlank(onExitOperation)) {
                final String onExitOperationLower = onExitOperation.toLowerCase().trim();
                if (onExitOperationLower.equals(OPERATION_UNDEPLOY)) {
                    ctrlCMessage = "undeploy the app";
                } else if (onExitOperationLower.equals(OPERATION_STOP)) {
                    ctrlCMessage = "scale down the app and stop tailing the log";
                } else {
                    log.warn("Unknown on-exit command: `%s`", onExitOperationLower);
                }
                resizeApp(nsKubernetesClient, entities, 1, log);
                Runtime.getRuntime().addShutdownHook(new Thread("pod log service shutdown hook") {
                    @Override
                    public void run() {
                        if (onExitOperationLower.equals(OPERATION_UNDEPLOY)) {
                            log.info("Undeploying the app:");
                            deleteEntities(nsKubernetesClient, entities, log);
                            if (context.getS2iBuildNameSuffix() != null) {
                                deleteOpenShiftEntities(nsKubernetesClient, entities, context.getS2iBuildNameSuffix(), log);
                            }
                        } else if (onExitOperationLower.equals(OPERATION_STOP)) {
                            log.info("Stopping the app:");
                            resizeApp(nsKubernetesClient, entities, 0, log);
                        }
                        if (podWatcher != null) {
                            podWatcher.close();
                        }
                        closeLogWatcher();
                    }
                });
            }
            waitAndLogPods(nsKubernetesClient, selector, watchAddedPodsOnly, ctrlCMessage, followLog, ignorePodsOlderThan, waitInCurrentThread);
        } else {
            log.warn("No selector detected and no Pod name specified, cannot watch Pods!");
        }
    }

    private void waitAndLogPods(final NamespacedKubernetesClient kc, LabelSelector selector, final boolean watchAddedPodsOnly, final String ctrlCMessage, final boolean
            followLog, Date ignorePodsOlderThan, boolean waitInCurrentThread) {
        final FilterWatchListDeletable<Pod, PodList, PodResource> pods;
        if (StringUtils.isNotBlank(context.getPodName())) {
            log.info("Watching pod with selector %s, and name %s waiting for a running pod...", selector, context.getPodName());
            pods = kc.pods().withField("metadata.name", context.getPodName());
        } else {
            log.info("Watching pods with selector %s waiting for a running pod...", selector);
            pods =  withSelector(kc.pods(), selector, log);
        }
        Pod latestPod = null;
        boolean runningPod = false;
        PodList list = pods.list();
        if (list != null) {
            List<Pod> items = list.getItems();
            if (items != null) {
                for (Pod pod : items) {
                    if (KubernetesHelper.isPodRunning(pod) || KubernetesHelper.isPodWaiting(pod)) {
                        if (latestPod == null || KubernetesHelper.isNewerResource(pod, latestPod)) {
                            if (ignorePodsOlderThan != null) {
                                Date podCreateTime = KubernetesHelper.getCreationTimestamp(pod);
                                if (podCreateTime != null && podCreateTime.compareTo(ignorePodsOlderThan) > 0) {
                                    latestPod = pod;
                                }
                            } else {
                                latestPod = pod;
                            }
                        }
                        runningPod = true;
                    }
                }
            }
        }
        // we may have missed the ADDED event so lets simulate one
        if (latestPod != null) {
            onPod(Watcher.Action.ADDED, latestPod, kc, ctrlCMessage, followLog);
        }
        if (!watchAddedPodsOnly && !runningPod) {
            log.warn("No pod is running yet. Are you sure you deployed your app using Eclipse JKube apply/deploy mechanism?");
            log.warn("Or did you undeploy it? If so try running the Eclipse JKube apply/deploy tasks again.");
        }
        podWatcher = pods.watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                onPod(action, pod, kc, ctrlCMessage, followLog);
            }

            @Override
            public void onClose(WatcherException e) {
                // ignore
            }
        });

        if (waitInCurrentThread) {
            while (terminateLatch.getCount() > 0) {
                try {
                    terminateLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void onPod(Watcher.Action action, Pod pod, NamespacedKubernetesClient kubernetes, String ctrlCMessage, boolean followLog) {
        String name = KubernetesHelper.getName(pod);
        if (action.equals(Watcher.Action.DELETED)) {
            addedPods.remove(name);
            if (Objects.equals(watchingPodName, name)) {
                watchingPodName = null;
                addedPods.remove(name);
            }
        } else {
            if (action.equals(Watcher.Action.ADDED) || action.equals(Watcher.Action.MODIFIED)) {
                addedPods.put(name, pod);
            }
        }

        Pod watchPod = KubernetesHelper.getNewestPod(addedPods.values());
        String newestPodName = KubernetesHelper.getName(watchPod);

        KitLogger statusLog = Objects.equals(name, newestPodName) ? context.getNewPodLog() : context.getOldPodLog();
        if (!action.equals(Watcher.Action.MODIFIED) || watchingPodName == null || !watchingPodName.equals(name)) {
            statusLog.info("%s status: %s%s", name, getPodStatusDescription(pod), getPodStatusMessagePostfix(action));
        }

        if (watchPod != null && KubernetesHelper.isPodRunning(watchPod)) {
            watchLogOfPodName(kubernetes, ctrlCMessage, followLog, watchPod, KubernetesHelper.getName(watchPod));
        }
    }

    private void watchLogOfPodName(NamespacedKubernetesClient kubernetes, String ctrlCMessage, boolean followLog, Pod pod, String name) {
        if (watchingPodName == null || !watchingPodName.equals(name)) {
            if (logWatcher != null) {
                log.info("Closing log watcher for %s as now watching %s", watchingPodName, name);
                closeLogWatcher();
            }
            PodResource podResource = kubernetes.pods().withName(name);
            List<Container> containers = KubernetesHelper.getContainers(pod);
            String containerName = null;
            if (followLog) {
                watchingPodName = name;
                logWatchTerminateLatch = new CountDownLatch(1);
                if (containers.size() < 2) {
                    logWatcher = podResource.watchLog();
                } else {
                    containerName = getLogContainerName(containers);
                    logWatcher = podResource.inContainer(containerName).watchLog();
                }
                watchLog(logWatcher, name, "Failed to read log of pod " + name + ".", ctrlCMessage, containerName);
            } else {
                String logText;
                if (containers.size() < 2) {
                    logText = podResource.getLog();
                } else {
                    containerName = getLogContainerName(containers);
                    logText = podResource.inContainer(containerName).getLog();
                }
                if (logText != null) {
                    String[] lines = logText.split("\n");
                    log.info("Log of pod: %s%s", name, containerNameMessage(containerName));
                    log.info("");
                    for (String line : lines) {
                        log.info("[[s]]%s", line);
                    }
                }
                terminateLatch.countDown();
            }
        }
    }

    private String getLogContainerName(List<Container> containers) {
        if (StringUtils.isNotBlank(context.getLogContainerName())) {
            for (Container container : containers) {
                if (Objects.equals(context.getLogContainerName(), container.getName())) {
                    return context.getLogContainerName();
                }
            }
            log.error("log container name %s does not exist in pod!! Did you set the correct value for property 'jkube.log.container'", context.getLogContainerName());
        }
        return containers.get(0).getName();
    }

    private void closeLogWatcher() {
        if (logWatcher != null) {
            logWatcher.close();
            logWatcher = null;
        }
        if (logWatchTerminateLatch != null) {
            logWatchTerminateLatch.countDown();
        }
    }

    private void watchLog(final LogWatch logWatcher, String podName, final String failureMessage, String ctrlCMessage, String containerName) {
        context.getNewPodLog().info("Tailing log of pod: " + podName + containerNameMessage(containerName));
        context.getNewPodLog().info("Press Ctrl-C to " + ctrlCMessage);
        context.getNewPodLog().info("");

        KubernetesHelper.printLogsAsync(logWatcher, line -> log.info("[[s]]%s", line))
          .whenComplete((v, t) -> {
              if (t != null) {
                  log.error("%s: %s", failureMessage, t);
              }
          });
    }

    private String containerNameMessage(String containerName) {
        if (StringUtils.isNotBlank(containerName)) {
            return " container: " + containerName;
        }
        return "";
    }

    // =======================================

    @Builder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class PodLogServiceContext {

        private static final String DEFAULT_S2I_BUILD_NAME_SUFFIX = "-s2i";
        private KitLogger log;
        private KitLogger newPodLog;
        private KitLogger oldPodLog;
        private String logContainerName;
        private String podName;
        private String s2iBuildNameSuffix;

        public String getS2iBuildNameSuffix() {
            return Optional.ofNullable(s2iBuildNameSuffix).orElse(DEFAULT_S2I_BUILD_NAME_SUFFIX);
        }

    }

}
