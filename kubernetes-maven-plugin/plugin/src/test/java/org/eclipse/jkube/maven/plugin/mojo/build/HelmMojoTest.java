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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.eclipse.jkube.kit.resource.helm.Maintainer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HelmMojoTest {

  @Mocked
  MavenProject mavenProject;
  @Mocked
  HelmService helmService;
  private HelmMojo helmMojo;

  @Before
  public void setUp() {
    helmMojo = new HelmMojo();
    helmMojo.project = mavenProject;
  }

  @After
  public void tearDown() {
    mavenProject = null;
    helmMojo = null;
    helmService = null;
  }

  @Test
  public void executeInternalWithNoConfigShouldInitConfigWithDefaultValuesAndGenerate(
      @Mocked Scm scm, @Mocked Developer developer) throws Exception {

    // Given
    assertThat(helmMojo.helm, nullValue());
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
    helmMojo.executeInternal();
    // Then
    assertThat(helmMojo.helm, notNullValue());
    assertThat(helmMojo.helm.getChart(), is("artifact-id"));
    assertThat(helmMojo.helm.getChartExtension(), is("tar.gz"));
    assertThat(helmMojo.helm.getVersion(), is("1337"));
    assertThat(helmMojo.helm.getDescription(), is("A description from Maven"));
    assertThat(helmMojo.helm.getHome(), is("https://project.url"));
    assertThat(helmMojo.helm.getSources(), contains("https://scm.url"));
    assertThat(helmMojo.helm.getMaintainers(), contains(
        new Maintainer("John", "john@example.com")
    ));
    assertThat(helmMojo.helm.getIcon(), nullValue());
    assertThat(helmMojo.helm.getAdditionalFiles(), empty());
    assertThat(helmMojo.helm.getTemplates(), empty());
    assertThat(helmMojo.helm.getTypes(), contains(HelmConfig.HelmType.KUBERNETES));
    assertThat(helmMojo.helm.getSourceDir(), is("target/classes/META-INF/jkube/"));
    assertThat(helmMojo.helm.getOutputDir(), is("target/jkube/helm/artifact-id"));
    assertThat(helmMojo.helm.getTarballOutputDir(), is("target"));
    new Verifications() {{
      HelmService.generateHelmCharts(null, helmMojo.helm);
      times = 1;
    }};
  }

  @Test(expected = MojoExecutionException.class)
  public void executeInternalWithNoConfigGenerateThrowsExceptionShouldRethrowWithMojoExecutionException()
  throws Exception {

    // Given
    new Expectations() {{
      mavenProject.getProperties();
      result = new Properties();
      HelmService.generateHelmCharts(null, withNotNull());
      result = new IOException("Exception is thrown");
    }};
    // When
    helmMojo.executeInternal();
    // Then
    fail();
  }

  @Test
  public void executeInternalFindTemplatesFromProvidedFile(
      @Mocked File kubernetesTemplate, @Mocked ResourceUtil resourceUtil, @Mocked Template template) throws Exception {

    // Given
    helmMojo.kubernetesTemplate = kubernetesTemplate;
    new Expectations() {{
      mavenProject.getProperties();
      result = new Properties();
      ResourceUtil.load(kubernetesTemplate, KubernetesResource.class, ResourceFileType.yaml);
      result = template;
    }};
    // When
    helmMojo.executeInternal();
    // Then
    assertThat(helmMojo.helm.getTemplates(), contains(template));
  }

  @Test
  public void executeInternalFindIconUrlFromProvidedFile(
      @Mocked File kubernetesManifest, @Mocked ResourceUtil resourceUtil, @Mocked HasMetadata listEntry) throws Exception {

    // Given
    helmMojo.kubernetesManifest = kubernetesManifest;
    new Expectations() {{
      mavenProject.getProperties();
      result = new Properties();
      kubernetesManifest.isFile();
      result = true;
      ResourceUtil.load(kubernetesManifest, KubernetesResource.class);
      result = new KubernetesList("List", Collections.singletonList(listEntry), "Invented", null);
      listEntry.getMetadata().getAnnotations();
      result = Collections.singletonMap("jkube.io/iconUrl", "https://my-icon");
    }};
    // When
    helmMojo.executeInternal();
    // Then
    assertThat(helmMojo.helm.getIcon(), is("https://my-icon"));
  }

  @Test
  public void initFromPropertyOrDefaultPropertyHasPrecedenceOverConfiguration() {
    // Given
    final Properties properties = new Properties();
    properties.put("jkube.helm.property", "Overrides Current Value");
    properties.put("jkube.helm.otherProperty", "Ignored");
    final HelmConfig config = new HelmConfig();
    config.setChart("This value will be overridden");
    new Expectations() {{
        mavenProject.getProperties();
        result = properties;
    }};
    // When
    helmMojo.initFromPropertyOrDefault(
        "jkube.helm.property", config::getChart, config::setChart, "default is ignored");
    // Then
    assertThat(config.getChart(), is("Overrides Current Value"));
  }

}
