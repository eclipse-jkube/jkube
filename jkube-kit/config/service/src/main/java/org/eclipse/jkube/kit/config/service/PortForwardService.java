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

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A service for forwarding connections to remote pods.
 *
 * @author nicola
 * @since 28/03/2017
 */
public class PortForwardService {

    private final KitLogger log;

    private final KubernetesClient kubernetes;

    public PortForwardService(KubernetesClient kubernetes, KitLogger log) {
        this.log = Objects.requireNonNull(log, "log");
        this.kubernetes = Objects.requireNonNull(kubernetes, "kubernetes");
    }

    /**
     * Forwards a port to the newest pod matching the given selector.
     * If another pod is created, it forwards connections to the new pod once it's ready.
     */
    public Closeable forwardPortAsync(final LabelSelector podSelector, final int remotePort, final int localPort) {

        final Lock monitor = new ReentrantLock(true);
        final Condition podChanged = monitor.newCondition();
        final Pod[] nextForwardedPod = new Pod[1];

        final Thread forwarderThread = new Thread() {
            @Override
            public void run() {

                Pod currentPod = null;
                Closeable currentPortForward = null;

                try {
                    monitor.lock();

                    while (true) {
                        if (podEquals(currentPod, nextForwardedPod[0])) {
                            podChanged.await();
                        } else {
                            Pod nextPod = nextForwardedPod[0]; // may be null
                            try {
                                monitor.unlock();
                                // out of critical section

                                if (currentPortForward != null) {
                                    log.info("Closing port-forward from pod %s", KubernetesHelper.getName(currentPod));
                                    currentPortForward.close();
                                    currentPortForward = null;
                                }

                                if (nextPod != null) {
                                    log.info("Starting port-forward to pod %s", KubernetesHelper.getName(nextPod));
                                    currentPortForward = forwardPortAsync( KubernetesHelper.getName(nextPod), KubernetesHelper.getNamespace(nextPod), remotePort, localPort);
                                } else {
                                    log.info("Waiting for a pod to become ready before starting port-forward");
                                }
                                currentPod = nextPod;
                            } finally {
                                monitor.lock();
                            }
                        }

                    }

                } catch (InterruptedException e) {
                    log.debug("Port-forwarding thread interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.warn("Error while port-forwarding to pod", e);
                } finally {
                    monitor.unlock();

                    if (currentPortForward != null) {
                        try {
                            currentPortForward.close();
                        } catch (Exception e) {}
                    }
                }
            }
        };

        // Switching forward to the current pod if present
        Pod newPod = getNewestPod(podSelector);
        nextForwardedPod[0] = newPod;

        final Watch watch = KubernetesHelper.withSelector(kubernetes.pods(), podSelector, log).watch(new Watcher<Pod>() {

            @Override
            public void eventReceived(Action action, Pod pod) {
                monitor.lock();
                try {
                    List<Pod> candidatePods;
                    if (nextForwardedPod[0] != null) {
                        candidatePods = new LinkedList<>();
                        candidatePods.add(nextForwardedPod[0]);
                        candidatePods.add(pod);
                    } else {
                        candidatePods = Collections.singletonList(pod);
                    }
                    Pod newPod = getNewestPod(candidatePods); // may be null
                    if (!podEquals(nextForwardedPod[0], newPod)) {
                        nextForwardedPod[0] = newPod;
                        podChanged.signal();
                    }
                } finally {
                    monitor.unlock();
                }
            }

            @Override
            public void onClose(WatcherException e) {
                // don't care
            }
        });

        forwarderThread.start();

        final Closeable handle = () -> {
            try {
                watch.close();
            } catch (Exception e) {}
            try {
                forwarderThread.interrupt();
                forwarderThread.join(15000);
            } catch (Exception e) {}
        };
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    handle.close();
                } catch (Exception e) {
                    // suppress
                }
            }
        });

        return handle;
    }

    private boolean podEquals(Pod pod1, Pod pod2) {
        if (pod1 == pod2) {
            return true;
        }
        if (pod1 == null || pod2 == null) {
            return false;
        }
        return KubernetesHelper.getName(pod1).equals(KubernetesHelper.getName(pod2));
    }

    private Pod getNewestPod(LabelSelector selector) {
        FilterWatchListDeletable<Pod, PodList> pods =
                KubernetesHelper.withSelector(kubernetes.pods(), selector, log);

        PodList list = pods.list();
        if (list != null) {
            List<Pod> items = list.getItems();
            return getNewestPod(items);
        }
        return null;
    }

    private Pod getNewestPod(List<Pod> items) {
        Pod targetPod = null;
        if (items != null) {
            for (Pod pod : items) {
                if (KubernetesHelper.isPodWaiting(pod) || KubernetesHelper.isPodRunning(pod)) {
                    if (targetPod == null || (KubernetesHelper.isPodReady(pod) && KubernetesHelper.isNewerResource(pod, targetPod))) {
                        targetPod = pod;
                    }
                }
            }
        }
        return targetPod;
    }

    PortForwardThread forwardPort(String pod, String namespace, int remotePort, int localPort) {
        log.info("Port forwarding to port " + remotePort + " on pod " + pod );
        LocalPortForward localPortForward = forwardPortAsync(pod, namespace, remotePort, localPort);
        log.info("");
        log.info("Now you can start a Remote debug execution in your IDE by using localhost and the debug port " + localPort);
        log.info("");

        return new PortForwardThread(kubernetes, pod, namespace, localPortForward, log);
    }

    LocalPortForward forwardPortAsync(String podName, String namespace, int remotePort, int localPort) {
        return kubernetes.pods().inNamespace(namespace).withName(podName).portForward(localPort, remotePort);
    }

    @Getter
    @AllArgsConstructor
    public static class PortForwardPodMonitorThread extends Thread {
        private final KubernetesClient kubernetesClient;
        private final String podName;
        private final String namespace;
        private final KitLogger logger;

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Pod pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).fromServer().get();
                    if (pod == null) {
                        logger.error("Pod %s no longer available", podName);
                        throw new IllegalStateException("Not able to port forward: Pod used in Port Forward no longer available..");
                    }
                    if (!KubernetesHelper.isPodRunning(pod)) {
                        logger.error("Pod %s no longer in Running state", podName);
                        throw new IllegalStateException("Not able to port forward: Pod no longer in running state");
                    }
                } catch (KubernetesClientException exception) {
                    throw new IllegalStateException("Error in getting Debug Pod details from Kuberntes API");
                }
            }
        }
    }

    @AllArgsConstructor
    @Getter
    public static class PortForwardThread extends Thread {
        private final KubernetesClient kubernetesClient;
        private final String podName;
        private final String namespace;
        private final LocalPortForward localPortForward;
        private final KitLogger logger;
        private final Lock lock = new ReentrantLock();
        private final Condition notInterrupted  = lock.newCondition();

        @Override
        public void run() {
            PortForwardPodMonitorThread monitorThread = new PortForwardPodMonitorThread(kubernetesClient, podName, namespace, logger);
            lock.lock();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    monitorThread.run();
                    notInterrupted.await();
                } catch (InterruptedException interruptedException) {
                    logger.warn("Interrupted : %s", interruptedException.getMessage());
                    monitorThread.interrupt();
                    Thread.currentThread().interrupt();
                    notInterrupted.signal();
                } catch (Exception exception) {
                    logger.warn("Not able to port forward : %s", exception.getMessage());
                    Thread.currentThread().interrupt();
                    notInterrupted.signal();
                } finally {
                    close();
                    lock.unlock();
                }
            }
        }

        private void close() {
            try {
                logger.info("Closing port forward for Debug Session ...");
                localPortForward.close();
            } catch (IOException exception) {
                logger.warn("Not able to close Port forward gracefully : %s", exception.getMessage());
            }
        }
    }
}
