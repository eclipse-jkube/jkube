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
package org.eclipse.jkube.watcher.standard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchException;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import org.eclipse.jkube.watcher.api.WatcherContext;

public class PodExecutor {

  private final WatcherContext watcherContext;
  private final Duration waitTimeout;
  private String output;

  public PodExecutor(WatcherContext watcherContext, Duration waitTimeout) {
    this.watcherContext = watcherContext;
    this.waitTimeout = waitTimeout;
  }

  void uploadChangedFilesToPod(Collection<HasMetadata> resources, File changedFilesTarball) throws WatchException {
    try {
      final KubernetesClient client = watcherContext.getJKubeServiceHub().getClient();
      final String namespace = watcherContext.getNamespace();
      File changedFilesDir = new File(changedFilesTarball.getParentFile(), "changed-files");
      File[] changedFiles = changedFilesDir.listFiles();
      if (changedFiles != null && changedFiles.length > 0) {
        PodResource podResource = client.pods()
            .inNamespace(namespace)
            .withName(KubernetesHelper.getNewestApplicationPodName(client, namespace, resources));
        for (File changedFile : changedFiles) {
          if (changedFile.isFile()) {
            podResource.file("/" + FileUtil.getRelativeFilePath(changedFilesDir.getPath(), changedFile.getPath()))
                .upload(changedFile.toPath());
          } else if (changedFile.isDirectory()) {
            podResource.dir("/" + FileUtil.getRelativeFilePath(changedFilesDir.getPath(), changedFile.getPath()))
                .upload(changedFile.toPath());
          }
        }
      }
    } catch (KubernetesClientException kubernetesClientException) {
      throw new WatchException("Error while uploading changed files archive to pod: " + kubernetesClientException.getMessage());
    }
  }

  void executeCommandInPod(Collection<HasMetadata> resources, String command) throws InterruptedException, WatchException, IOException {
    final KubernetesClient client = watcherContext.getJKubeServiceHub().getClient();
    try (
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ) {
      final String namespace = watcherContext.getNamespace();
      final ExecListenerLatch latch = new ExecListenerLatch();
      ExecWatch execWatch = client.pods().inNamespace(namespace)
          .withName(KubernetesHelper.getNewestApplicationPodName(client, namespace, resources))
          .redirectingInput()
          .writingOutput(byteArrayOutputStream)
          .redirectingError()
          .usingListener(latch)
          .exec("sh", "-c", command);
      final boolean completed = latch.await(waitTimeout.toMillis(), TimeUnit.MILLISECONDS);
      execWatch.close();
      output = byteArrayOutputStream.toString();
      if (!completed) {
        throw new WatchException("Command execution timed out");
      }
      if (latch.getCloseCode() != 1000) {
        throw new WatchException("Command execution socket closed unexpectedly " + latch.getCloseReason());
      }
      final Status status = latch.getExitStatus();
      if (status.getStatus().equals("Failure")) {
        throw new WatchException("Command execution failed: " + status.getMessage());
      }
    } catch (KubernetesClientException e) {
      throw new WatchException("Execution failed due to a KubernetesClient error: " + e.getMessage(), e);
    }
  }

  public String getOutput() {
    return output;
  }
}
