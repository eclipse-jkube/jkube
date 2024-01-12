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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.Loggable;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getName;
import static org.eclipse.jkube.kit.config.service.PodLogService.OPERATION_STOP;
import static org.eclipse.jkube.kit.config.service.PodLogService.OPERATION_UNDEPLOY;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusDescription;

public class PodLogEventHandler implements ResourceEventHandler<Pod>, AutoCloseable {

  private final PodLogService.PodLogServiceContext context;
  private final KubernetesClient kubernetesClient; // TODO, move to context
  private final String onExitOperation;
  private final boolean followLog;
  // When followLog=false,  Used as a latch to signal that the log watch has been terminated
  private final CompletableFuture<Void> logsRetrieved;
  private final ConcurrentMap<String, Pod> activePods;
  private final AtomicReference<LogWatchLogger> currentLogWatch;


  public PodLogEventHandler(PodLogService.PodLogServiceContext context, KubernetesClient kubernetesClient, String onExitOperation, boolean followLog) {
    this.context = context;
    this.kubernetesClient = kubernetesClient;
    this.onExitOperation = onExitOperation;
    this.followLog = followLog;
    activePods = new ConcurrentHashMap<>();
    currentLogWatch = new AtomicReference<>();
    logsRetrieved = new CompletableFuture<>();
  }

  @Override
  public void onAdd(Pod pod) {
    activePods.put(getName(pod), pod);
    logStatus(context.getNewPodLog(), pod);
    podLog();
  }

  @Override
  public void onUpdate(Pod oldPod, Pod newPod) {
    activePods.put(getName(newPod), newPod);
    if (!Objects.equals(getCurrentlyLoggedPodName(), getName(newPod))) {
      logStatus(Objects.equals(getName(mostRecentPod()), getName(newPod)) ? context.getNewPodLog() : context.getOldPodLog(), newPod);
    }
    podLog();
  }

  @Override
  public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
    activePods.remove(getName(pod));
    if (Objects.equals(getCurrentlyLoggedPodName(), getName(pod))) {
      context.getLog().info("Closing log watcher for %s (Deleted)", getCurrentlyLoggedPodName());
      currentLogWatch.getAndSet(null).close();
    }
    logStatus(context.getOldPodLog(), pod,": Pod Deleted");
    podLog();
  }

  @Override
  public void close() {
      if (currentLogWatch.get() != null) {
        currentLogWatch.get().close();
      }
  }

  public final String getCurrentlyLoggedPodName() {
    return currentLogWatch.get() != null ? currentLogWatch.get().podName : null;
  }

  public final CompletableFuture<Void> getLogsRetrieved() {
    return logsRetrieved;
  }

  private void podLog() {
    final Pod pod = mostRecentPod();
    final String podName = getName(pod);
    if (pod == null || !KubernetesHelper.isPodRunning(pod) || Objects.equals(getCurrentlyLoggedPodName(), podName)) {
      return;
    }
    if (currentLogWatch.get() != null) {
      context.getLog().info("Closing log watcher for %s as now watching %s", getCurrentlyLoggedPodName(), podName);
      currentLogWatch.getAndSet(null).close();
    }
    final List<Container> containers = KubernetesHelper.getContainers(pod);
    final String containerName;
    final Loggable loggable;
    if (containers.size() < 2) {
      containerName = containers.isEmpty() ? null : containers.iterator().next().getName();
      loggable = kubernetesClient.pods().withName(podName);
    } else {
      containerName = getLogContainerName(containers);
      loggable = kubernetesClient.pods().withName(podName).inContainer(containerName);
    }
    if (followLog) {
      currentLogWatch.set(watchLog(loggable, podName, containerName));
    } else {
      printLog(loggable, podName, containerName);
    }
  }

  private LogWatchLogger watchLog(Loggable loggable, String podName, String containerName) {
    context.getNewPodLog().info("Tailing log of pod: " + podName + containerNameMessage(containerName));
    context.getNewPodLog().info("Press Ctrl-C to " + computeCtrlCMessage());
    context.getNewPodLog().info("");
    final LogWatch logWatch = loggable.watchLog();
    // It's important to persist this CompletableFuture and not a chained one, this one will allow to stop the log watch
    final CompletableFuture<Void> asyncLogger = KubernetesHelper
      .printLogsAsync(logWatch, line -> context.getLog().info("[[s]]%s", line));
    asyncLogger.whenComplete((v, t) -> {
      if (t != null) {
        context.getLog().error("Failed to read log of Pod %s: %s", podName, t);
      }
    });
    return new LogWatchLogger(logWatch, podName, asyncLogger);
  }

  private void printLog(Loggable loggable, String podName, String containerName) {
    final String logText = loggable.getLog();
    if (logText != null) {
      String[] lines = logText.split("\n");
      context.getLog().info("Log of pod: %s%s", podName, containerNameMessage(containerName));
      context.getLog().info("");
      for (String line : lines) {
        context.getLog().info("[[s]]%s", line);
      }
    }
    logsRetrieved.complete(null);
  }

  private Pod mostRecentPod() {
    return KubernetesHelper.getNewestPod(activePods.values());
  }

  private String getLogContainerName(List<Container> containers) {
    if (StringUtils.isNotBlank(context.getLogContainerName())) {
      for (Container container : containers) {
        if (Objects.equals(context.getLogContainerName(), container.getName())) {
          return context.getLogContainerName();
        }
      }
      context.getLog().error("log container name %s does not exist in pod!! Did you set the correct value for property 'jkube.log.container'", context.getLogContainerName());
    }
    return containers.get(0).getName();
  }

  private String computeCtrlCMessage() {
    if (StringUtils.isNotBlank(onExitOperation)) {
      if (onExitOperation.toLowerCase(Locale.ROOT).equals(OPERATION_UNDEPLOY)) {
        return "undeploy the app";
      } else if (onExitOperation.toLowerCase(Locale.ROOT).equals(OPERATION_STOP)) {
        return "scale down the app and stop tailing the log";
      }
    }
    return  "stop tailing the log";
  }

  private static String containerNameMessage(String containerName) {
    if (StringUtils.isNotBlank(containerName)) {
      return " container: " + containerName;
    }
    return "";
  }

  private static void logStatus(KitLogger logger, Pod pod) {
    logStatus(logger, pod, "");
  }

  private static void logStatus(KitLogger logger, Pod pod, String postfix) {
    logger.info("%s status: %s%s", getName(pod), getPodStatusDescription(pod), postfix);
  }

  private static final class LogWatchLogger implements AutoCloseable {
    private final LogWatch logWatch;
    private final String podName;
    private final CompletableFuture<Void> asyncLogger;


    public LogWatchLogger(LogWatch logWatch, String podName, CompletableFuture<Void> asyncLogger) {
      this.logWatch = logWatch;
      this.podName = podName;
      this.asyncLogger = asyncLogger;
    }

    @Override
    public void close() {
      asyncLogger.complete(null);
      logWatch.close();
    }
  }
}
