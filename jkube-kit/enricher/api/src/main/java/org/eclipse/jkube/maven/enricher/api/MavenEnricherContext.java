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
package org.eclipse.jkube.maven.enricher.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.maven.enricher.api.model.Dependency;
import org.eclipse.jkube.maven.enricher.api.util.MavenConfigurationExtractor;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * The context given to each enricher from where it can extract build specific information.
 *
 * @author roland
 * @since 01/04/16
 */
public class MavenEnricherContext implements EnricherContext {

    // overall configuration for the build
    private Configuration configuration;

    private Settings settings;

    private Map<String, String> processingInstruction;

    private MavenProject project;
    private KitLogger log;

    private MavenSession session;

    private Properties properties;

    private MavenEnricherContext() {}

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public Settings getSettings() {
        return settings;
    }

    public Map<String, String> getProcessingInstructions() {
        return processingInstruction;
    }

    public void setProcessingInstructions(Map<String, String> instruction) {
        this.processingInstruction = instruction;
    }

    @Override
    public KitLogger getLog() {
        return log;
    }


    @Override
    public GroupArtifactVersion getGav() {

        return new GroupArtifactVersion(project.getGroupId(),
                                        project.getArtifactId(),
                                        project.getVersion());
    }


    @Override
    public File getProjectDirectory() {
        return getProject().getBasedir();
    }

    @Override
    public List<Dependency> getDependencies(boolean transitive) {
        final Set<Artifact> artifacts = transitive ?
            getProject().getArtifacts() : getProject().getDependencyArtifacts();

        final List<Dependency> dependencies = new ArrayList<>();

        for (Artifact artifact : artifacts) {
            dependencies.add(
                new Dependency(new GroupArtifactVersion(artifact.getGroupId(),
                                                        artifact.getArtifactId(),
                                                        artifact.getVersion()),
                               artifact.getType(),
                               artifact.getScope(),
                               artifact.getFile()));
        }

        return dependencies;
    }

    @Override
    public boolean hasPlugin(String groupId, String artifactId) {
        if (groupId != null) {
            return MavenUtil.hasPlugin(getProject(), groupId, artifactId);
        } else {
            return MavenUtil.hasPluginOfAnyGroupId(getProject(), artifactId);
        }
    }

    @Override
    public ProjectClassLoaders getProjectClassLoaders() {
        return new ProjectClassLoaders(MavenUtil.getCompileClassLoader(getProject())
        );
    }

    @Override
    public Object getProperty(String key) {
        return properties != null ? properties.getProperty(key) : null;
    }

    // ========================================================================
    // Maven specific methods, only available after casting
    public MavenProject getProject() {
        return project;
    }

    //Method used in MOJO
    public String getDockerJsonConfigString(final Settings settings, final String serverId) {
        Server server = getServer(settings, serverId);
        if (server == null) {
            return "";
        }

        JsonObject auth = new JsonObject();
        auth.add("username", new JsonPrimitive(server.getUsername()));
        auth.add("password", new JsonPrimitive(server.getPassword()));

        String mail = getConfigurationValue(server, "email");
        if (!StringUtils.isBlank(mail)) {
            auth.add("email", new JsonPrimitive(mail));
        }

        JsonObject json = new JsonObject();
        json.add(serverId, auth);
        return json.toString();
    }

    public Server getServer(final Settings settings, final String serverId) {
        if (settings == null || StringUtils.isBlank(serverId)) {
            return null;
        }
        return settings.getServer(serverId);
    }

    private String getConfigurationValue(final Server server, final String key) {

        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        final Xpp3Dom node = configuration.getChild(key);
        if (node == null) {
            return null;
        }

        return node.getValue();
    }
    // =======================================================================================================
    public static class Builder {

        private MavenEnricherContext ctx = new MavenEnricherContext();

        private ResourceConfig resources;
        private List<ImageConfiguration> images;
        private ProcessorConfig processorConfig;

        public Builder session(MavenSession session) {
            ctx.session = session;
            return this;
        }

        public Builder log(KitLogger log) {
            ctx.log = log;
            return this;
        }

        public Builder project(MavenProject project) {
            ctx.project = project;
            return this;
        }

        public Builder config(ProcessorConfig config) {
            this.processorConfig = config;
            return this;
        }

        public Builder resources(ResourceConfig resources) {
            this.resources = resources;
            return this;
        }

        public Builder images(List<ImageConfiguration> images) {
            this.images = images;
            return this;
        }

        public Builder settings(Settings settings) {
            ctx.settings = settings;
            return this;
        }

        public Builder properties(Properties properties) {
            ctx.properties = properties;
            return this;
        }

        public Builder processingInstructions(Map<String, String> pi) {
            ctx.processingInstruction = pi;
            return this;
        }

        public MavenEnricherContext build() {
            ctx.configuration =
                new Configuration.Builder()
                    .properties(ctx.project.getProperties())
                    .images(images)
                    .resource(resources)
                    .processorConfig(processorConfig)
                    .pluginConfigLookup(
                        (system, id) -> {
                            if (!"maven".equals(system)) {
                                return Optional.empty();
                            }
                            final Plugin plugin = ctx.project.getPlugin(id);
                            if (plugin == null) {
                                return Optional.empty();
                            }
                            return Optional.of(MavenConfigurationExtractor.extract((Xpp3Dom) plugin.getConfiguration()));
                        })
                    .secretConfigLookup(
                        id -> {
                            Settings settings = ctx.session.getSettings();
                            if (settings == null || StringUtils.isBlank(id)) {
                                return Optional.empty();
                            }
                            Server server = settings.getServer(id);
                            if (server == null) {
                                return Optional.empty();
                            }
                            Map<String, Object> config = MavenConfigurationExtractor.extract((Xpp3Dom) server.getConfiguration());
                            config.put("id", server.getId());
                            config.put("username", server.getUsername());
                            config.put("password", server.getPassword());
                            return Optional.of(config);
                        })
                    .build();
            return ctx;
        }

    }
}
