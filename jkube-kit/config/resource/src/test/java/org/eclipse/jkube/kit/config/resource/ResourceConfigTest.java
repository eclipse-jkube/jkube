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
package org.eclipse.jkube.kit.config.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SuppressWarnings("deprecation")
class ResourceConfigTest {
  @Test
  void resolveControllerConfig_whenInvoked_shouldMergeLegacyOptionsWithController() {
    // Given
    ProbeConfig probeConfig = ProbeConfig.builder().build();
    ResourceConfig resourceConfig = ResourceConfig.builder()
        .env(Collections.singletonMap("FOO", "BAR"))
        .liveness(probeConfig)
        .readiness(probeConfig)
        .startup(probeConfig)
        .controllerName("test-controller")
        .imagePullPolicy("Always")
        .restartPolicy("OnFailure")
        .replicas(2)
        .containerPrivileged(false)
        .build();

    // When
    ControllerResourceConfig controllerResourceConfig = resourceConfig.getController();

    // Then
    assertThat(controllerResourceConfig)
        .hasFieldOrPropertyWithValue("controllerName", "test-controller")
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("FOO", "BAR"))
        .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
        .hasFieldOrPropertyWithValue("replicas", 2)
        .hasFieldOrPropertyWithValue("liveness", probeConfig)
        .hasFieldOrPropertyWithValue("readiness", probeConfig)
        .hasFieldOrPropertyWithValue("startup", probeConfig)
        .hasFieldOrPropertyWithValue("restartPolicy", "OnFailure")
        .hasFieldOrPropertyWithValue("containerPrivileged", false);
  }

  @Test
  void resolveControllerConfig_whenInvoked_shouldMergeSingleControllerConfig() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder()
        .controller(ControllerResourceConfig.builder()
            .controllerName("test-controller")
            .imagePullPolicy("Always")
            .replicas(2)
            .containerPrivileged(false)
            .resourceRequestsLimits(RequestsLimitsConfig.builder()
                .request("memory", "64Mi")
                .limit("cpu", "500m")
                .build())
            .build())
        .build();

    // When
    ControllerResourceConfig controllerConfigList = resourceConfig.getController();

    // Then
    assertThat(controllerConfigList)
        .hasFieldOrPropertyWithValue("controllerName", "test-controller")
        .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
        .hasFieldOrPropertyWithValue("replicas", 2)
        .hasFieldOrPropertyWithValue("containerPrivileged", false)
        .hasFieldOrPropertyWithValue("resourceRequestsLimits.limits.cpu", "500m")
        .hasFieldOrPropertyWithValue("resourceRequestsLimits.requests.memory", "64Mi");
  }

  @Test
  void resolveControllerConfig_whenInvokedWithBothResourceAndControllerConfig_shouldThrowException() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder()
        .controller(ControllerResourceConfig.builder()
            .controllerName("test-controller")
            .build())
        .controllerName("test-controller")
        .build();

    // When + Then
    assertThatIllegalArgumentException()
        .isThrownBy(resourceConfig::getController)
        .withMessage("Can't use both controller and resource level controller configuration fields. Please migrate to controller configuration");
  }

  @Test
  void toBuilder_whenProvidedNullObject_thenReturnsValidObject() {
    assertThat(ResourceConfig.toBuilder(null)).isNotNull();
  }

  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final ResourceConfig result = mapper.readValue(
        getClass().getResourceAsStream("/resource-config.json"),
        ResourceConfig.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("KEY1", "VALUE1"))
        .hasFieldOrPropertyWithValue("controllerName", "test-controller")
        .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent")
        .hasFieldOrPropertyWithValue("containerPrivileged", false)
        .hasFieldOrPropertyWithValue("replicas", 3)
        .hasFieldOrPropertyWithValue("restartPolicy", "OnFailure")
        .hasFieldOrPropertyWithValue("remotes", Collections.singletonList("http://example.com/manifests/deployment.yaml"))
        .hasFieldOrPropertyWithValue("namespace", "foo-ns")
        .hasFieldOrPropertyWithValue("serviceAccount", "foo-sa")
        .hasFieldOrPropertyWithValue("customResourceDefinitions", Collections.singletonList("crontab.sample.example.com"))
        .hasFieldOrPropertyWithValue("createExternalUrls", true)
        .hasFieldOrPropertyWithValue("routeDomain", "example.com")
        .satisfies(r -> assertProbe(r.getLiveness()))
        .satisfies(r -> assertProbe(r.getReadiness()))
        .satisfies(r -> assertProbe(r.getStartup()))
        .satisfies(r -> assertThat(r.getVolumes())
            .singleElement(InstanceOfAssertFactories.type(VolumeConfig.class))
            .hasFieldOrPropertyWithValue("name", "workdir")
            .hasFieldOrPropertyWithValue("type", "emptyDir")
            .hasFieldOrPropertyWithValue("path", "/work-dir"))
        .satisfies(r -> assertThat(r.getLabels())
            .extracting(MetaDataConfig::getAll)
            .hasFieldOrPropertyWithValue("label_key", "label_value"))
        .satisfies(r -> assertThat(r.getAnnotations())
            .extracting(MetaDataConfig::getAll)
            .hasFieldOrPropertyWithValue("annotation_key", "annotation_value"))
        .satisfies(r -> assertThat(r.getSecrets())
            .singleElement(InstanceOfAssertFactories.type(SecretConfig.class))
            .hasFieldOrPropertyWithValue("name", "secret1")
            .hasFieldOrPropertyWithValue("dockerServerId", "dockerhub"))
        .satisfies(r -> assertThat(r.getServices())
            .singleElement(InstanceOfAssertFactories.type(ServiceConfig.class))
            .hasFieldOrPropertyWithValue("name", "service1")
            .hasFieldOrPropertyWithValue("headless", true)
            .extracting(ServiceConfig::getPorts)
            .asList()
            .singleElement()
            .hasFieldOrPropertyWithValue("port", 8080))
        .satisfies(r -> assertThat(r.getConfigMap())
            .hasFieldOrPropertyWithValue("name", "configmap1")
            .extracting(ConfigMap::getEntries)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(ConfigMapEntry.class))
            .hasFieldOrPropertyWithValue("name", "entryKey")
            .hasFieldOrPropertyWithValue("value", "entryValue"))
        .satisfies(r -> assertThat(r.getServiceAccounts())
            .singleElement(InstanceOfAssertFactories.type(ServiceAccountConfig.class))
            .hasFieldOrPropertyWithValue("name", "foo-sa")
            .hasFieldOrPropertyWithValue("deploymentRef", "foo-deployment"))
        .satisfies(r -> assertThat(r.getOpenshiftBuildConfig())
            .hasFieldOrPropertyWithValue("limits.memory", "128Mi")
            .hasFieldOrPropertyWithValue("limits.cpu", "500m")
            .hasFieldOrPropertyWithValue("requests.memory", "64Mi")
            .hasFieldOrPropertyWithValue("requests.cpu", "250m"))
        .satisfies(r -> assertThat(r.getIngress())
            .hasFieldOrPropertyWithValue("ingressTlsConfigs", Collections.singletonList(IngressTlsConfig.builder()
                    .host("example.com")
                    .secretName("testsecret-tls")
                .build()))
            .extracting(IngressConfig::getIngressRules)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(IngressRuleConfig.class))
            .hasFieldOrPropertyWithValue("host", "example.com")
            .extracting(IngressRuleConfig::getPaths)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(IngressRulePathConfig.class))
            .hasFieldOrPropertyWithValue("pathType", "Prefix")
            .hasFieldOrPropertyWithValue("path", "/foo")
            .hasFieldOrPropertyWithValue("serviceName", "service1")
            .hasFieldOrPropertyWithValue("servicePort", 8080));
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    ResourceConfig  rc1 = ResourceConfig.builder().namespace("ns1").build();
    ResourceConfig rc2 = ResourceConfig.builder().namespace("ns1").build();
    // When + Then
    assertThat(rc1)
        .isEqualTo(rc2)
        .hasSameHashCodeAs(rc2);
  }

  private void assertProbe(ProbeConfig probeConfig) {
    assertThat(probeConfig)
        .hasFieldOrPropertyWithValue("getUrl", "http://:8080/q/health")
        .hasFieldOrPropertyWithValue("initialDelaySeconds", 3)
        .hasFieldOrPropertyWithValue("timeoutSeconds", 3);
  }
}
