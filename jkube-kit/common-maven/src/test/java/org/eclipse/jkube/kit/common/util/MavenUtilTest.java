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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Site;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MavenUtilTest {
    @Test
    public void testJKubeProjectConversion() throws DependencyResolutionRequiredException {
        MavenProject mavenProject = getMavenProject();

        JKubeProject jkubeProject = MavenUtil.convertMavenProjectToJKubeProject(mavenProject, getMavenSession());
        assertEquals("testProject", jkubeProject.getName());
        assertEquals("org.eclipse.jkube", jkubeProject.getGroupId());
        assertEquals("test-project", jkubeProject.getArtifactId());
        assertEquals("0.1.0", jkubeProject.getVersion());
        assertEquals("test description", jkubeProject.getDescription());
        assertEquals("./target", jkubeProject.getOutputDirectory());
        assertEquals(".", jkubeProject.getBuildDirectory());
        assertEquals("https://www.eclipse.org/jkube/", jkubeProject.getDocumentationUrl());
        assertEquals(1, mavenProject.getCompileClasspathElements().size());
        assertEquals("./target", mavenProject.getCompileClasspathElements().get(0));
        assertEquals("bar", jkubeProject.getProperties().get("foo"));
    }

    @Test
    public void testDependencyParsing() throws DependencyResolutionRequiredException {
        MavenProject project = getMavenProject();
        Set<Artifact> dependencies = new HashSet<>();
        dependencies.add(new DefaultArtifact("org.eclipse.jkube", "foo-dependency", "0.1.0", "runtime", "jar", "", new DefaultArtifactHandler("jar")));
        dependencies.add(new DefaultArtifact("org.eclipse.jkube", "bar-dependency", "0.1.0", "runtime", "jar", "", new DefaultArtifactHandler("jar")));
        project.setDependencyArtifacts(dependencies);

        JKubeProject jkubeProject = MavenUtil.convertMavenProjectToJKubeProject(project, getMavenSession());
        assertEquals(2, jkubeProject.getDependencies().size());
        assertEquals("[org.eclipse.jkube,foo-dependency,0.1.0,jar,runtime,, org.eclipse.jkube,bar-dependency,0.1.0,jar,runtime,]", MavenUtil.getDependenciesAsString(project, false).toString());
    }

    @Test
    public void testLoadedPomFromFile() throws Exception {
        MavenProject mavenProject = loadMavenProjectFromPom();
        JKubeProject project = MavenUtil.convertMavenProjectToJKubeProject(mavenProject, getMavenSession());

        assertEquals("Eclipse JKube Maven :: Sample :: Spring Boot Web", project.getName());
        assertEquals("Minimal Example with Spring Boot", project.getDescription());
        assertEquals("jkube-maven-sample-spring-boot", project.getArtifactId());
        assertEquals("org.eclipse.jkube", project.getGroupId());
        assertEquals("0.1.1-SNAPSHOT", project.getVersion());

        List<JKubeProjectPlugin> plugins = MavenUtil.getPluginsAsString(mavenProject);
        assertEquals(2, plugins.size());
        assertEquals("org.springframework.boot", plugins.get(0).getGroupId());
        assertEquals("spring-boot-maven-plugin", plugins.get(0).getArtifactId());
        assertEquals("org.eclipse.jkube", plugins.get(1).getGroupId());
        assertEquals("k8s-maven-plugin", plugins.get(1).getArtifactId());
        assertEquals("0.1.0", plugins.get(1).getVersion());
        assertEquals(3, plugins.get(1).getExecutions().size());
        assertEquals(Arrays.asList("resource", "build", "helm"), plugins.get(1).getExecutions());
    }

    private MavenProject getMavenProject() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setName("testProject");
        mavenProject.setGroupId("org.eclipse.jkube");
        mavenProject.setArtifactId("test-project");
        mavenProject.setVersion("0.1.0");
        mavenProject.setDescription("test description");
        Build build = new Build();
        build.setOutputDirectory("./target");
        build.setDirectory(".");
        mavenProject.setBuild(build);
        DistributionManagement distributionManagement = new DistributionManagement();
        Site site = new Site();
        site.setUrl("https://www.eclipse.org/jkube/");
        distributionManagement.setSite(site);
        mavenProject.setDistributionManagement(distributionManagement);
        return mavenProject;
    }

    private MavenProject loadMavenProjectFromPom() throws IOException, XmlPullParserException {
        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        File pomfile = new File(getClass().getResource("/util/test-pom.xml").getFile());
        reader = new FileReader(pomfile);
        model = mavenreader.read(reader);
        model.setPomFile(pomfile);
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

        return new MavenSession(null, settings, localRepository, null, null, Collections.<String>emptyList(), ".", systemProperties, userProperties, new Date(System.currentTimeMillis()));
    }
}
