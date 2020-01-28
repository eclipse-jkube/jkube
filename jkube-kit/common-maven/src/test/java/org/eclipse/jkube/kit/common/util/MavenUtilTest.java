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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
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
import org.eclipse.jkube.kit.common.JkubeProject;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MavenUtilTest {
    @Test
    public void testJkubeProjectConversion() throws DependencyResolutionRequiredException {
        MavenProject mavenProject = getMavenProject();

        JkubeProject jkubeProject = MavenUtil.convertMavenProjectToJkubeProject(mavenProject, getMavenSession());
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
    }

    @Test
    public void testDependencyParsing() throws DependencyResolutionRequiredException {
        MavenProject project = getMavenProject();
        Dependency dependency = new Dependency();
        dependency.setArtifactId("foo-dependency");
        dependency.setGroupId("org.eclipse.jkube");
        dependency.setVersion("0.1.0");

        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId("bar-dependency");
        dependency1.setGroupId("org.eclipse.jkube");
        dependency1.setVersion("0.1.0");

        project.setDependencies(Arrays.asList(dependency, dependency1));

        JkubeProject jkubeProject = MavenUtil.convertMavenProjectToJkubeProject(project, getMavenSession());
        assertEquals(2, jkubeProject.getDependencies().size());
        assertEquals("[org.eclipse.jkube,foo-dependency,0.1.0, org.eclipse.jkube,bar-dependency,0.1.0]", MavenUtil.getDependenciesAsString(project).toString());
    }

    @Test
    public void testLoadedPomFromFile() throws Exception {
        MavenProject mavenProject = loadMavenProjectFromPom();
        JkubeProject project = MavenUtil.convertMavenProjectToJkubeProject(mavenProject, getMavenSession());

        assertEquals("Eclipse Jkube Maven :: Sample :: Spring Boot Web", project.getName());
        assertEquals("Minimal Example with Spring Boot", project.getDescription());
        assertEquals("jkube-maven-sample-spring-boot", project.getArtifactId());
        assertEquals("org.eclipse.jkube", project.getGroupId());
        assertEquals("0.1.1-SNAPSHOT", project.getVersion());
        assertEquals(3, project.getDependencies().size());
        List<String> dependenciesAsStr = MavenUtil.getDependenciesAsString(mavenProject);
        assertEquals("org.springframework.boot,spring-boot-starter-web,null", dependenciesAsStr.get(0));
        assertEquals("org.springframework.boot,spring-boot-starter-actuator,null", dependenciesAsStr.get(1));
        assertEquals("org.jolokia,jolokia-core,1.6.2", dependenciesAsStr.get(2));

        List<String> pluginsAsStr = MavenUtil.getPluginsAsString(mavenProject);
        assertEquals(2, pluginsAsStr.size());
        assertEquals("org.springframework.boot,spring-boot-maven-plugin,null,null", pluginsAsStr.get(0));
        assertEquals("org.eclipse.jkube,k8s-maven-plugin,0.1.0,<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<configuration>\n" +
                "  <resources>\n" +
                "    <labels>\n" +
                "      <all>\n" +
                "        <testProject>spring-boot-sample</testProject>\n" +
                "      </all>\n" +
                "    </labels>\n" +
                "  </resources>\n" +
                "  <generator>\n" +
                "    <includes>\n" +
                "      <include>spring-boot</include>\n" +
                "    </includes>\n" +
                "    <config>\n" +
                "      <spring-boot>\n" +
                "        <color>always</color>\n" +
                "      </spring-boot>\n" +
                "    </config>\n" +
                "  </generator>\n" +
                "  <enricher>\n" +
                "    <excludes>\n" +
                "      <exclude>jkube-expose</exclude>\n" +
                "    </excludes>\n" +
                "    <config>\n" +
                "      <jkube-service>\n" +
                "        <type>NodePort</type>\n" +
                "      </jkube-service>\n" +
                "    </config>\n" +
                "  </enricher>\n" +
                "</configuration>,resource|build|helm", pluginsAsStr.get(1));
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

        return new MavenSession(null, settings, localRepository, null, null, Collections.<String>emptyList(), ".", null, null, new Date(System.currentTimeMillis()));
    }
}
