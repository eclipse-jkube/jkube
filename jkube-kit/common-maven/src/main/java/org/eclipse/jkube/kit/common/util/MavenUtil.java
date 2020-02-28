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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Site;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.jkube.kit.common.util.ClassUtil.createClassLoader;
import static org.eclipse.jkube.kit.common.util.EnvUtil.greaterOrEqualsVersion;

/**
 * @author roland
 * @since 31/03/16
 */
public class MavenUtil {
    private static final transient Logger LOG = LoggerFactory.getLogger(MavenUtil.class);

    private static final String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";

    public static boolean isKubernetesJsonArtifact(String classifier, String type) {
        return "json".equals(type) && "kubernetes".equals(classifier);
    }

    public static boolean hasKubernetesJson(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
            for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
                if (entry.getName().equals(DEFAULT_CONFIG_FILE_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static URLClassLoader getCompileClassLoader(MavenProject project) {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            return createClassLoader(classpathElements, project.getBuild().getOutputDirectory());
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalArgumentException("Cannot resolve artifact from compile classpath",e);
        }
    }

    public static URLClassLoader getTestClassLoader(MavenProject project) {
        try {
            List<String> classpathElements = project.getTestClasspathElements();
            return createClassLoader(classpathElements, project.getBuild().getTestOutputDirectory());
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalArgumentException("Cannot resolve artifact from test classpath", e);
        }
    }



    // ====================================================

    /**
     * Returns true if the maven project has a dependency with the given groupId and artifactId (if not null)
     *
     * @param project MavenProject object for project
     * @param groupId group id of project
     * @param artifactId artifact id of project
     * @return boolean value indicating whether dependency is there or not
     */
    public static boolean hasDependency(MavenProject project, String groupId, String artifactId) {
        return getDependencyVersion(project, groupId, artifactId) != null;
    }

    /**
     * Returns the version associated to the dependency with the given groupId and artifactId (if present)
     *
     * @param project MavenProject object for project
     * @param groupId group id
     * @param artifactId artifact id
     * @return version associated to dependency
     */
    public static String getDependencyVersion(MavenProject project, String groupId, String artifactId) {
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                String scope = artifact.getScope();
                if (Objects.equal("test", scope)) {
                    continue;
                }
                if (artifactId != null && !Objects.equal(artifactId, artifact.getArtifactId())) {
                    continue;
                }
                if (Objects.equal(groupId, artifact.getGroupId())) {
                    return artifact.getVersion();
                }
            }
        }
        return null;
    }

    public static boolean hasPlugin(MavenProject project, String groupId, String artifactId) {
        return project.getPlugin(groupId + ":" + artifactId) != null;
    }

    public static boolean hasPluginOfAnyGroupId(MavenProject project, String pluginArtifact) {
        return getPluginOfAnyGroupId(project, pluginArtifact) != null;
    }

    public static Plugin getPluginOfAnyGroupId(MavenProject project, String pluginArtifact) {
        return getPlugin(project, null, pluginArtifact);
    }

    /**
     * Returns a comma separated string with dependency list in format
     *  groupId,artifactId,version,configuration,execution1|execution2|execution3
     *
     * @param project Maven project
     * @return list of dependencies
     */
    public static List<JKubeProjectPlugin> getPluginsAsString(MavenProject project) {
        List<JKubeProjectPlugin> projectPlugins = new ArrayList<>();
        for (Plugin plugin : project.getBuildPlugins()) {
            JKubeProjectPlugin.Builder jkubeProjectPluginBuilder = new JKubeProjectPlugin.Builder();

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

    public static List<String> getPluginExecutionsAsList(Plugin plugin) {
        List<String> pluginExecutions = new ArrayList<>();
        for (PluginExecution pluginExecution : plugin.getExecutions()) {
            pluginExecutions.addAll(pluginExecution.getGoals());
        }
        return pluginExecutions;
    }

    public static List<String> getDependenciesAsString(MavenProject project, boolean transitive) {
        final Set<Artifact> artifacts = transitive ?
                project.getArtifacts() : project.getDependencyArtifacts();
        final List<String> jkubeProjectDependenciesAsStr = new ArrayList<>();

        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                jkubeProjectDependenciesAsStr.add(
                        artifact.getGroupId() + "," +
                                artifact.getArtifactId() + "," +
                                artifact.getVersion() + "," +
                                artifact.getType() + "," +
                                artifact.getScope() + "," +
                                (artifact.getFile() != null ? artifact.getFile().getAbsolutePath() : ""));
            }
        }

        return jkubeProjectDependenciesAsStr;
    }

