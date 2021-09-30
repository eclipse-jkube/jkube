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
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jkube.kit.common.JavaProject;

import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KubernetesExtensionPropertyTest {

  @Parameterized.Parameters(name = "{index} {0}, with {1}={2}, returns {3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new Object[] { "getOfflineOrDefault", "jkube.offline", "false", false },
      new Object[] { "getUseProjectClassPathOrDefault", "jkube.useProjectClasspath", "true", true },
      new Object[] { "getFailOnValidationErrorOrDefault", "jkube.failOnValidationError", "false", false},
      new Object[] { "getMergeWithDekorateOrDefault", "jkube.mergeWithDekorate", "false", false},
      new Object[] { "getInterpolateTemplateParametersOrDefault", "jkube.interpolateTemplateParameters", "true", true},
      new Object[] { "getSkipResourceValidationOrDefault", "jkube.skipResourceValidation", "false", false},
      new Object[] { "getLogFollowOrDefault", "jkube.log.follow", "true", true},
      new Object[] { "getRecreateOrDefault", "jkube.recreate", "false", false},
      new Object[] { "getSkipApplyOrDefault", "jkube.skip.apply", "false", false},
      new Object[] { "getFailOnNoKubernetesJsonOrDefault", "jkube.deploy.failOnNoKubernetesJson", "false",false},
      new Object[] { "getCreateNewResourcesOrDefault", "jkube.deploy.create", "true", true},
      new Object[] { "getServicesOnlyOrDefault", "jkube.deploy.servicesOnly", "false", false},
      new Object[] { "getIgnoreServicesOrDefault", "jkube.deploy.ignoreServices", "false", false},
      new Object[] { "getJsonLogDirOrDefault", "jkube.deploy.jsonLogDir", Paths.get("build", "jkube", "applyJson").toString(), Paths.get("build", "jkube", "applyJson").toFile()},
      new Object[] { "getDeletePodsOnReplicationControllerUpdateOrDefault", "jkube.deploy.deletePods", "true", true},
      new Object[] { "getRollingUpgradesOrDefault", "jkube.rolling", "false", false},
      new Object[] { "getServiceUrlWaitTimeSecondsOrDefault", "jkube.serviceUrl.waitSeconds", "5", 5},
      new Object[] { "getKubernetesManifestOrDefault", "jkube.kubernetesManifest", Paths.get("META-INF", "jkube", "kubernetes.yml").toString(), Paths.get("META-INF", "jkube", "kubernetes.yml").toFile()},
      new Object[] { "getSkipOrDefault", "jkube.skip", "false", false},
      new Object[] { "getIgnoreRunningOAuthClientsOrDefault", "jkube.deploy.ignoreRunningOAuthClients", "true", true},
      new Object[] { "getProcessTemplatesLocallyOrDefault", "jkube.deploy.processTemplatesLocally", "true", true},
      new Object[] { "getRollingUpgradePreserveScaleOrDefault", "jkube.rolling.preserveScale", "false", false},
      new Object[] { "getSkipPushOrDefault", "jkube.skip.push", "false", false},
      new Object[] { "getSkipTagOrDefault", "jkube.skip.tag", "false", false},
      new Object[] { "getPushRetriesOrDefault", "jkube.docker.push.retries", "0", 0},
      new Object[] { "getSkipExtendedAuthOrDefault", "jkube.docker.skip.extendedAuth", "false", false},
      new Object[] { "getUseColorOrDefault", "jkube.useColor", "true", true},
      new Object[] { "getMaxConnectionsOrDefault", "jkube.docker.maxConnections", "100", 100},
      new Object[] { "getFilterOrDefault", "jkube.image.filter", "foo", "foo"},
      new Object[] { "getApiVersionOrDefault", "jkube.docker.apiVersion", "1.24", "1.24"},
      new Object[] { "getImagePullPolicyOrDefault", "jkube.docker.imagePullPolicy", "Always", "Always"},
      new Object[] { "getAutoPullOrDefault", "jkube.docker.autoPull", "true", "true"},
      new Object[] { "getDockerHostOrDefault", "jkube.docker.host", "unix:///var/run/docker.sock", "unix:///var/run/docker.sock"},
      new Object[] { "getCertPathOrDefault", "jkube.docker.certPath", "~/.docker", "~/.docker"},
      new Object[] { "getSkipMachineOrDefault", "jkube.docker.skip.machine", "false", false},
      new Object[] { "getForcePullOrDefault", "jkube.build.forcePull", "false", false},
      new Object[] { "getRegistryOrDefault", "jkube.docker.registry", "docker.io", "docker.io"},
      new Object[] { "getPullRegistryOrDefault", "jkube.docker.pull.registry", "quay.io", "quay.io"},
      new Object[] { "getBuildSourceDirectoryOrDefault", "jkube.build.source.dir", "src/main/docker", "src/main/docker"},
      new Object[] { "getBuildOutputDirectoryOrDefault", "jkube.build.target.dir", "build/docker", "build/docker"},
      new Object[] { "getResourceSourceDirectoryOrDefault", "jkube.resourceDir", Paths.get("src", "main", "jkube").toString(), Paths.get("src", "main", "jkube").toFile()},
      new Object[] { "getResourceTargetDirectoryOrDefault", "jkube.targetDir", Paths.get("META-INF", "jkube").toString(), Paths.get("META-INF", "jkube").toFile()},
      new Object[] { "getResourceEnvironmentOrDefault", "jkube.environment", "dev", "dev"},
      new Object[] { "getWorkDirectoryOrDefault", "jkube.workDir", Paths.get("jkube").toString(), Paths.get("jkube").toFile()},
      new Object[] { "getProfileOrDefault", "jkube.profile", "default", "default"},
      new Object[] { "getNamespaceOrDefault", "jkube.namespace", "test", "test"},
      new Object[] { "getBuildStrategyOrDefault", "jkube.build.strategy", "docker", JKubeBuildStrategy.docker},
      new Object[] { "getBuildStrategyOrDefault", "jkube.build.strategy", "jib", JKubeBuildStrategy.jib},
      new Object[] { "getResourceFileTypeOrDefault", "jkube.resourceType", "yaml", ResourceFileType.yaml},
      new Object[] { "getResourceFileTypeOrDefault", "jkube.resourceType", "json", ResourceFileType.json},
      new Object[] { "getBuildRecreateOrDefault", "jkube.build.recreate", "none", "none"},
      new Object[] { "getLogPodNameOrDefault", "jkube.log.pod", "test", "test"},
      new Object[] { "getLogContainerNameOrDefault", "jkube.log.container", "test", "test"}
    );
  }

  @Parameterized.Parameter
  public String method;

  @Parameterized.Parameter(1)
  public String property;

  @Parameterized.Parameter(2)
  public String propertyValue;

  @Parameterized.Parameter(3)
  public Object expectedValue;

  @Test
  public void test() throws Exception {
    // Given
    final KubernetesExtension extension = new TestKubernetesExtension();
    extension.javaProject = JavaProject.builder()
      .baseDirectory(Paths.get("").toFile())
      .buildDirectory(new File(Paths.get("").toFile(), "build"))
      .outputDirectory(new File(Paths.get("").toFile(), "build"))
      .build();
    extension.javaProject.getProperties().setProperty(property, propertyValue);
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
