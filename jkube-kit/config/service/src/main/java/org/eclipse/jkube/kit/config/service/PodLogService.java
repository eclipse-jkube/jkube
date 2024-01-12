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
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.withSelector;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.deleteEntities;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.deleteOpenShiftEntities;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.resizeApp;

/**
 * Prints to the console the output of the pods.
 */
public class PodLogService {

    public static final String OPERATION_UNDEPLOY = "undeploy";
    public static final String OPERATION_STOP = "stop";

    private final PodLogServiceContext context;
    private final KitLogger log;

    private SharedIndexInformer<Pod> podInformer;

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
            if (StringUtils.isNotBlank(onExitOperation)) {
                final String onExitOperationLower = onExitOperation.toLowerCase().trim();
                if (!onExitOperationLower.equals(OPERATION_UNDEPLOY) && !onExitOperationLower.equals(OPERATION_STOP)) {
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
                        if (podInformer != null) {
                            podInformer.close();
                        }
                    }
                });
            }
            waitAndLogPods(nsKubernetesClient, selector, watchAddedPodsOnly, onExitOperation, followLog, ignorePodsOlderThan, waitInCurrentThread);
        } else {
            log.warn("No selector detected and no Pod name specified, cannot watch Pods!");
        }
    }

    private void waitAndLogPods(final NamespacedKubernetesClient kc, LabelSelector selector, final boolean watchAddedPodsOnly, String onExitOperation, final boolean
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
                                Instant podCreateTime = KubernetesHelper.getCreationTimestamp(pod);
                                if (podCreateTime != null && Date.from(podCreateTime).compareTo(ignorePodsOlderThan) > 0) {
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
        if (!watchAddedPodsOnly && !runningPod) {
            log.warn("No pod is running yet. Are you sure you deployed your app using Eclipse JKube apply/deploy mechanism?");
            log.warn("Or did you undeploy it? If so try running the Eclipse JKube apply/deploy tasks again.");
        }
        final PodLogEventHandler podLogEventHandler = new PodLogEventHandler(context, kc, onExitOperation, followLog);
        podInformer = pods.inform(podLogEventHandler);
        podInformer.stopped().whenComplete((v, t) -> podLogEventHandler.close());
        if (waitInCurrentThread && !podLogEventHandler.getLogsRetrieved().isDone()) {
            AsyncUtil.get(podLogEventHandler.getLogsRetrieved());
        }
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
