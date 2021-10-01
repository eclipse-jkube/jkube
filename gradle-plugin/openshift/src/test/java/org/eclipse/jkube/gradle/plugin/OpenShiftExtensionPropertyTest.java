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

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class OpenShiftExtensionPropertyTest {

  public static File BASE = new File("");

  @Parameterized.Parameters(name = "{index} {0}, returns {4}, or with property {1}={2} returns {3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "getBuildStrategyOrDefault", "jkube.build.strategy", "jib", JKubeBuildStrategy.jib,
            JKubeBuildStrategy.s2i },
        new Object[] { "getBuildStrategyOrDefault", "jkube.build.strategy", "docker", JKubeBuildStrategy.docker,
            JKubeBuildStrategy.s2i },
        new Object[] { "getOpenshiftPullSecretOrDefault", "jkube.build.pullSecret", "pullsecret-other", "pullsecret-other",
            "pullsecret-jkube" },
        new Object[] { "getS2iBuildNameSuffixOrDefault", "jkube.s2i.buildNameSuffix", "-other", "-other", "-s2i" },
        new Object[] { "getS2iImageStreamLookupPolicyLocalOrDefault", "jkube.s2i.imageStreamLookupPolicyLocal", "false",
            false, true },
        new Object[] { "getBuildOutputKindOrDefault", "jkube.build.buildOutput.kind", "DockerImage", "DockerImage",
            "ImageStreamTag" },
        new Object[] { "getProcessTemplatesLocallyOrDefault", "jkube.deploy.processTemplatesLocally", "true", true, false },
        new Object[] { "getOpenShiftManifestOrDefault", "jkube.openshiftManifest",
            Paths.get("META-INF", "jkube", "other.yml").toString(),
            Paths.get("META-INF", "jkube", "other.yml").toFile(),
            new File(BASE, "build").toPath().resolve(Paths.get("META-INF", "jkube", "openshift.yml")).toFile() },
        new Object[] { "getImageStreamManifestOrDefault", "jkube.openshiftImageStreamManifest",
            Paths.get("test-project-is.yml").toString(),
            Paths.get("test-project-is.yml").toFile(),
            new File(BASE, "build").toPath().resolve("artifact-id-is.yml").toFile()
        }
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

  @Parameterized.Parameter(4)
  public Object expectedDefault;

  private TestOpenShiftExtension extension;

  @Before
  public void setUp() {
    extension = new TestOpenShiftExtension();
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
