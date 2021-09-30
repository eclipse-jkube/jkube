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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class OpenShiftExtensionPropertyTest {
  @Parameterized.Parameters(name = "{index} {0}, with {1}={2}, returns {3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new Object[] { "getOpenshiftPullSecretOrDefault", "jkube.build.pullSecret", "pullsecret-jkube", "pullsecret-jkube" },
      new Object[] { "getS2iBuildNameSuffixOrDefault", "jkube.s2i.buildNameSuffix", "-s2i", "-s2i"},
      new Object[] { "getS2iImageStreamLookupPolicyLocalOrDefault", "jkube.s2i.imageStreamLookupPolicyLocal", "true", true},
      new Object[] { "getProcessTemplatesLocallyOrDefault", "jkube.deploy.processTemplatesLocally", "false", false},
      new Object[] { "getOpenShiftManifestOrDefault", "jkube.openshiftManifest", Paths.get("META-INF", "jkube", "openshift.yml").toString(), Paths.get("META-INF", "jkube", "openshift.yml").toFile()},
      new Object[] { "getImageStreamManifestOrDefault", "jkube.openshiftImageStreamManifest", Paths.get("test-project-is.yml").toString(), Paths.get("test-project-is.yml").toFile()}
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
    final OpenShiftExtension extension = new TestOpenShiftExtension();
    extension.javaProject = JavaProject.builder()
      .artifactId("test-project")
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
