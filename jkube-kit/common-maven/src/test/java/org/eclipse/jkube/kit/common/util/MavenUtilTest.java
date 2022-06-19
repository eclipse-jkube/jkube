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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;


import org.apache.maven.model.Build;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Site;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.MavenSession;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@RunWith(MockitoJUnitRunner.class)
public class MavenUtilTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mock
  private KitLogger log;
  @Mock
  MavenProject mavenProject;
  @Mock
  BuildPluginManager pluginManager;
  @Mock
  MavenSession session;

  @Test
  public void testJKubeProjectConversion() throws DependencyResolutionRequiredException {
    MavenProject mavenProject = getMavenProject();

    JavaProject project = MavenUtil.convertMavenProjectToJKubeProject(mavenProject, getMavenSession());
    assertEquals("testProject", project.getName());
    assertEquals("org.eclipse.jkube", project.getGroupId());
    assertEquals("test-project", project.getArtifactId());
    assertEquals("0.1.0", project.getVersion());
    assertEquals("test description", project.getDescription());
    assertEquals("target", project.getOutputDirectory().getName());
    assertEquals(".", project.getBuildDirectory().getName());
    assertEquals("https://www.eclipse.org/jkube/", project.getDocumentationUrl());
    assertEquals(1, mavenProject.getCompileClasspathElements().size());
    assertEquals("./target", mavenProject.getCompileClasspathElements().get(0));
    assertEquals("bar", project.getProperties().get("foo"));
    assertEquals("https://projects.eclipse.org/projects/ecd.jkube", project.getUrl());
    assertEquals(Collections.singletonList(org.eclipse.jkube.kit.common.Maintainer.builder()
        .name("Dev1")
        .email("dev1@eclipse.org")
        .build()), project.getMaintainers());
  }

  @Test
  public void testGetDependencies() {
    // Given
    final org.apache.maven.model.Dependency dep1 = new org.apache.maven.model.Dependency();
    dep1.setGroupId("org.eclipse.jkube");
    dep1.setArtifactId("artifact1");
    dep1.setVersion("1.33.7");
    dep1.setType("war");
    dep1.setScope("compile");
    final org.apache.maven.model.Dependency dep2 = dep1.clone();
    dep2.setArtifactId("artifact2");
    dep2.setType("jar");
    when(mavenProject.getDependencies()).thenReturn(Arrays.asList(dep1, dep2));
    // When
    final List<Dependency> dependencies = MavenUtil.getDependencies(mavenProject);
    // Then
    assertThat(dependencies).hasSize(2);
  }

  @Test
  public void testGetTransitiveDependencies() {
    // Given
    final Artifact artifact1 = new DefaultArtifact("org.eclipse.jkube", "foo-dependency", "1.33.7",
        "runtime", "jar", "", new DefaultArtifactHandler("jar"));
    final Artifact artifact2 = new DefaultArtifact("org.eclipse.jkube", "bar-dependency", "1.33.7",
        "runtime", "jar", "", new DefaultArtifactHandler("jar"));
    when(mavenProject.getArtifacts()).thenReturn(new HashSet<>(Arrays.asList(artifact1, artifact2)));
    // When
    final List<Dependency> result = MavenUtil.getTransitiveDependencies(mavenProject);
    // Then
    assertThat(result).hasSize(2)
        .contains(Dependency.builder().groupId("org.eclipse.jkube").artifactId("foo-dependency").version("1.33.7")
            .type("jar").scope("runtime").build(),
            Dependency.builder().groupId("org.eclipse.jkube").artifactId("bar-dependency").version("1.33.7")
                .type("jar").scope("runtime").build());
  }

  @Test
  public void testLoadedPomFromFile() throws Exception {
    MavenProject mavenProject = loadMavenProjectFromPom();
    JavaProject project = MavenUtil.convertMavenProjectToJKubeProject(mavenProject, getMavenSession());

    assertEquals("Eclipse JKube Maven :: Sample :: Spring Boot Web", project.getName());
    assertEquals("Minimal Example with Spring Boot", project.getDescription());
    assertEquals("jkube-maven-sample-spring-boot", project.getArtifactId());
    assertEquals("org.eclipse.jkube", project.getGroupId());
    assertEquals("0.1.1-SNAPSHOT", project.getVersion());

    List<Plugin> plugins = MavenUtil.getPlugins(mavenProject);
    assertEquals(2, plugins.size());
    assertEquals("org.springframework.boot", plugins.get(0).getGroupId());
    assertEquals("spring-boot-maven-plugin", plugins.get(0).getArtifactId());
    assertEquals("org.eclipse.jkube", plugins.get(1).getGroupId());
    assertEquals("kubernetes-maven-plugin", plugins.get(1).getArtifactId());
    assertEquals("0.1.0", plugins.get(1).getVersion());
    assertEquals(3, plugins.get(1).getExecutions().size());
    assertEquals(Arrays.asList("resource", "build", "helm"), plugins.get(1).getExecutions());
  }

  private MavenProject getMavenProject() {
    MavenProject mavenProject = new MavenProject();
    File baseDir = new File("test-project-base-dir");
    mavenProject.setFile(baseDir);
    mavenProject.setName("testProject");
    mavenProject.setGroupId("org.eclipse.jkube");
    mavenProject.setArtifactId("test-project");
    mavenProject.setVersion("0.1.0");
    mavenProject.setDescription("test description");
    Build build = new Build();
    org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();
    plugin.setGroupId("org.apache.maven.plugins");
    plugin.setArtifactId("maven-help-plugin");
    plugin.setVersion("3.2.0");
    build.addPlugin(plugin);
    build.setOutputDirectory("./target");
    build.setDirectory(".");
    mavenProject.setBuild(build);
    DistributionManagement distributionManagement = new DistributionManagement();
    Site site = new Site();
    site.setUrl("https://www.eclipse.org/jkube/");
    distributionManagement.setSite(site);
    mavenProject.setDistributionManagement(distributionManagement);
    mavenProject.setUrl("https://projects.eclipse.org/projects/ecd.jkube");
    Developer developer = new Developer();
    developer.setName("Dev1");
    developer.setEmail("dev1@eclipse.org");
    mavenProject.setDevelopers(Collections.singletonList(developer));
    return mavenProject;
  }

  private MavenProject loadMavenProjectFromPom() throws IOException, XmlPullParserException, URISyntaxException {
    MavenXpp3Reader mavenreader = new MavenXpp3Reader();
    File pomfile = new File(getClass().getResource("/util/test-pom.xml").toURI());
    final FileReader reader = new FileReader(pomfile);
    final Model model = mavenreader.read(reader);
    model.setPomFile(pomfile);
    model.getBuild().setOutputDirectory(temporaryFolder.newFolder("outputDirectory").getAbsolutePath());
    model.getBuild().setDirectory(temporaryFolder.newFolder("build").getAbsolutePath());
    return new MavenProject(model);
  }

  private MavenSession getMavenSession() {
    Settings settings = new Settings();
    ArtifactRepository localRepository = new MavenArtifactRepository() {
      public String getBasedir() {
        return "repository";
      }
    };

    Properties userProperties = new Properties();
    userProperties.put("user.maven.home", "/home/user/.m2");

    Properties systemProperties = new Properties();
    systemProperties.put("foo", "bar");

    return new MavenSession(null, settings, localRepository, null, null, Collections.<String> emptyList(), ".",
        systemProperties, userProperties, new Date(System.currentTimeMillis()));
  }
  @Test
  public void testCallMavenPluginWithGoal() {
    try (MockedConstruction<MojoExecutionService> mojoExecutionServiceMocked = Mockito.mockConstruction(MojoExecutionService.class)) {
      // Given
      MavenProject mavenProject = getMavenProject();
      MavenSession mavenSession = getMavenSession();

      // When
      MavenUtil.callMavenPluginWithGoal(mavenProject, mavenSession, pluginManager,
              "org.apache.maven.plugins:maven-help-plugin:help", log);

      // Then
      verify(mojoExecutionServiceMocked.constructed().iterator().next(), times(1))
              .callPluginGoal("org.apache.maven.plugins:maven-help-plugin:help");
    }
  }

  @Test
  public void testgetRootProjectFolder() {
    // Given
    File projectBaseDir = new File("projectBaseDir");
    when(mavenProject.getBasedir()).thenReturn(projectBaseDir);
    when(mavenProject.getParent()).thenReturn(null);

    // When
    File rootFolder = MavenUtil.getRootProjectFolder(mavenProject);

    // Then
    assertNotNull(rootFolder);
    assertEquals("projectBaseDir", rootFolder.getName());
  }
}
