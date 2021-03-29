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

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.eclipse.jkube.kit.resource.helm.Maintainer;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import static org.eclipse.jkube.maven.plugin.mojo.build.HelmPushMojo.PROPERTY_UPLOAD_REPO_SNAPSHOT_NAME;
import static org.eclipse.jkube.maven.plugin.mojo.build.HelmPushMojo.PROPERTY_UPLOAD_REPO_SNAPSHOT_TYPE;
import static org.eclipse.jkube.maven.plugin.mojo.build.HelmPushMojo.PROPERTY_UPLOAD_REPO_STABLE_NAME;
import static org.eclipse.jkube.maven.plugin.mojo.build.HelmPushMojo.PROPERTY_UPLOAD_REPO_STABLE_TYPE;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class HelmPushMojoTest {

  @Mocked
  private KitLogger logger;
  @Mocked
  MavenProject mavenProject;
  @Mocked
  HelmService helmService;
  private HelmPushMojo helmPushMojo;

  @Before
  public void setUp() {
    helmPushMojo = new HelmPushMojo();
    helmPushMojo.project = mavenProject;
  }

  @After
  public void tearDown() {
    mavenProject = null;
    helmPushMojo = null;
    helmService = null;
  }

  @Test
  public void executeInternalWithNoConfigShouldInitConfigWithDefaultValuesAndGenerate(
      @Mocked Scm scm, @Mocked Developer developer) throws Exception {

    // Given
    assertThat(helmPushMojo.helm, nullValue());
    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
      helmPushMojo.getHelmRepository(); result = new HelmRepository();
      helmPushMojo.setAuthentication(withInstanceOf(HelmRepository.class));
    }};
    new Expectations() {{
      mavenProject.getProperties();
      result = new Properties();
      mavenProject.getBuild().getOutputDirectory();
      result = "target/classes";
      mavenProject.getBuild().getDirectory();
      result = "target";
      mavenProject.getArtifactId();
      result = "artifact-id";
      mavenProject.getVersion();
      result = "1337";
      mavenProject.getDescription();
      result = "A description from Maven";
      mavenProject.getUrl();
      result = "https://project.url";
      mavenProject.getScm();
      result = scm;
      scm.getUrl();
      result = "https://scm.url";
      mavenProject.getDevelopers();
      result = Arrays.asList(developer, developer);
      developer.getName();
      result = "John"; result = "John"; result = null;
      developer.getEmail();
      result = "john@example.com"; result = null;
    }};
    // When
    helmPushMojo.executeInternal();
    // Then
    assertThat(helmPushMojo.helm, notNullValue());
    assertThat(helmPushMojo.helm.getChart(), is("artifact-id"));
    assertThat(helmPushMojo.helm.getChartExtension(), is("tar.gz"));
    assertThat(helmPushMojo.helm.getVersion(), is("1337"));
    assertThat(helmPushMojo.helm.getDescription(), is("A description from Maven"));
    assertThat(helmPushMojo.helm.getHome(), is("https://project.url"));
    assertThat(helmPushMojo.helm.getSources(), contains("https://scm.url"));
    assertThat(helmPushMojo.helm.getMaintainers(), contains(
        new Maintainer("John", "john@example.com")
    ));
    assertThat(helmPushMojo.helm.getIcon(), nullValue());
    assertThat(helmPushMojo.helm.getAdditionalFiles(), empty());
    assertThat(helmPushMojo.helm.getTemplates(), empty());
    assertThat(helmPushMojo.helm.getTypes(), contains(HelmConfig.HelmType.KUBERNETES));
    assertThat(helmPushMojo.helm.getSourceDir(), is("target/classes/META-INF/jkube/"));
    assertThat(helmPushMojo.helm.getOutputDir(), is("target/jkube/helm/artifact-id"));
    assertThat(helmPushMojo.helm.getTarballOutputDir(), is("target"));
    assertThat(helmPushMojo.helm.getSnapshotRepository(), nullValue());
    assertThat(helmPushMojo.helm.getStableRepository(), nullValue());
    assertThat(helmPushMojo.helm.getSecurity(), is("~/.m2/settings-security.xml"));
    new Verifications() {{
      HelmService.uploadHelmChart(null, helmPushMojo.helm, withNotNull());
      times = 1;
    }};
  }

  @Test(expected = MojoExecutionException.class)
  public void executeInternalWithNoConfigGenerateThrowsExceptionShouldRethrowWithMojoExecutionException()
  throws Exception {

    // Given
    helmPushMojo.skip = false;
    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
      helmPushMojo.getHelmRepository(); result = new HelmRepository();
      helmPushMojo.setAuthentication(withInstanceOf(HelmRepository.class));
    }};
    new Expectations() {{
      mavenProject.getProperties();
      result = new Properties();
      helmService.uploadHelmChart(null, withNotNull(), withNotNull());
      result = new BadUploadException("Exception is thrown");
    }};
    // When
    helmPushMojo.executeInternal();
    // Then
    fail();
  }

  @Test(expected = MojoExecutionException.class)
  public void executeInternalWithNoRepositoryGenerateThrowsExceptionShouldRethrowWithMojoExecutionException()
      throws Exception {

    // Given
    helmPushMojo.skip = false;
    new Expectations(helmPushMojo) {{
        helmPushMojo.getKitLogger(); result = logger;
    }};
    new Expectations() {{
      mavenProject.getProperties(); result = new Properties();
    }};
    // When
    helmPushMojo.executeInternal();
    // Then
    fail();
  }


  @Test
  public void initFromPropertyOrDefaultPropertyHasPrecedenceOverConfiguration() {
    // Given
    final Properties properties = new Properties();
    properties.put("jkube.helm.property", "Overrides Current Value");
    properties.put("jkube.helm.otherProperty", "Ignored");
    final HelmConfig config = new HelmConfig();
    config.setSecurity("This value will be overridden");
    new Expectations() {{
      mavenProject.getProperties();
      result = properties;
    }};
    // When
    helmPushMojo.initFromPropertyOrDefault(
        "jkube.helm.property", config::getChart, config::setChart, "default is ignored");
    // Then
    assertThat(config.getChart(), is("Overrides Current Value"));
  }

  @Test
  public void initRepositoriesFromProperties(@Mocked HelmConfig helmConfig) throws IOException, MojoExecutionException {
    // Given
    final Properties properties = new Properties();
    properties.put(PROPERTY_UPLOAD_REPO_STABLE_NAME, "stable");
    properties.put(PROPERTY_UPLOAD_REPO_STABLE_TYPE, HelmRepoType.ARTIFACTORY.name());
    properties.put(PROPERTY_UPLOAD_REPO_SNAPSHOT_NAME, "snapshot");
    properties.put(PROPERTY_UPLOAD_REPO_SNAPSHOT_TYPE, HelmRepoType.ARTIFACTORY.name());

    new Expectations() {{
      mavenProject.getProperties();
      result = properties;
    }};
    new Expectations(helmPushMojo, helmConfig) {{
      helmPushMojo.getHelm(); result = helmConfig;
    }};
    // When
    helmPushMojo.initDefaults();
    // Then
    assertThat(helmConfig.getStableRepository().getName(), is("stable"));
    assertThat(helmConfig.getSnapshotRepository().getName(), is("snapshot"));
  }

  @Test(expected = MojoExecutionException.class)
  public void initStableRepositoriesWithIncompletePropertiesThrowsException(@Mocked HelmConfig helmConfig) throws IOException, MojoExecutionException {
    // Given
    final Properties properties = new Properties();
    properties.put(PROPERTY_UPLOAD_REPO_STABLE_NAME, "stable");
    new Expectations() {{
      mavenProject.getProperties();
      result = properties;
    }};
    new Expectations(helmPushMojo, helmConfig) {{
      helmPushMojo.getKitLogger(); result = logger;
    }};
    // When
    helmPushMojo.initDefaults();
    // Then
    fail();
  }

  @Test(expected = MojoExecutionException.class)
  public void initSnapshotRepositoriesWithIncompletePropertiesThrowsException(@Mocked HelmConfig helmConfig) throws IOException, MojoExecutionException {
    // Given
    final Properties properties = new Properties();
    properties.put(PROPERTY_UPLOAD_REPO_SNAPSHOT_NAME, "stable");
    new Expectations() {{
      mavenProject.getProperties();
      result = properties;
    }};
    new Expectations(helmPushMojo, helmConfig) {{
      helmPushMojo.getKitLogger(); result = logger;
    }};
    // When
    helmPushMojo.initDefaults();
    // Then
    fail();
  }

  @Test
  public void executeSetAuthentication() throws SecDispatcherException {
    // Given
    final HelmRepository helmRepository = new HelmRepository();
    helmRepository.setName("repository");
    helmRepository.setUsername("username");
    helmRepository.setPassword("password");
    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
    }};
    // When
    helmPushMojo.setAuthentication(helmRepository);
    // Then
    assertThat(helmRepository.getName(), is("repository"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void executeSetAuthenticationWithUsernameWithoutPasswordThrowsException() throws SecDispatcherException {
    // Given
    final HelmRepository helmRepository = new HelmRepository();
    helmRepository.setName("repository");
    helmRepository.setUsername("username");
    // When
    helmPushMojo.setAuthentication(helmRepository);
    // Then
    fail();
  }


  @Test
  public void executeSetAuthenticationWithoutUsername(@Mocked Settings settings) throws SecDispatcherException {
    // Given
    final HelmRepository helmRepository = new HelmRepository();
    helmRepository.setName("repository");
    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
      helmPushMojo.getSettings(); result = settings;
      settings.getServer(anyString); result = withNull();
    }};
    // When
    helmPushMojo.setAuthentication(helmRepository);
    // Then
    assertThat(helmRepository.getUsername(), nullValue());
  }

  @Test
  public void executeSetAuthenticationWithoutUsernameWithNullServer(@Mocked Settings settings) throws SecDispatcherException {
    // Given
    final HelmRepository helmRepository = new HelmRepository();
    helmRepository.setName("repository");
    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
      helmPushMojo.getSettings(); result = settings;
      settings.getServer(anyString); result = withNull();
    }};
    // When
    helmPushMojo.setAuthentication(helmRepository);
    // Then
    assertThat(helmRepository.getUsername(), nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void executeSetAuthenticationWithoutUsernameWithServerThrowsException(@Mocked Settings settings) throws SecDispatcherException {
    // Given
    final HelmRepository helmRepository = new HelmRepository();
    helmRepository.setName("repository");
    final Server server = new Server();

    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
      helmPushMojo.getSettings(); result = settings;
      settings.getServer(anyString); result = server;
    }};
    // When
    helmPushMojo.setAuthentication(helmRepository);
    // Then
    fail();
  }

  @Test
  public void executeSetAuthenticationWithoutUsernameWithServer(@Mocked Settings settings, @Mocked
      SecDispatcher secDispatcher) throws SecDispatcherException {
    // Given
    final HelmRepository helmRepository = new HelmRepository();
    helmRepository.setName("repository");
    final Server server = new Server();
    server.setUsername("username");
    server.setPassword("password");

    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger;
      helmPushMojo.getSettings(); result = settings;
      settings.getServer(anyString); result = server;
      helmPushMojo.getSecDispatcher(); result = secDispatcher;
      secDispatcher.decrypt(anyString); result = "password";
    }};
    // When
    helmPushMojo.setAuthentication(helmRepository);
    // Then
    assertThat(helmRepository.getPassword(), is("password"));
  }

  @Test
  public void skipExecution() throws MojoFailureException, MojoExecutionException {
    // Given
    helmPushMojo.skip = true;
    new Expectations(helmPushMojo) {{
    }};
    // When
    helmPushMojo.canExecute();
    // Then
    new Verifications() {{
      helmPushMojo.execute();
      times = 0;
    }};
  }
}
