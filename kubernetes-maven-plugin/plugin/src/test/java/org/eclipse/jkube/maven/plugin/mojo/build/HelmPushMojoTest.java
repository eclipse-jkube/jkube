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
package org.eclipse.jkube.maven.plugin.mojo.build;

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class HelmPushMojoTest {

  @Mocked
  KitLogger logger;
  @Mocked
  JavaProject javaProject;
  @Mocked
  HelmService helmService;
  @Mocked
  SecDispatcher secDispatcher;

  private Properties mavenProperties;

  private HelmPushMojo helmPushMojo;

  @Before
  public void setUp() throws Exception {
    mavenProperties = new Properties();
    helmPushMojo = new HelmPushMojo();
    helmPushMojo.helm = new HelmConfig();
    helmPushMojo.javaProject = javaProject;
    helmPushMojo.settings = new Settings();
    helmPushMojo.securityDispatcher = secDispatcher;
    helmPushMojo.log = logger;
    // @formatter:off
    new Expectations() {{
      javaProject.getProperties(); result = mavenProperties; minTimes = 0;
      secDispatcher.decrypt(anyString);
      result = new Delegate<String>() {String delegate(String arg) {return arg;}}; minTimes = 0;
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
    javaProject = null;
    helmPushMojo = null;
    helmService = null;
  }

  @Test
  public void executeInternalWithSkipShouldSkipExecution() throws Exception {
    // Given
    helmPushMojo.skip = true;
    // When
    helmPushMojo.executeInternal();
    // Then
    new Verifications() {{
      HelmService.uploadHelmChart(helmPushMojo.helm, withNotNull(), withNotNull(), logger);
      times = 0;
    }};
  }

  @Test
  public void executeInternal_shouldCallHelmServiceForUpload() throws Exception {
    // Given
    // When
    helmPushMojo.executeInternal();

    // Then
    assertHelmServiceUpload();
  }

  @Test
  public void executeInternal_whenHelmServiceThrowsException_shouldThrowMojoExecutionException() throws Exception {
    // Given
    BadUploadException badUploadException = new BadUploadException("Failure");
    new Expectations() {{
      HelmService.uploadHelmChart(helmPushMojo.helm, withNotNull(), withNotNull(), logger);
      result = badUploadException;
    }};
    // When
    MojoExecutionException mojoExecutionException = assertThrows(MojoExecutionException.class, () -> helmPushMojo.executeInternal());

    // Then
    assertThat(mojoExecutionException).hasMessage("Failure");
    new Verifications() {{
      logger.error("Error performing helm push", badUploadException);
    }};
  }

  @Test
  public void canExecuteWithSkipShouldReturnFalse() {
    // Given
    helmPushMojo.skip = true;
    // When
    final boolean result = helmPushMojo.canExecute();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void getMavenPasswordDecryptionMethod_whenInvoked_callSecDispatcherForDecryption() throws SecDispatcherException {
    // Given + When
    String result = helmPushMojo.getMavenPasswordDecryptionMethod("foo");

    // Then
    assertThat(result).isEqualTo("foo");
    new Verifications() {{
      secDispatcher.decrypt("foo");
      times = 1;
    }};
  }

  @Test
  public void getMavenPasswordDecryptionMethod_whenInvoked_throwsException() throws SecDispatcherException {
    // Given
    new Expectations() {{
      secDispatcher.decrypt(anyString);
      result = new SecDispatcherException("Failure");
    }};

    // When
    String result = helmPushMojo.getMavenPasswordDecryptionMethod("foo");

    // Then
    assertThat(result).isNull();
    new Verifications() {{
      logger.error("Failure in decrypting password");
      times = 1;
    }};
  }

  private void assertHelmServiceUpload() throws Exception {
    // @formatter:off
    new Verifications() {{
      HelmService.uploadHelmChart(helmPushMojo.helm, withNotNull(), withNotNull(), logger);
      times = 1;
    }};
    // @formatter:on
  }
}
