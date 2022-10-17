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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

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
import org.apache.maven.model.Build;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Site;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavenUtilTest {

  @TempDir
  Path temporaryFolder;

  private KitLogger log;
  private MavenProject mavenProject;
  private BuildPluginManager pluginManager;

  @BeforeEach
  void setUp() {
    mavenProject = mock(MavenProject.class);
    pluginManager = mock(BuildPluginManager.class);
    log = mock(KitLogger.class);
  }

  @Test
  void testJKubeProjectConversion() throws DependencyResolutionRequiredException {
    mavenProject = getMavenProject();

    JavaProject project = MavenUtil.convertMavenProjectToJKubeProject(mavenProject, getMavenSession());
    assertThat(project.getName()).isEqualTo("testProject");
    assertThat(project.getGroupId()).isEqualTo("org.eclipse.jkube");
    assertThat(project.getArtifactId()).isEqualTo("test-project");
    assertThat(project.getVersion()).isEqualTo("0.1.0");
    assertThat(project.getDescription()).isEqualTo("test description");
    assertThat(project.getOutputDirectory()).hasName("target");
    assertThat(project.getBuildDirectory()).hasName(".");
    assertThat(project.getDocumentationUrl()).isEqualTo("https://www.eclipse.org/jkube/");
    assertThat(mavenProject.getCompileClasspathElements()).hasSize(1);
    assertThat(mavenProject.getCompileClasspathElements()).first().isEqualTo("./target");
    assertThat(project.getProperties()).contains(entry("foo", "bar"));
    assertThat(project.getUrl()).isEqualTo("https://projects.eclipse.org/projects/ecd.jkube");
    assertThat(project.getMaintainers()).isEqualTo(
            Collections.singletonList(org.eclipse.jkube.kit.common.Maintainer.builder()
                    .name("Dev1")
                    .email("dev1@eclipse.org")
                    .build())
    );
  }

  @Test
  void testGetDependencies() {
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
  void testGetTransitiveDependencies() {
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
  void testLoadedPomFromFile() throws Exception {
    mavenProject = loadMavenProjectFromPom();
    JavaProject project = MavenUtil.convertMavenProjectToJKubeProject(mavenProject, getMavenSession());

    assertThat(project.getName()).isEqualTo("Eclipse JKube Maven :: Sample :: Spring Boot Web");
    assertThat(project.getDescription()).isEqualTo("Minimal Example with Spring Boot");
    assertThat(project.getArtifactId()).isEqualTo("jkube-maven-sample-spring-boot");
    assertThat(project.getGroupId()).isEqualTo("org.eclipse.jkube");
    assertThat(project.getVersion()).isEqualTo("0.1.1-SNAPSHOT");

    List<Plugin> plugins = MavenUtil.getPlugins(mavenProject);
    assertThat(plugins.get(0).getGroupId()).isEqualTo("org.springframework.boot");
    assertThat(plugins.get(0).getArtifactId()).isEqualTo("spring-boot-maven-plugin");
    assertThat(plugins.get(1).getGroupId()).isEqualTo("org.eclipse.jkube");
    assertThat(plugins.get(1).getArtifactId()).isEqualTo("kubernetes-maven-plugin");
    assertThat(plugins.get(1).getVersion()).isEqualTo("0.1.0");
    assertThat(plugins.get(1).getExecutions()).hasSize(3);
    assertThat(plugins.get(1).getExecutions()).isEqualTo(Arrays.asList("resource", "build", "helm"));
  }

  @Test
  void getLastExecutingGoal_whenSessionReturnsGoalsList_thenLastExecutingGoalReturned() {
    // Given
    MavenSession mavenSession = mock(MavenSession.class);
    when(mavenSession.getGoals()).thenReturn(Arrays.asList("clean", "k8s:build", "k8s:resource"));

    // When
    String lastExecutingGoal = MavenUtil.getLastExecutingGoal(mavenSession, "k8s:");

    // Then
    assertThat(lastExecutingGoal).isEqualTo("resource");
  }

  @Test
  void getLastExecutingGoal_whenSessionReturnsEmptyList_thenNullReturned() {
    // Given
    MavenSession mavenSession = mock(MavenSession.class);
    when(mavenSession.getGoals()).thenReturn(Collections.emptyList());

    // When
    String lastExecutingGoal = MavenUtil.getLastExecutingGoal(mavenSession, "k8s:");

    // Then
    assertThat(lastExecutingGoal).isNull();
  }

  private MavenProject getMavenProject() {
    mavenProject = new MavenProject();
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

  private MavenProject loadMavenProjectFromPom() throws IOException, XmlPullParserException {
    MavenXpp3Reader mavenreader = new MavenXpp3Reader();
    File pomfile = new File(getClass().getResource("/util/test-pom.xml").getFile());
    final FileReader reader = new FileReader(pomfile);
    final Model model = mavenreader.read(reader);
    model.setPomFile(pomfile);
    model.getBuild().setOutputDirectory(temporaryFolder.resolve("outputDirectory").toAbsolutePath().toString());
    model.getBuild().setDirectory(temporaryFolder.resolve("build").toAbsolutePath().toString());
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

    return new MavenSession(null, settings, localRepository, null, null, Collections.emptyList(), ".",
        systemProperties, userProperties, new Date(System.currentTimeMillis()));
  }

  @Test
  void testCallMavenPluginWithGoal() {
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
  void testgetRootProjectFolder() {
    // Given
    File projectBaseDir = new File("projectBaseDir");
    when(mavenProject.getBasedir()).thenReturn(projectBaseDir);
    when(mavenProject.getParent()).thenReturn(null);

    // When
    File rootFolder = MavenUtil.getRootProjectFolder(mavenProject);

    // Then
    assertThat(rootFolder).isNotNull().hasName("projectBaseDir");
  }
}
