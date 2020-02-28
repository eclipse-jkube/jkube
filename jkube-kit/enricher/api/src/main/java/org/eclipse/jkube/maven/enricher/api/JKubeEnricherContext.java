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
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectDependency;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.apache.commons.lang3.StringUtils;

/**
 * The context given to each enricher from where it can extract build specific information.
 *
 * @author roland
 * @since 01/04/16
 */
public class JKubeEnricherContext implements EnricherContext {

    // overall configuration for the build
    private Configuration configuration;

    private List<RegistryServerConfiguration> settings;

    private Map<String, String> processingInstruction;

    private JKubeProject project;
    private KitLogger log;

    private Properties properties;

    private JKubeEnricherContext() {}

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public List<RegistryServerConfiguration> getSettings() {
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
        return getProject().getBaseDirectory();
    }

    @Override
    public List<JKubeProjectDependency> getDependencies(boolean transitive) {
        return transitive ? getProject().getDependenciesWithTransitive() : getProject().getDependencies();
    }

    @Override
    public boolean hasPlugin(String groupId, String artifactId) {
        if (groupId != null) {
            return JKubeProjectUtil.hasPlugin(getProject(), groupId, artifactId);
        } else {
            return JKubeProjectUtil.getPlugin(getProject(), artifactId) != null;
        }
    }

    @Override
    public ProjectClassLoaders getProjectClassLoaders() {
        return new ProjectClassLoaders(ClassUtil.createClassLoader(getProject().getCompileClassPathElements(), getProject().getOutputDirectory())
        );
    }

    @Override
    public Object getProperty(String key) {
        return properties != null ? properties.getProperty(key) : null;
    }

    // ========================================================================
    // Maven specific methods, only available after casting
    public JKubeProject getProject() {
        return project;
    }

    //Method used in MOJO
    public String getDockerJsonConfigString(final List<RegistryServerConfiguration> settings, final String serverId) {
        RegistryServerConfiguration server = RegistryServerConfiguration.getServer(settings, serverId);
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



    private String getConfigurationValue(final RegistryServerConfiguration server, final String key) {

        final Map<String, Object> configuration = server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        if (configuration.containsKey(key)) {
            return configuration.get(key).toString();
        }
        return null;
    }
    // =======================================================================================================
    public static class Builder {

        private JKubeEnricherContext ctx = new JKubeEnricherContext();

        private ResourceConfig resources;
        private List<ImageConfiguration> images;
        private ProcessorConfig processorConfig;

        public Builder log(KitLogger log) {
            ctx.log = log;
            return this;
        }

        public Builder project(JKubeProject project) {
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

        public Builder settings(List<RegistryServerConfiguration> settings) {
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

        public JKubeEnricherContext build() {
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
                            final JKubeProjectPlugin plugin = JKubeProjectUtil.getPlugin(ctx.project, id);
                            if (plugin == null) {
                                return Optional.empty();
                            }
                            return Optional.of(plugin.getConfiguration());
                        })
                    .secretConfigLookup(
                        id -> {
                            List<RegistryServerConfiguration> settings = ctx.getSettings();
                            if (settings == null || StringUtils.isBlank(id)) {
                                return Optional.empty();
                            }
                            RegistryServerConfiguration server = RegistryServerConfiguration.getServer(settings, id);
                            if (server == null) {
                                return Optional.empty();
                            }
                            Map<String, Object> config = server.getConfiguration();
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
