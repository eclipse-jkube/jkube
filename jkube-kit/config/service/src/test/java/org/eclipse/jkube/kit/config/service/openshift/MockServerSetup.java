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
package org.eclipse.jkube.kit.config.service.openshift;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ImageStreamStatusBuilder;
import io.fabric8.openshift.api.model.ImageStreamTagBuilder;
import io.fabric8.openshift.api.model.NamedTagEventListBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.eclipse.jkube.kit.common.BuildRecreateMode;

/**
 * Builder class for setting up mock server expectations for OpenshiftBuildService tests.
 *
 * <p>This class simplifies the mock server setup by providing a fluent API using Lombok:
 *
 * <pre>{@code
 * WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
 *     .namespace("ns1")
 *     .resourceName("myapp")
 *     .buildConfigSuffix("-s2i")
 *     .buildConfigExists(false)
 *     .imageStreamExists(false)
 *     .configure();
 * }</pre>
 */
@Getter
@Setter
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(staticName = "forServer")
public class MockServerSetup {

  private final KubernetesMockServer mockServer;

  private String namespace = "ns1";
  private String resourceName = "myapp";
  private String buildConfigSuffix = "-s2i";
  private String buildOutputKind = "ImageStreamTag";
  private boolean buildConfigExists = false;
  private boolean imageStreamExists = false;
  private boolean buildSucceeds = true;
  private boolean additionalTagsCreated = false;
  private long buildDelay = 50L;
  private BuildRecreateMode recreateMode = BuildRecreateMode.none;

  /**
   * Configures the mock server with all necessary expectations and returns an event collector.
   *
   * @return WebServerEventCollector for asserting on recorded events
   */
  public WebServerEventCollector configure() {
    WebServerEventCollector collector = new WebServerEventCollector();
    String buildConfigName = resourceName + buildConfigSuffix;

    // Create test resources
    BuildConfig bc = createBuildConfig(buildConfigName);
    ImageStream imageStream = createImageStream();
    KubernetesList builds = createBuildsList();
    Build build = createBuild();

    // Configure BuildConfig endpoints
    configureBuildConfigEndpoints(collector, buildConfigName, bc);

    // Configure ImageStream endpoints (skip for DockerImage output)
    if (!"DockerImage".equals(buildOutputKind)) {
      configureImageStreamEndpoints(collector, imageStream);
    }

    // Configure build execution endpoints
    configureBuildExecutionEndpoints(collector, buildConfigName, builds, build, imageStream);

    // Configure additional tags if needed
    if (additionalTagsCreated) {
      configureAdditionalTagEndpoints(collector);
    }

    return collector;
  }

  private void configureBuildConfigEndpoints(WebServerEventCollector collector, String buildConfigName, BuildConfig bc) {
    String bcPath = "/apis/build.openshift.io/v1/namespaces/" + namespace + "/buildconfigs/";
    String bcBasePath = "/apis/build.openshift.io/v1/namespaces/" + namespace + "/buildconfigs";

    boolean recreateBuildConfig = recreateMode == BuildRecreateMode.buildConfig || recreateMode == BuildRecreateMode.all;

    if (recreateBuildConfig && buildConfigExists) {
      // Recreate mode: check (200) -> delete -> check (404) -> create
      mockServer.expect().get().withPath(bcPath + buildConfigName)
          .andReply(collector.record("build-config-check").andReturn(200, bc))
          .once();
      mockServer.expect().delete().withPath(bcPath + buildConfigName)
          .andReply(collector.record("build-config-delete").andReturn(200, bc))
          .once();
      mockServer.expect().get().withPath(bcPath + buildConfigName)
          .andReply(collector.record("build-config-check-after-delete").andReturn(404, ""))
          .once();
      mockServer.expect().post().withPath(bcBasePath)
          .andReply(collector.record("new-build-config").andReturn(201, bc))
          .once();
    } else if (!buildConfigExists) {
      // New BuildConfig: check (404) -> create
      mockServer.expect().get().withPath(bcPath + buildConfigName)
          .andReply(collector.record("build-config-check").andReturn(404, ""))
          .once();
      mockServer.expect().post().withPath(bcBasePath)
          .andReply(collector.record("new-build-config").andReturn(201, bc))
          .once();
    } else {
      // Existing BuildConfig: patch
      mockServer.expect().patch().withPath(bcPath + buildConfigName)
          .andReply(collector.record("patch-build-config").andReturn(200, bc))
          .once();
    }

    // BuildConfig always available after operations
    mockServer.expect().get().withPath(bcPath + buildConfigName)
        .andReply(collector.record("build-config-check").andReturn(200, bc))
        .always();
  }

