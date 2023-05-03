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
package org.eclipse.jkube.gradle.plugin;

import java.io.File;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.build.service.docker.helper.ContainerNamingUtil;
import org.eclipse.jkube.kit.common.JavaProject;

import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class KubernetesExtensionPropertyTest {

  private static final File BASE = new File("");
  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() {
    extension = new TestKubernetesExtension();
    extension.javaProject = JavaProject.builder()
        .artifactId("artifact-id")
        .baseDirectory(BASE)
        .buildDirectory(new File(BASE, "build"))
        .outputDirectory(new File(BASE, "build"))
        .build();
  }

  @ParameterizedTest(name = "{index}: {0} with defaults returns ''{1}''")
  @MethodSource("defaultValues")
  void getValue_withDefaults_shouldReturnDefaultValue(String method, Object expectedDefault) throws Exception {
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedDefault);
  }

  static Stream<Arguments> defaultValues() {
    return Stream.of(
        arguments("getOfflineOrDefault", false),
        arguments("getFailOnValidationErrorOrDefault", false),
        arguments("getMergeWithDekorateOrDefault", false),
        arguments("getInterpolateTemplateParametersOrDefault", true),
        arguments("getSkipResourceValidationOrDefault", false),
        arguments("getSkipResourceOrDefault", false),
        arguments("getSkipBuildOrDefault", false),
        arguments("getLogFollowOrDefault", true),
        arguments("getRecreateOrDefault", false),
        arguments("getSkipApplyOrDefault", false),
        arguments("getFailOnNoKubernetesJsonOrDefault", false),
        arguments("getCreateNewResourcesOrDefault", true),
        arguments("getServicesOnlyOrDefault", false),
        arguments("getIgnoreServicesOrDefault", false),
        arguments("getJsonLogDirOrDefault", new File(BASE, "build").toPath().resolve(Paths.get("jkube", "applyJson")).toFile()),
        arguments("getDeletePodsOnReplicationControllerUpdateOrDefault", true),
        arguments("getRollingUpgradesOrDefault", false),
        arguments("getServiceUrlWaitTimeSecondsOrDefault", 5),
        arguments("getKubernetesManifestOrDefault", new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "kubernetes.yml")).toFile()),
        arguments("getSkipOrDefault", false),
        arguments("getIgnoreRunningOAuthClientsOrDefault", true),
        arguments("getProcessTemplatesLocallyOrDefault", true),
        arguments("getRollingUpgradePreserveScaleOrDefault", false),
        arguments("getSkipPushOrDefault", false),
        arguments("getPushRegistryOrNull", null),
        arguments("getSkipTagOrDefault", false),
        arguments("getPushRetriesOrDefault", 0),
        arguments("getSkipExtendedAuthOrDefault", false),
        arguments("getBuildRecreateOrDefault", "none"),
        arguments("getUseColorOrDefault", true),
        arguments("getMaxConnectionsOrDefault", 100),
        arguments("getFilterOrNull", null),
        arguments("getImagePullPolicyOrNull", null),
        arguments("getAutoPullOrNull", null),
        arguments("getDockerHostOrNull", null),
        arguments("getCertPathOrNull", null),
        arguments("getSkipMachineOrDefault", false),
        arguments("getForcePullOrDefault", false),
        arguments("getRegistryOrDefault", "docker.io"),
        arguments("getPullRegistryOrDefault", "docker.io"),
        arguments("getBuildSourceDirectoryOrDefault", "src/main/docker"),
        arguments("getBuildOutputDirectoryOrDefault", "build/docker"),
        arguments("getResourceSourceDirectoryOrDefault", BASE.toPath().resolve(Paths.get("src", "main", "jkube")).toFile()),
        arguments("getResourceTargetDirectoryOrDefault", new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube")).toFile()),
        arguments("getResourceEnvironmentOrNull", null),
        arguments("getWorkDirectoryOrDefault", new File(BASE, "build").toPath().resolve(Paths.get("jkube")).toFile()),
        arguments("getProfileOrNull", null),
        arguments("getNamespaceOrNull", null),
        arguments("getBuildStrategyOrDefault", JKubeBuildStrategy.docker),
        arguments("getResourceFileTypeOrDefault", ResourceFileType.yaml),
        arguments("getLogPodNameOrNull", null),
        arguments("getLogDateOrNull", null),
        arguments("getLogStdoutOrDefault", false),
        arguments("getLogContainerNameOrNull", null),
        arguments("getUseProjectClassPathOrDefault", false),
        arguments("getLocalDebugPortOrDefault", 5005),
        arguments("getDebugSuspendOrDefault", false),
        arguments("getKubernetesTemplateOrDefault", new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "kubernetes")).toFile()),
        arguments("getWatchModeOrDefault", WatchMode.both),
        arguments("getWatchIntervalOrDefault", 5000),
        arguments("getWatchKeepRunningOrDefault", false),
        arguments("getWatchPostExecOrNull", null),
        arguments("getWatchAutoCreateCustomNetworksOrDefault", false),
        arguments("getWatchKeepContainerOrDefault", false),
        arguments("getWatchRemoveVolumesOrDefault", false),
        arguments("getWatchContainerNamePatternOrDefault", ContainerNamingUtil.DEFAULT_CONTAINER_NAME_PATTERN),
        arguments("getWatchFollowOrDefault", false),
        arguments("getWatchShowLogsOrNull", null));
  }

  @ParameterizedTest(name = "{index}: {0} with property ''{1}={2}'' returns ''{3}''")
  @MethodSource("propertiesAndValues")
  void getValue_withProperty_shouldReturnFromPropertyValue(String method, String property, String propertyValue, Object expectedValue) throws Exception {
    // Given
    extension.javaProject.getProperties().setProperty(property, propertyValue);
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }

  static Stream<Arguments> propertiesAndValues() {
    return Stream.of(
        arguments("getOfflineOrDefault", "jkube.offline", "true", true),
        arguments("getFailOnValidationErrorOrDefault", "jkube.failOnValidationError", "true", true),
        arguments("getMergeWithDekorateOrDefault", "jkube.mergeWithDekorate", "true", true),
        arguments("getInterpolateTemplateParametersOrDefault", "jkube.interpolateTemplateParameters", "false", false),
        arguments("getSkipResourceValidationOrDefault", "jkube.skipResourceValidation", "true", true),
        arguments("getSkipResourceOrDefault", "jkube.skip.resource", "true", true),
        arguments("getSkipBuildOrDefault", "jkube.skip.build", "true", true),
        arguments("getLogFollowOrDefault", "jkube.log.follow", "false", false),
        arguments("getRecreateOrDefault", "jkube.recreate", "true", true),
        arguments("getSkipApplyOrDefault", "jkube.skip.apply", "true", true),
        arguments("getFailOnNoKubernetesJsonOrDefault", "jkube.deploy.failOnNoKubernetesJson", "true", true),
        arguments("getCreateNewResourcesOrDefault", "jkube.deploy.create", "false", false),
        arguments("getServicesOnlyOrDefault", "jkube.deploy.servicesOnly", "true", true),
        arguments("getIgnoreServicesOrDefault", "jkube.deploy.ignoreServices", "true", true),
        arguments("getJsonLogDirOrDefault", "jkube.deploy.jsonLogDir",
            Paths.get("build", "jkube", "other").toString(),
            Paths.get("build", "jkube", "other").toFile()),
        arguments("getDeletePodsOnReplicationControllerUpdateOrDefault", "jkube.deploy.deletePods", "false", false),
        arguments("getRollingUpgradesOrDefault", "jkube.rolling", "true", true),
        arguments("getServiceUrlWaitTimeSecondsOrDefault", "jkube.serviceUrl.waitSeconds", "1337", 1337),
        arguments("getKubernetesManifestOrDefault", "jkube.kubernetesManifest",
            Paths.get("META-INF", "jkube", "other.yml").toString(),
            Paths.get("META-INF", "jkube", "other.yml").toFile()),
        arguments("getSkipOrDefault", "jkube.skip", "true", true),
        arguments("getIgnoreRunningOAuthClientsOrDefault", "jkube.deploy.ignoreRunningOAuthClients", "false", false),
        arguments("getProcessTemplatesLocallyOrDefault", "jkube.deploy.processTemplatesLocally", "false", false),
        arguments("getRollingUpgradePreserveScaleOrDefault", "jkube.rolling.preserveScale", "true", true),
        arguments("getSkipPushOrDefault", "jkube.skip.push", "true", true),
        arguments("getPushRegistryOrNull", "jkube.docker.push.registry", "https://custom:5000", "https://custom:5000"),
        arguments("getSkipTagOrDefault", "jkube.skip.tag", "true", true),
        arguments("getPushRetriesOrDefault", "jkube.docker.push.retries", "1337", 1337),
        arguments("getSkipExtendedAuthOrDefault", "jkube.docker.skip.extendedAuth", "true", true),
        arguments("getBuildRecreateOrDefault", "jkube.build.recreate", "changed", "changed"),
        arguments("getUseColorOrDefault", "jkube.useColor", "false", false),
        arguments("getMaxConnectionsOrDefault", "jkube.docker.maxConnections", "1337", 1337),
        arguments("getFilterOrNull", "jkube.image.filter", "foo", "foo"),
        arguments("getImagePullPolicyOrNull", "jkube.docker.imagePullPolicy", "Always", "Always"),
        arguments("getAutoPullOrNull", "jkube.docker.autoPull", "true", "true"),
        arguments("getDockerHostOrNull", "jkube.docker.host", "unix:///var/run/docker.sock", "unix:///var/run/docker.sock"),
        arguments("getCertPathOrNull", "jkube.docker.certPath", "~/.docker", "~/.docker"),
        arguments("getSkipMachineOrDefault", "jkube.docker.skip.machine", "true", true),
        arguments("getForcePullOrDefault", "jkube.build.forcePull", "true", true),
        arguments("getRegistryOrDefault", "jkube.docker.registry", "quay.io", "quay.io"),
        arguments("getPullRegistryOrDefault", "jkube.docker.pull.registry", "quay.io", "quay.io"),
        arguments("getBuildSourceDirectoryOrDefault", "jkube.build.source.dir", "src/main/other", "src/main/other"),
        arguments("getBuildOutputDirectoryOrDefault", "jkube.build.target.dir", "build/other", "build/other"),
        arguments("getResourceSourceDirectoryOrDefault", "jkube.resourceDir",
            Paths.get("src", "main", "other").toString(),
            Paths.get("src", "main", "other").toFile()),
        arguments("getResourceTargetDirectoryOrDefault", "jkube.targetDir",
            Paths.get("META-INF", "jkube", "other").toString(),
            Paths.get("META-INF", "jkube", "other").toFile()),
        arguments("getResourceEnvironmentOrNull", "jkube.environment", "dev", "dev"),
        arguments("getWorkDirectoryOrDefault", "jkube.workDir",
            Paths.get("jkube-work-other").toString(),
            Paths.get("jkube-work-other").toFile()),
        arguments("getProfileOrNull", "jkube.profile", "default", "default"),
        arguments("getNamespaceOrNull", "jkube.namespace", "test", "test"),
        arguments("getBuildStrategyOrDefault", "jkube.build.strategy", "s2i", JKubeBuildStrategy.s2i),
        arguments("getBuildStrategyOrDefault", "jkube.build.strategy", "jib", JKubeBuildStrategy.jib),
        arguments("getBuildStrategyOrDefault", "jkube.build.strategy", "docker", JKubeBuildStrategy.docker),
        arguments("getResourceFileTypeOrDefault", "jkube.resourceType", "json", ResourceFileType.json),
        arguments("getLogPodNameOrNull", "jkube.log.pod", "test", "test"),
        arguments("getLogDateOrNull", "jkube.docker.logDate", "test", "test"),
        arguments("getLogStdoutOrDefault", "jkube.docker.logStdout", "true", true),
        arguments("getLogContainerNameOrNull", "jkube.log.container", "test", "test"),
        arguments("getUseProjectClassPathOrDefault", "jkube.useProjectClasspath", "true", true),
        arguments("getLocalDebugPortOrDefault", "jkube.debug.port", "1337", 1337),
        arguments("getDebugSuspendOrDefault", "jkube.debug.suspend", "true", true),
        arguments("getKubernetesTemplateOrDefault", "jkube.kubernetesTemplate",
            Paths.get("META-INF", "jkube", "other").toString(),
            Paths.get("META-INF", "jkube", "other").toFile()),
        arguments("getWatchModeOrDefault", "jkube.watch.mode", "copy", WatchMode.copy),
        arguments("getWatchIntervalOrDefault", "jkube.watch.interval", "10000", 10000),
        arguments("getWatchKeepRunningOrDefault", "jkube.watch.keepRunning", "true", true),
        arguments("getWatchPostExecOrNull", "jkube.watch.postExec", "ls -lt", "ls -lt"),
        arguments("getWatchAutoCreateCustomNetworksOrDefault", "jkube.watch.autoCreateCustomNetworks", "true", true),
        arguments("getWatchKeepContainerOrDefault", "jkube.watch.keepContainer", "true", true),
        arguments("getWatchRemoveVolumesOrDefault", "jkube.watch.removeVolumes", "true", true),
        arguments("getWatchContainerNamePatternOrDefault", "jkube.watch.containerNamePattern", "%n-%g", "%n-%g"),
        arguments("getWatchFollowOrDefault", "jkube.watch.follow", "true", true),
        arguments("getWatchShowLogsOrNull", "jkube.watch.showLogs", "true", "true"));
  }
}
