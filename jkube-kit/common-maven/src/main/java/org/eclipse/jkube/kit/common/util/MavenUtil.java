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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.plugin.BuildPluginManager;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Site;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static org.eclipse.jkube.kit.common.util.EnvUtil.greaterOrEqualsVersion;

/**
 * @author roland
 */
public class MavenUtil {
    private MavenUtil() {}

    /**
     * Returns a list of {@link Plugin}
     *
     * @param project Maven project
     * @return list of plugins
     */
    public static List<Plugin> getPlugins(MavenProject project) {
        List<Plugin> projectPlugins = new ArrayList<>();
        for (org.apache.maven.model.Plugin plugin : project.getBuildPlugins()) {
            Plugin.PluginBuilder jkubeProjectPluginBuilder = Plugin.builder();

            jkubeProjectPluginBuilder.groupId(plugin.getGroupId())
                    .artifactId(plugin.getArtifactId())
                    .version(plugin.getVersion());

            if (plugin.getExecutions() != null && !plugin.getExecutions().isEmpty()) {
                jkubeProjectPluginBuilder.executions(getPluginExecutionsAsList(plugin));
            }

            jkubeProjectPluginBuilder.configuration(MavenConfigurationExtractor.extract((Xpp3Dom)plugin.getConfiguration()));
            projectPlugins.add(jkubeProjectPluginBuilder.build());
        }
        return projectPlugins;
    }

    public static List<String> getPluginExecutionsAsList(org.apache.maven.model.Plugin plugin) {
        List<String> pluginExecutions = new ArrayList<>();
        for (PluginExecution pluginExecution : plugin.getExecutions()) {
            pluginExecutions.addAll(pluginExecution.getGoals());
        }
        return pluginExecutions;
    }

  public static List<Dependency> getTransitiveDependencies(MavenProject project) {
    return project.getArtifacts().stream()
        .map(a -> Dependency.builder()
            .groupId(a.getGroupId()).artifactId(a.getArtifactId()).version(a.getVersion()).type(a.getType())
            .scope(a.getScope()).file(a.getFile()).build())
        .collect(Collectors.toList());
  }

  public static List<Dependency> getDependencies(MavenProject project) {
    return project.getDependencies().stream()
        .map(d -> Dependency.builder()
            .groupId(d.getGroupId()).artifactId(d.getArtifactId()).version(d.getVersion()).type(d.getType())
            .scope(d.getScope()).file(getArtifactFileFromArtifactMap(project, d)).build())
        .collect(Collectors.toList());
  }