  private void configureImageStreamEndpoints(WebServerEventCollector collector, ImageStream imageStream) {
    String isPath = "/apis/image.openshift.io/v1/namespaces/" + namespace + "/imagestreams/";
    String isBasePath = "/apis/image.openshift.io/v1/namespaces/" + namespace + "/imagestreams";

    boolean recreateImageStream = recreateMode == BuildRecreateMode.imageStream || recreateMode == BuildRecreateMode.all;

    if (recreateImageStream && imageStreamExists) {
      // Recreate mode: check (200) -> delete -> check (404) -> create
      mockServer.expect().get().withPath(isPath + resourceName)
          .andReply(collector.record("imagestream-check").andReturn(200, imageStream))
          .once();
      mockServer.expect().delete().withPath(isPath + resourceName)
          .andReply(collector.record("imagestream-delete").andReturn(200, imageStream))
          .once();
      mockServer.expect().get().withPath(isPath + resourceName)
          .andReturn(404, "")
          .once();
      mockServer.expect().post().withPath(isBasePath)
          .andReply(collector.record("imagestream-create").andReturn(201, imageStream))
          .once();
    } else if (!imageStreamExists) {
      // New ImageStream: check (404) -> create
      mockServer.expect().get().withPath(isPath + resourceName)
          .andReturn(404, "")
          .once();
      mockServer.expect().post().withPath(isBasePath)
          .andReply(collector.record("imagestream-create").andReturn(201, imageStream))
          .once();
    } else {
      // ImageStream exists but no recreate - just return existing
      mockServer.expect().get().withPath(isPath + resourceName)
          .andReply(collector.record("imagestream-check").andReturn(200, imageStream))
          .once();
    }

    // ImageStream always available after operations
    mockServer.expect().get().withPath(isPath + resourceName)
        .andReturn(200, imageStream)
        .always();
  }

  private void configureBuildExecutionEndpoints(WebServerEventCollector collector, String buildConfigName,
                                                 KubernetesList builds, Build build, ImageStream imageStream) {
    // Pod label selector - match the pattern from the working test
    mockServer.expect().get()
        .withPath("/api/v1/namespaces/" + namespace + "/pods?labelSelector=openshift.io%2Fbuild.name")
        .andReply(collector.record("build-config-check").andReturn(200, builds))
        .always();

    // Build instantiation - returns imageStream like the original test
    mockServer.expect().post()
        .withPath("/apis/build.openshift.io/v1/namespaces/" + namespace + "/buildconfigs/" +
            buildConfigName + "/instantiatebinary?name=" + buildConfigName + "&namespace=" + namespace)
        .andReply(collector.record("pushed").andReturn(201, imageStream))
        .once();

    // Build status endpoints
    mockServer.expect().get()
        .withPath("/apis/build.openshift.io/v1/namespaces/" + namespace + "/builds")
        .andReply(collector.record("check-build").andReturn(200, builds))
        .always();
    mockServer.expect().get()
        .withPath("/apis/build.openshift.io/v1/namespaces/" + namespace +
            "/builds?labelSelector=openshift.io/build-config.name%3D" + buildConfigName)
        .andReturn(200, builds)
        .always();
    mockServer.expect()
        .withPath("/apis/build.openshift.io/v1/namespaces/" + namespace + "/builds/" + resourceName)
        .andReturn(200, build)
        .always();

    // Build watch (WebSocket)
    mockServer.expect()
        .withPath("/apis/build.openshift.io/v1/namespaces/" + namespace +
            "/builds?allowWatchBookmarks=true&fieldSelector=metadata.name%3D" + resourceName + "&watch=true")
        .andUpgradeToWebSocket().open()
        .waitFor(buildDelay)
        .andEmit(new WatchEvent(build, "MODIFIED"))
        .done()
        .always();
  }

  private void configureAdditionalTagEndpoints(WebServerEventCollector collector) {
    mockServer.expect().get()
        .withPath("/apis/image.openshift.io/v1/namespaces/" + namespace + "/imagestreamtags/" + resourceName + ":latest")
        .andReply(collector.record("imagestreamtag-get").andReturn(200,
            new ImageStreamTagBuilder()
                .withNewMetadata().withName(resourceName + ":latest").endMetadata()
                .withNewImage()
                .withDockerImageReference("foo-registry.openshift.svc:5000/test/" + resourceName + "@sha256:1234")
                .endImage()
                .build()))
        .once();

    mockServer.expect().post()
        .withPath("/apis/image.openshift.io/v1/namespaces/" + namespace + "/imagestreamtags")
        .andReply(collector.record("imagestreamtag-create").andReturn(200,
            new ImageStreamTagBuilder()
                .withNewMetadata().withName(resourceName + ":t1").endMetadata()
                .build()))
        .once();
  }

  // Resource creation helpers

  private BuildConfig createBuildConfig(String name) {
    return new BuildConfigBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .withNewOutput()
        .withNewTo()
        .withKind(buildOutputKind)
        .endTo()
        .endOutput()
        .endSpec()
        .build();
  }

  private ImageStream createImageStream() {
    return new ImageStreamBuilder()
        .withNewMetadata()
        .withName(resourceName)
        .endMetadata()
        .withStatus(new ImageStreamStatusBuilder()
            .addNewTagLike(new NamedTagEventListBuilder()
                .addNewItem()
                .withImage("abcdef0123456789")
                .endItem()
                .build())
            .endTag()
            .build())
        .build();
  }

  private KubernetesList createBuildsList() {
    return new KubernetesListBuilder()
        .withItems(new BuildBuilder()
            .withNewMetadata()
            .withName(resourceName)
            .endMetadata()
            .build())
        .withNewMetadata().withResourceVersion("1").endMetadata()
        .build();
  }

  private Build createBuild() {
    return new BuildBuilder()
        .withNewMetadata().withResourceVersion("2").endMetadata()
        .withNewStatus().withPhase(buildSucceeds ? "Complete" : "Fail").endStatus()
        .build();
  }
}
