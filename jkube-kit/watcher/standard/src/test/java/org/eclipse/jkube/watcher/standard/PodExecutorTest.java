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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchException;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PodExecutorTest {
  private KubernetesClient kubernetesClient;
  private PodExecutor podExecutor;
  private PodOperationsImpl podOperations;
  private NonNamespaceOperation<Pod, PodList, PodResource> podNonNamespaceOp;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;

  @BeforeEach
  public void setUp() {
    kubernetesClient = mock(KubernetesClient.class);
    final WatcherContext watcherContext = mock(WatcherContext.class, RETURNS_DEEP_STUBS);
    when(watcherContext.getNamespace()).thenReturn("default");
    when(watcherContext.getJKubeServiceHub().getClient()).thenReturn(kubernetesClient);
    podOperations = mock(PodOperationsImpl.class, RETURNS_DEEP_STUBS);
    podNonNamespaceOp = createPodsInNamespaceMock();
    when(podNonNamespaceOp.withName(anyString())).thenReturn(podOperations);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    kubernetesHelperMockedStatic.when(() -> KubernetesHelper.getNewestApplicationPodName(eq(kubernetesClient), any(), any())).thenReturn("test-pod");
    podExecutor = new PodExecutor(watcherContext, Duration.ZERO);
  }

  @AfterEach
  public void tearDown() {
    kubernetesHelperMockedStatic.close();
  }

  @Test
  void executeCommandInPodKubernetesError() {
    // Given
    when(podNonNamespaceOp.withName(anyString())).thenThrow(new KubernetesClientException("Mocked Error"));
    // When + Then
    assertThatExceptionOfType(WatchException.class)
        .isThrownBy(() -> podExecutor.executeCommandInPod(Collections.emptySet(), "sh"))
        .withMessage("Execution failed due to a KubernetesClient error: Mocked Error");
  }

  @Test
  void executeCommandInPodTimeout() {
    // When + Then
    assertThatExceptionOfType(WatchException.class)
        .isThrownBy(() -> podExecutor.executeCommandInPod(Collections.emptySet(), "sh"))
        .withMessage("Command execution timed out");
  }

  @Test
  void uploadChangedFilesToPod_whenChangedFilesDirEmpty_thenDoNothing(@TempDir File tempDir) throws WatchException {
    // Given
    File changedFilesTarball = createChangedFilesDir(tempDir);

    // When
    podExecutor.uploadChangedFilesToPod(Collections.emptyList(), changedFilesTarball);

    // Then
    verify(kubernetesClient, times(0)).pods();
  }

  @Test
  void uploadChangedFilesToPod_whenChangedFilesDirContainsFile_thenUploadFilesToPod(@TempDir File tempDir) throws WatchException, IOException {
    // Given
    File changedFilesTarball = createChangedFilesDir(tempDir);
    File changedFilesDir = new File(tempDir, "changed-files");
    File changedFile = new File(changedFilesDir, "ROOT.war");
    boolean changedFileCreated = changedFile.createNewFile();
    when(podOperations.file(anyString())).thenReturn(podOperations);
    when(podOperations.upload(any(Path.class))).thenReturn(true);
    ArgumentCaptor<Path> uploadedFilePathCaptor = ArgumentCaptor.forClass(Path.class);

    // When
    podExecutor.uploadChangedFilesToPod(Collections.emptyList(), changedFilesTarball);

    // Then
    assertThat(changedFileCreated).isTrue();
    verify(podOperations).upload(uploadedFilePathCaptor.capture());
    assertThat(uploadedFilePathCaptor.getValue()).isEqualTo(changedFile.toPath());
  }

  @Test
  void uploadChangedFilesToPod_whenChangedFilesDirContainsDir_thenUploadDirToPod(@TempDir File tempDir) throws WatchException, IOException {
    // Given
    File changedFilesTarball = createChangedFilesDir(tempDir);
    File changedFilesDir = new File(tempDir, "changed-files");
    File changedDeploymentDir = new File(changedFilesDir, "/deployments");
    boolean changedDeploymentDirCreated = changedDeploymentDir.mkdir();
    when(podOperations.dir(anyString())).thenReturn(podOperations);
    when(podOperations.upload(any(Path.class))).thenReturn(true);
    ArgumentCaptor<Path> uploadedFilePathCaptor = ArgumentCaptor.forClass(Path.class);

    // When
    podExecutor.uploadChangedFilesToPod(Collections.emptyList(), changedFilesTarball);

    // Then
    assertThat(changedDeploymentDirCreated).isTrue();
    verify(podOperations).upload(uploadedFilePathCaptor.capture());
    assertThat(uploadedFilePathCaptor.getValue()).isEqualTo(changedDeploymentDir.toPath());
  }

  @Test
  void uploadChangedFilesToPod_whenUploadFailed_thenThrowsException(@TempDir File tempDir) throws IOException, WatchException {
    // Given
    File changedFilesTarball = createChangedFilesDir(tempDir);
    File changedFilesDir = new File(tempDir, "changed-files");
    File changedFile = new File(changedFilesDir, "ROOT.war");
    assertThat(changedFile.createNewFile()).isTrue();
    when(podOperations.file(anyString())).thenReturn(podOperations);
    when(podOperations.upload(any(Path.class))).thenThrow(new KubernetesClientException("Mock Error"));

    // When
    assertThatExceptionOfType(WatchException.class)
        .isThrownBy(() -> podExecutor.uploadChangedFilesToPod(Collections.emptyList(), changedFilesTarball))
        .withMessage("Error while uploading changed files archive to pod: Mock Error");
  }

  private NonNamespaceOperation<Pod, PodList, PodResource> createPodsInNamespaceMock() {
    MixedOperation<Pod, PodList, PodResource> podMixedOp = mock(MixedOperation.class);
    NonNamespaceOperation<Pod, PodList, PodResource> nonNamespaceOperation = mock(NonNamespaceOperation.class);
    when(kubernetesClient.pods()).thenReturn(podMixedOp);
    when(podMixedOp.inNamespace(anyString())).thenReturn(nonNamespaceOperation);
    return nonNamespaceOperation;
  }

  private File createChangedFilesDir(File rootDir) {
    File changedFilesDir = new File(rootDir, "changed-files");
    assertThat(changedFilesDir.mkdir()).isTrue();
    return new File(rootDir, "changed-files.tar");
  }
}