    /**
     * Returns the version from the list of pre-configured versions of common groupId/artifact pairs
     *
     * @param groupId group id
     * @param artifactId artifact id
     * @return version according to that pair
     * @throws IOException IOException in case file is not found
     */
    public static String getVersion(String groupId, String artifactId) throws IOException {
        String path = String.format("META-INF/maven/%s/%s/pom.properties" , groupId, artifactId);
        InputStream in = MavenUtil.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException("Could not find " + path + " on classath!");
        }
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            throw new IOException("Failed to load " + path + ". " + e, e);
        }
        String version = properties.getProperty("version");
        if (StringUtils.isBlank(version)) {
            throw new IOException("No version property in " + path);

        }
        return version;
    }

    public static boolean isMaven350OrLater(MavenSession mavenSession) {
        // Maven enforcer and help:evaluate goals both use mavenSession.getSystemProperties(),
        // and it turns out that System.getProperty("maven.version") does not return the value.
        String mavenVersion = mavenSession.getSystemProperties().getProperty("maven.version", "3");
        return greaterOrEqualsVersion(mavenVersion, "3.5.0");
    }

    /**
     * Retrieves the URL used for documentation from the provided {@link MavenProject}.
     *
     * @param project MavenProject from which to retrieve the documentation URL
     * @return the documentation URL
     */
    public static String getDocumentationUrl(MavenProject project) {
        while (project != null) {
            DistributionManagement distributionManagement = project.getDistributionManagement();
            if (distributionManagement != null) {
                Site site = distributionManagement.getSite();
                if (site != null) {
                    return site.getUrl();
                }
            }
            project = project.getParent();
        }
        return null;
    }

    public static List<RegistryServerConfiguration> getRegistryServerFromMavenSettings(Settings settings) {
        List<RegistryServerConfiguration> registryServerConfigurations = new ArrayList<>();
        for (Server server : settings.getServers()) {
            if (server.getUsername() != null) {
                registryServerConfigurations.add(RegistryServerConfiguration.builder()
                        .id(server.getId())
                        .username(server.getUsername())
                        .password(server.getPassword())
                        .configuration(MavenConfigurationExtractor.extract((Xpp3Dom) server.getConfiguration()))
                        .build());
            }
        }
        return registryServerConfigurations;
    }

    public static JavaProject convertMavenProjectToJKubeProject(MavenProject mavenProject, MavenSession mavenSession) throws DependencyResolutionRequiredException {
        JavaProject.JavaProjectBuilder builder = JavaProject.builder();

        Properties properties = new Properties();

        if (mavenProject.getProperties() != null) {
            properties.putAll(mavenProject.getProperties());
        }
        if (mavenSession != null) {
            if (mavenSession.getUserProperties() != null) {
                properties.putAll(mavenSession.getUserProperties());
            }
            if (mavenSession.getSystemProperties() != null) {
                properties.putAll(mavenSession.getSystemProperties());
            }
            if (mavenSession.getExecutionProperties() != null) {
                properties.putAll(mavenSession.getExecutionProperties());
            }
        }

        builder.name(mavenProject.getName())
                .description(mavenProject.getDescription())
                .groupId(mavenProject.getGroupId())
                .artifactId(mavenProject.getArtifactId())
                .version(mavenProject.getVersion())
                .baseDirectory(mavenProject.getBasedir())
                .documentationUrl(getDocumentationUrl(mavenProject))
                .compileClassPathElements(mavenProject.getCompileClasspathElements())
                .properties(properties)
                .packaging(mavenProject.getPackaging())
                .dependencies(getDependencies(mavenProject))
                .dependenciesWithTransitive(getTransitiveDependencies(mavenProject))
                .plugins(getPlugins(mavenProject));

        if (mavenProject.getOrganization() != null) {
            builder.site(mavenProject.getOrganization().getUrl())
                    .organizationName(mavenProject.getOrganization().getName());
        }

        Optional.ofNullable(mavenProject.getBuild())
            .ifPresent(mavenBuild -> builder
                .outputDirectory(new File(mavenBuild.getOutputDirectory()))
                .buildFinalName(mavenBuild.getFinalName())
                .buildDirectory(new File(mavenBuild.getDirectory()))
                .buildPackageDirectory(new File(mavenBuild.getDirectory())));

        if (mavenProject.getIssueManagement() != null) {
            builder.issueManagementSystem(mavenProject.getIssueManagement().getSystem());
            builder.issueManagementUrl(mavenProject.getIssueManagement().getUrl());
        }

        if (mavenProject.getScm() != null) {
            builder.scmTag(mavenProject.getScm().getTag());
            builder.scmUrl(mavenProject.getScm().getUrl());
        }

        if (mavenProject.getArtifact() != null) {
            builder.artifact(mavenProject.getArtifact().getFile());
        }

        if (mavenProject.getUrl() != null) {
            builder.url(mavenProject.getUrl());
        }

        if (mavenProject.getDevelopers() != null) {
            builder.maintainers(Optional.of(mavenProject)
              .map(MavenProject::getDevelopers)
              .orElse(Collections.emptyList())
              .stream()
              .filter(developer -> StringUtils.isNotBlank(developer.getName()) || StringUtils.isNotBlank(developer.getEmail()))
              .map(developer -> new Maintainer(developer.getName(), developer.getEmail()))
              .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public static void callMavenPluginWithGoal(
        MavenProject project, MavenSession session, BuildPluginManager pluginManager, String mavenPluginGoal, KitLogger log) {

        if (mavenPluginGoal != null) {
            log.info("Calling %s Maven Goal", mavenPluginGoal);
            MojoExecutionService mojoExecutionService = new MojoExecutionService(project, session, pluginManager);
            mojoExecutionService.callPluginGoal(mavenPluginGoal);
        }
    }

    /**
     * Returns the root project folder
     */
    public static File getRootProjectFolder(MavenProject project) {
        File answer = null;
        while (project != null) {
            File basedir = project.getBasedir();
            if (basedir != null) {
                answer = basedir;
            }
            project = project.getParent();
        }
        return answer;
    }

    public static String getLastExecutingGoal(MavenSession session, String logPrefix) {
        List<String> goals = session.getGoals().stream()
            .filter(g -> g.startsWith(logPrefix))
            .collect(Collectors.toList());
        return goals.isEmpty() ? null : goals.get(goals.size() - 1).substring(logPrefix.length());
    }

    private static File getArtifactFileFromArtifactMap(MavenProject mavenProject, org.apache.maven.model.Dependency dependency) {
        Artifact artifact = mavenProject.getArtifactMap().get(dependency.getGroupId() + ":" + dependency.getArtifactId());
        if (artifact != null) {
            return artifact.getFile();
        }
        return null;
    }
}

