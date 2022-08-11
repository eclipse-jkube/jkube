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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import org.eclipse.jkube.kit.build.service.docker.watch.WatchException;
import org.eclipse.jkube.kit.config.access.ClusterAccess;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.Execable;
import io.fabric8.kubernetes.client.dsl.TtyExecable;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unused")
class PodExecutorTest {

  @Mocked
  private ClusterAccess clusterAccess;
  @Mocked
  private PodOperationsImpl podOperations;
  @Mocked
  private KubernetesClient kubernetesClient;

  private PodExecutor podExecutor;

  @BeforeEach
  void setUp() {
    // @formatter:off
    new Expectations() {{
      clusterAccess.getNamespace(); result = "default";
      clusterAccess.createDefaultClient(); result = kubernetesClient;
      kubernetesClient.pods().inNamespace(anyString).withName(anyString); result = podOperations;
    }};
    // @formatter:on
    podExecutor = new PodExecutor(clusterAccess, Duration.ZERO);
  }

  @Test
  void executeCommandInPodKubernetesError() {
    // Given
    // @formatter:off
    new Expectations() {{
      kubernetesClient.pods().inNamespace(anyString).withName(anyString); result = new KubernetesClientException("Mocked Error");
    }};
    // @formatter:on
    // When
    final WatchException result = assertThrows(WatchException.class,
        () -> podExecutor.executeCommandInPod(Collections.emptySet(), "sh"));
    // Then
    assertThat(result).hasMessage("Execution failed due to a KubernetesClient error: Mocked Error");
  }

  @Test
  void executeCommandInPodTimeout() {
    // When
    final WatchException result = assertThrows(WatchException.class,
        () -> podExecutor.executeCommandInPod(Collections.emptySet(), "sh"));
    // Then
    assertThat(result).hasMessage("Command execution timed out");
  }

  @Test
  void executeCommandInPodSocketError() {
    // Given
    listenableCloseWithCode(1337);
    // When
    final WatchException result = assertThrows(WatchException.class,
        () -> podExecutor.executeCommandInPod(Collections.emptySet(), "sh"));
    // Then
    assertThat(result).hasMessage("Command execution socket closed unexpectedly Closed by mock");
  }

  private void listenableCloseWithCode(int code) {
    new MockUp<PodOperationsImpl>() {
      @Mock
      public Execable usingListener(ExecListener execListener) {
        execListener.onClose(code, "Closed by mock");
        return podOperations;
      }
    };
  }

  private void withErrorChannelResponse(String response) {
    new MockUp<PodOperationsImpl>() {
      @Mock
      public TtyExecable writingErrorChannel(OutputStream errChannel) {
        try {
          IOUtils.write(response, errChannel, StandardCharsets.UTF_8);
        } catch (IOException e) {
          fail("Couldn't fake ErrorChannelResponse");
        }
        return podOperations;
      }
    };
  }
}
