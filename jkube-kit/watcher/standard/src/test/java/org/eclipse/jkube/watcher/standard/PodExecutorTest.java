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
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchException;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
class PodExecutorTest {
    private PodOperationsImpl podOperations;
    private KubernetesClient kubernetesClient;
    private PodExecutor podExecutor;
    private NonNamespaceOperation<Pod, PodList, PodResource> podNonNamespaceOp;
    private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;

    @BeforeEach
    public void setUp() {
        ClusterAccess clusterAccess = mock(ClusterAccess.class);
        podOperations = mock(PodOperationsImpl.class);
        kubernetesClient = mock(KubernetesClient.class);
        when(clusterAccess.getNamespace()).thenReturn("default");
        when(clusterAccess.createDefaultClient()).thenReturn(kubernetesClient);
        podNonNamespaceOp = createPodsInNamespaceMock(kubernetesClient);
        when(podNonNamespaceOp.withName(anyString())).thenReturn(podOperations);
        kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
        kubernetesHelperMockedStatic.when(() -> KubernetesHelper.getNewestApplicationPodName(eq(kubernetesClient), any(), any())).thenReturn("test-pod");
        podExecutor = new PodExecutor(clusterAccess, Duration.ZERO);
    }

    @AfterEach
    public void tearDown() {
        kubernetesHelperMockedStatic.close();
    }

    @Test
    void executeCommandInPodKubernetesError() {
//Given
        when(podNonNamespaceOp.withName(anyString())).thenThrow(new KubernetesClientException("MockedError"));
//When
        final WatchException result = assertThrows(WatchException.class,
                () -> podExecutor.executeCommandInPod(Collections.emptySet(), "sh"));
//Then
        assertThat(result).hasMessage("ExecutionfailedduetoaKubernetesClienterror:MockedError");
    }

    private NonNamespaceOperation<Pod, PodList, PodResource> createPodsInNamespaceMock(KubernetesClient client) {
        MixedOperation<Pod, PodList, PodResource> podMixedOp = mock(MixedOperation.class);
        NonNamespaceOperation<Pod, PodList, PodResource> nonNamespaceOperation = mock(NonNamespaceOperation.class);
        when(kubernetesClient.pods()).thenReturn(podMixedOp);
        when(podMixedOp.inNamespace(anyString())).thenReturn(nonNamespaceOperation);
        return nonNamespaceOperation;
    }

    private void createPodExecMock(NonNamespaceOperation<Pod, PodList, PodResource> podNonNamespaceOp) {
        PodResource mockPodResource = mock(PodResource.class);
        when(podNonNamespaceOp.withName(anyString())).thenReturn(mockPodResource);
        when(mockPodResource.readingInput(any())).thenReturn(podOperations);
        when(podOperations.writingOutput(any())).thenReturn(podOperations);
        when(podOperations.writingError(any())).thenReturn(podOperations);
        when(podOperations.writingErrorChannel(any())).thenReturn(podOperations);
        when(podOperations.usingListener(any())).thenReturn(podOperations);
        ExecWatch execWatch = mock(ExecWatch.class);
        when(podOperations.exec(any())).thenReturn(execWatch);
    }
}
