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
package org.eclipse.jkube.kit.config.service.ingresscontroller;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressClassBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressClassListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKubernetesMockClient
class KongIngressControllerDetectorTest {
  private KubernetesMockServer mockServer;
  private KubernetesClient client;
  private KongIngressControllerDetector detector;

  @BeforeEach
  void setUp() {
    detector = new KongIngressControllerDetector(client);
  }

  @Test
  void isDetected_whenNothingProvided_thenReturnFalse() {
    assertThat(detector.isDetected()).isFalse();
  }

  @Test
  void isDetected_whenIngressClassPresentAndManualInstallation_thenReturnTrue() {
    // Given
    mockServer.expect().get()
        .withPath("/apis/networking.k8s.io/v1/ingressclasses")
        .andReturn(HttpURLConnection.HTTP_OK, new IngressClassListBuilder()
            .addToItems(new IngressClassBuilder().withNewSpec().withController("ingress-controllers.konghq.com/kong").endSpec().build())
            .build())
        .once();
    mockServer.expect().get()
        .withPath("/api/v1/pods?labelSelector=app.kubernetes.io%2Fname%3Dkong")
        .andReturn(HttpURLConnection.HTTP_OK, new PodListBuilder().build())
        .once();
    mockServer.expect().get()
        .withPath("/api/v1/namespaces/kong/pods?labelSelector=app%3Dingress-kong")
        .andReturn(HttpURLConnection.HTTP_OK, new PodListBuilder()
            .addToItems(new PodBuilder().withNewStatus().withPhase("Running").endStatus().build())
            .build())
        .once();

    // When
    boolean result = detector.isDetected();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isDetected_whenIngressClassPresentAndHelmInstallation_thenReturnTrue() {
    // Given
    mockServer.expect().get()
        .withPath("/apis/networking.k8s.io/v1/ingressclasses")
        .andReturn(HttpURLConnection.HTTP_OK, new IngressClassListBuilder()
            .addToItems(new IngressClassBuilder().withNewSpec().withController("ingress-controllers.konghq.com/kong").endSpec().build())
            .build())
        .once();
    mockServer.expect().get()
        .withPath("/api/v1/namespaces/kong/pods?labelSelector=app%3Dingress-kong")
        .andReturn(HttpURLConnection.HTTP_OK, new PodListBuilder().build())
        .once();
    mockServer.expect().get()
        .withPath("/api/v1/pods?labelSelector=app.kubernetes.io%2Fname%3Dkong")
        .andReturn(HttpURLConnection.HTTP_OK, new PodListBuilder()
            .addToItems(new PodBuilder().withNewStatus().withPhase("Running").endStatus().build())
            .build())
        .once();

    // When
    boolean result = detector.isDetected();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasPermissions_whenAccessReviewReturnsAllowed_thenReturnTrue() {
    // Given
    mockServer.expect().post()
        .withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
        .andReturn(HttpURLConnection.HTTP_CREATED, new SelfSubjectAccessReviewBuilder()
            .withNewStatus()
            .withAllowed(true)
            .endStatus()
            .build())
        .times(2);

    // When
    boolean permitted = detector.hasPermissions();

    // Then
    assertThat(permitted).isTrue();
  }

  @Test
  void hasPermissions_whenAccessReviewReturnsNotAllowed_thenReturnFalse() {
    // Given
    mockServer.expect().post()
        .withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
        .andReturn(HttpURLConnection.HTTP_CREATED, new SelfSubjectAccessReviewBuilder()
            .withNewStatus()
            .withAllowed(false)
            .endStatus()
            .build())
        .once();

    // When
    boolean permitted = detector.hasPermissions();

    // Then
    assertThat(permitted).isFalse();
  }
}
