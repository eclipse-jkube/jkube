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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KubernetesExtensionPropertyTest {

  public static File BASE = new File("");

  @Parameterized.Parameters(name = "{index} {0}, returns {4}, or with property {1}={2} returns {3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "getOfflineOrDefault", "jkube.offline", "true", true, false },
        new Object[] { "getFailOnValidationErrorOrDefault", "jkube.failOnValidationError", "true", true, false },
        new Object[] { "getMergeWithDekorateOrDefault", "jkube.mergeWithDekorate", "true", true, false },
        new Object[] { "getInterpolateTemplateParametersOrDefault", "jkube.interpolateTemplateParameters", "false", false,
            true },
        new Object[] { "getSkipResourceValidationOrDefault", "jkube.skipResourceValidation", "true", true, false },
        new Object[] { "getSkipResourceOrDefault", "jkube.skip.resource", "true", true, false},
        new Object[] { "getSkipBuildOrDefault", "jkube.skip.build", "true", true, false},
        new Object[] { "getLogFollowOrDefault", "jkube.log.follow", "false", false, true },
        new Object[] { "getRecreateOrDefault", "jkube.recreate", "true", true, false },
        new Object[] { "getSkipApplyOrDefault", "jkube.skip.apply", "true", true, false },
        new Object[] { "getFailOnNoKubernetesJsonOrDefault", "jkube.deploy.failOnNoKubernetesJson", "true", true, false
        },
        new Object[] { "getCreateNewResourcesOrDefault", "jkube.deploy.create", "false", false, true },
        new Object[] { "getServicesOnlyOrDefault", "jkube.deploy.servicesOnly", "true", true, false },
        new Object[] { "getIgnoreServicesOrDefault", "jkube.deploy.ignoreServices", "true", true, false },
        new Object[] { "getJsonLogDirOrDefault", "jkube.deploy.jsonLogDir",
            Paths.get("build", "jkube", "other").toString(),
            Paths.get("build", "jkube", "other").toFile(),
            new File(BASE, "build").toPath().resolve(Paths.get("jkube", "applyJson")).toFile() },
        new Object[] { "getDeletePodsOnReplicationControllerUpdateOrDefault", "jkube.deploy.deletePods", "false", false,
            true },
        new Object[] { "getRollingUpgradesOrDefault", "jkube.rolling", "true", true, false },
        new Object[] { "getServiceUrlWaitTimeSecondsOrDefault", "jkube.serviceUrl.waitSeconds", "1337", 1337, 5 },
        new Object[] { "getKubernetesManifestOrDefault", "jkube.kubernetesManifest",
            Paths.get("META-INF", "jkube", "other.yml").toString(),
            Paths.get("META-INF", "jkube", "other.yml").toFile(),
            new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "kubernetes.yml")).toFile() },
        new Object[] { "getSkipOrDefault", "jkube.skip", "true", true, false },
        new Object[] { "getIgnoreRunningOAuthClientsOrDefault", "jkube.deploy.ignoreRunningOAuthClients", "false", false,
            true },
        new Object[] { "getProcessTemplatesLocallyOrDefault", "jkube.deploy.processTemplatesLocally", "false", false,
            true },
        new Object[] { "getRollingUpgradePreserveScaleOrDefault", "jkube.rolling.preserveScale", "true", true, false },
        new Object[] { "getSkipPushOrDefault", "jkube.skip.push", "true", true, false },
        new Object[] { "getSkipTagOrDefault", "jkube.skip.tag", "true", true, false },
        new Object[] { "getPushRetriesOrDefault", "jkube.docker.push.retries", "1337", 1337, 0 },
        new Object[] { "getSkipExtendedAuthOrDefault", "jkube.docker.skip.extendedAuth", "true", true, false },
        new Object[] { "getBuildRecreateOrDefault", "jkube.build.recreate", "changed", "changed", "none" },
        new Object[] { "getUseColorOrDefault", "jkube.useColor", "false", false, true },
        new Object[] { "getMaxConnectionsOrDefault", "jkube.docker.maxConnections", "1337", 1337, 100 },
        new Object[] { "getFilterOrNull", "jkube.image.filter", "foo", "foo", null },
        new Object[] { "getApiVersionOrNull", "jkube.docker.apiVersion", "1.24", "1.24", null },
        new Object[] { "getImagePullPolicyOrNull", "jkube.docker.imagePullPolicy", "Always", "Always", null },
        new Object[] { "getAutoPullOrNull", "jkube.docker.autoPull", "true", "true", null },
        new Object[] { "getDockerHostOrNull", "jkube.docker.host", "unix:///var/run/docker.sock",
            "unix:///var/run/docker.sock", null },
        new Object[] { "getCertPathOrNull", "jkube.docker.certPath", "~/.docker", "~/.docker", null },
        new Object[] { "getSkipMachineOrDefault", "jkube.docker.skip.machine", "true", true, false },
        new Object[] { "getForcePullOrDefault", "jkube.build.forcePull", "true", true, false },
        new Object[] { "getRegistryOrDefault", "jkube.docker.registry", "quay.io", "quay.io", "docker.io" },
        new Object[] { "getPullRegistryOrDefault", "jkube.docker.pull.registry", "quay.io", "quay.io", "docker.io" },
        new Object[] { "getBuildSourceDirectoryOrDefault", "jkube.build.source.dir", "src/main/other", "src/main/other",
            "src/main/docker" },
        new Object[] { "getBuildOutputDirectoryOrDefault", "jkube.build.target.dir", "build/other", "build/other",
            "build/docker" },
        new Object[] { "getResourceSourceDirectoryOrDefault", "jkube.resourceDir",
            Paths.get("src", "main", "other").toString(),
            Paths.get("src", "main", "other").toFile(),
            BASE.toPath().resolve(Paths.get("src", "main", "jkube")).toFile()
        },
        new Object[] { "getResourceTargetDirectoryOrDefault", "jkube.targetDir",
            Paths.get("META-INF", "jkube", "other").toString(),
            Paths.get("META-INF", "jkube", "other").toFile(),
            new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube")).toFile()
        },
        new Object[] { "getResourceEnvironmentOrNull", "jkube.environment", "dev", "dev", null },
        new Object[] { "getWorkDirectoryOrDefault", "jkube.workDir",
            Paths.get("jkube-work-other").toString(),
            Paths.get("jkube-work-other").toFile(),
            new File(BASE, "build").toPath().resolve(Paths.get("jkube")).toFile() },
        new Object[] { "getProfileOrNull", "jkube.profile", "default", "default", null },
        new Object[] { "getNamespaceOrNull", "jkube.namespace", "test", "test", null },
        new Object[] { "getBuildStrategyOrDefault", "jkube.build.strategy", "s2i", JKubeBuildStrategy.s2i,
            JKubeBuildStrategy.docker },
        new Object[] { "getBuildStrategyOrDefault", "jkube.build.strategy", "jib", JKubeBuildStrategy.jib,
            JKubeBuildStrategy.docker },
        new Object[] { "getResourceFileTypeOrDefault", "jkube.resourceType", "json", ResourceFileType.json,
            ResourceFileType.yaml },
        new Object[] { "getLogPodNameOrNull", "jkube.log.pod", "test", "test", null },
        new Object[] { "getLogContainerNameOrNull", "jkube.log.container", "test", "test", null },
        new Object[] { "getUseProjectClassPathOrDefault", "jkube.useProjectClasspath", "true", true, false },
        new Object[] { "getLocalDebugPortOrDefault", "jkube.debug.port", "1337", 1337, 5005 },
        new Object[] { "getDebugSuspendOrDefault", "jkube.debug.suspend", "true", true, false },
        new Object[] { "getKubernetesTemplateOrDefault", "jkube.kubernetesTemplate",
            Paths.get("META-INF", "jkube", "other").toString(),
            Paths.get("META-INF", "jkube", "other").toFile(),
            new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "kubernetes")).toFile()
        });
  }

  @Parameterized.Parameter
  public String method;

  @Parameterized.Parameter(1)
  public String property;

  @Parameterized.Parameter(2)
  public String propertyValue;

  @Parameterized.Parameter(3)
  public Object expectedValue;

  @Parameterized.Parameter(4)
  public Object expectedDefault;

  private TestKubernetesExtension extension;

  @Before
  public void setUp() throws Exception {
    extension = new TestKubernetesExtension();
    extension.javaProject = JavaProject.builder()
        .artifactId("artifact-id")
        .baseDirectory(BASE)
        .buildDirectory(new File(BASE, "build"))
        .outputDirectory(new File(BASE, "build"))
        .build();
  }

  @Test
  public void getValue_withDefaults_shouldReturnDefaultValue() throws Exception {
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedDefault);
  }

  @Test
  public void getValue_withProperty_shouldReturnFromPropertyValue() throws Exception {
    // Given
    extension.javaProject.getProperties().setProperty(property, propertyValue);
    // When
    final Object result = extension.getClass().getMethod(method).invoke(extension);
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