    /**
     * Returns the plugin with the given groupId (if present) and artifactId.
     *
     * @param project MavenProject of project
     * @param groupId group id
     * @param artifactId artifact id
     * @return return Plugin object for the specific plugin
     */
    public static Plugin getPlugin(MavenProject project, String groupId, String artifactId) {
        if (artifactId == null) {
            throw new IllegalArgumentException("artifactId cannot be null");
        }

        List<Plugin> plugins = project.getBuildPlugins();
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                boolean matchesArtifactId = artifactId.equals(plugin.getArtifactId());
                boolean matchesGroupId = groupId == null || groupId.equals(plugin.getGroupId());

                if (matchesGroupId && matchesArtifactId) {
                    return plugin;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if any of the given resources could be found on the given class loader
     *
     * @param project project object
     * @param paths array of strings as path
     * @return boolean value indicating whether that project has resource or not
     */
    public static boolean hasResource(MavenProject project, String... paths) {
        URLClassLoader compileClassLoader = getCompileClassLoader(project);
        for (String path : paths) {
            try {
                if (compileClassLoader.getResource(path) != null) {
                    return true;
                }
            } catch (Throwable e) {
                // ignore
            }
        }
        return false;
    }

    public static void createArchive(File sourceDir, File destinationFile, TarArchiver archiver) throws IOException {
        try {
            archiver.setCompression(TarArchiver.TarCompressionMethod.gzip);
            archiver.setLongfile(TarLongFileMode.posix);
            archiver.addDirectory(sourceDir);
            archiver.setDestFile(destinationFile);
            archiver.createArchive();
        } catch (IOException e) {
            throw new IOException("Failed to create archive " + destinationFile + ": " + e, e);
        }
    }


    public static void createArchive(File sourceDir, File destinationFile, ZipArchiver archiver) throws IOException {
        try {
            archiver.addDirectory(sourceDir);
            archiver.setDestFile(destinationFile);
            archiver.createArchive();
        } catch (IOException e) {
            throw new IOException("Failed to create archive " + destinationFile + ": " + e, e);
        }
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
        String path = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
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

    /**
     * Return all properties in Maven project, merged with all System properties (-D flags sent to Maven).
     * <p>
     * System properties always takes precedence.
     *
     * @param project Project to extract Properties from
     * @return Properties merged
     */
    public static Properties getPropertiesWithSystemOverrides(MavenProject project) {
        Properties properties = new Properties(project.getProperties());
        properties.putAll(System.getProperties());
        return properties;
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
    public static String getDocumentationUrl (MavenProject project) {
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

    public static Optional<List<String>> getCompileClasspathElementsIfRequested(MavenProject project, boolean useProjectClasspath) throws IOException {
        if (!useProjectClasspath) {
            return Optional.empty();
        }

        try {
            return Optional.of(project.getCompileClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            throw new IOException("Cannot extract compile class path elements", e);
        }
    }

    public static List<RegistryServerConfiguration> getRegistryServerFromMavenSettings(Settings settings) {
        List<RegistryServerConfiguration> registryServerConfigurations = new ArrayList<>();
        for (Server server : settings.getServers()) {
            if (server.getUsername() != null) {
                registryServerConfigurations.add(new RegistryServerConfiguration.Builder()
                        .id(server.getId())
                        .username(server.getUsername())
                        .password(server.getPassword())
                        .configuration(MavenConfigurationExtractor.extract((Xpp3Dom) server.getConfiguration()))
                        .build());
            }
        }
        return registryServerConfigurations;
    }

    public static JKubeProject convertMavenProjectToJKubeProject(MavenProject mavenProject, MavenSession mavenSession) throws DependencyResolutionRequiredException {
        JKubeProject.Builder builder = new JKubeProject.Builder();

        Properties properties = new Properties();
        String localRepositoryBaseDir = null;

        if (mavenProject.getProperties() != null) {
            properties.putAll(mavenProject.getProperties());
        }
        if (mavenSession != null) {
            if (mavenSession.getLocalRepository().getBasedir() != null) {
                localRepositoryBaseDir = mavenSession.getLocalRepository().getBasedir();
            }
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
                .dependencies(MavenUtil.getDependenciesAsString(mavenProject, false))
                .dependenciesWithTransitive(MavenUtil.getDependenciesAsString(mavenProject, true))
                .localRepositoryBaseDirectory(localRepositoryBaseDir)
                .plugins(MavenUtil.getPluginsAsString(mavenProject));

        if (mavenProject.getOrganization() != null) {
            builder.site(mavenProject.getOrganization().getUrl())
                    .organization(mavenProject.getOrganization().getName());
        }

        if (mavenProject.getBuild() != null) {
            builder.outputDirectory(mavenProject.getBuild().getOutputDirectory())
                    .buildFinalName(mavenProject.getBuild().getFinalName())
                    .buildDirectory(mavenProject.getBuild().getDirectory());
        }

        if (mavenProject.getIssueManagement() != null) {
            builder.issueManagementSystem(mavenProject.getIssueManagement().getSystem());
            builder.issueManagementUrl(mavenProject.getIssueManagement().getUrl());
        }

        if (mavenProject.getScm() != null) {
            builder.scmTag(mavenProject.getScm().getTag());
            builder.scmUrl(mavenProject.getScm().getUrl());
        }

        return builder.build();
    }
}

