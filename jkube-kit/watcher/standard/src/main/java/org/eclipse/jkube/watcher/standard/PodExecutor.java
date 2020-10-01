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
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.build.service.docker.watch.WatchException;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import okhttp3.Response;

public class PodExecutor {

  private final ClusterAccess clusterAccess;
  private final InputStream readingInput;
  private final Duration waitTimeout;
  private final Consumer<Response> onOpen;
  private final ObjectMapper objectMapper;
  private String output;

  public PodExecutor(ClusterAccess clusterAccess, Duration waitTimeout) {
    this(clusterAccess, null, waitTimeout, null);
  }

  public PodExecutor(ClusterAccess clusterAccess, InputStream readingInput, Duration waitTimeout, Consumer<Response> onOpen) {
    this.clusterAccess = clusterAccess;
    this.readingInput = readingInput;
    this.waitTimeout = waitTimeout;
    this.onOpen = onOpen;
    objectMapper = new ObjectMapper();
  }

  void executeCommandInPod(Set<HasMetadata> resources, String command) throws IOException, InterruptedException, WatchException {
    try (
        KubernetesClient client = clusterAccess.createDefaultClient();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream()
    ) {
      String namespace = clusterAccess.getNamespace();
      final ExecListenerLatch latch = new ExecListenerLatch(onOpen);
      ExecWatch execWatch = client.pods().inNamespace(namespace)
          .withName(KubernetesHelper.getNewestApplicationPodName(client, namespace, resources))
          .readingInput(readingInput)
          .writingOutput(outputStream)
          .writingError(outputStream)
          .writingErrorChannel(error)
          .usingListener(latch)
          .exec("sh", "-c", command);
      final boolean completed = latch.await(waitTimeout.toMillis(), TimeUnit.MILLISECONDS);
      execWatch.close();
      output = outputStream.toString();
      if (!completed) {
        throw new WatchException("Command execution timed out");
      }
      if (latch.getCloseCode() != 1000) {
        throw new WatchException("Command execution socket closed unexpectedly " + latch.getCloseReason());
      }
      final Map<String, Object> status = objectMapper.readValue(error.toString(), Map.class);
      if (status.getOrDefault("status", "Failure").equals("Failure")) {
        throw new WatchException("Command execution failed: " + status.getOrDefault("message", ""));
      }
    } catch (KubernetesClientException e) {
      throw new WatchException("Execution failed due to a KubernetesClient error: " + e.getMessage(), e);
    }
  }

  public String getOutput() {
    return output;
  }
}
