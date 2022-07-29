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
package org.eclipse.jkube.kit.enricher.api;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.enricher.handler.HandlerHub;

/**
 * The context given to each enricher from where it can extract build specific information.
 *
 * @author roland
 */
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class JKubeEnricherContext implements EnricherContext {

    /**
     * overall configuration for the build.
     */
    private Configuration configuration;
    private List<RegistryServerConfiguration> settings;
    private Map<String, String> processingInstructions;
    private JavaProject project;
    private KitLogger log;
    @Getter(AccessLevel.NONE)
    private ResourceConfig resources;
    @Getter(AccessLevel.NONE)
    private List<ImageConfiguration> images;
    @Getter(AccessLevel.NONE)
    private ProcessorConfig processorConfig;
    private HandlerHub handlerHub;
    @Getter(AccessLevel.NONE)
    private JKubeBuildStrategy jKubeBuildStrategy;


    @Builder(toBuilder = true)
    public JKubeEnricherContext(
        @Singular  List<RegistryServerConfiguration> settings, @Singular Map<String, String> processingInstructions,
        JavaProject project, KitLogger log,
        ResourceConfig resources, @Singular List<ImageConfiguration> images, ProcessorConfig processorConfig, JKubeBuildStrategy jKubeBuildStrategy) {
        this.settings = settings;
        this.processingInstructions = processingInstructions;
        this.project = project;
        this.log = log;
        this.resources = resources;
        this.images = images;
        this.processorConfig = processorConfig;
        this.handlerHub = new HandlerHub(getGav(), getProperties());
        this.configuration = Configuration.builder()
            .images(images)
            .resource(resources)
            .processorConfig(processorConfig)
            .jKubeBuildStrategy(jKubeBuildStrategy)
            .pluginConfigLookup(
                (system, id) -> {
                    if (!"maven".equals(system)) {
                        return Optional.empty();
                    }
                    final Plugin plugin = JKubeProjectUtil.getPlugin(this.project, id);
                    if (plugin == null) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(plugin.getConfiguration());
                })
            .secretConfigLookup(
                id -> {
                    if (this.settings == null || StringUtils.isBlank(id)) {
                        return Optional.empty();
                    }
                    RegistryServerConfiguration server = RegistryServerConfiguration.getServer(this.settings, id);
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
    }

    @Override
    public Map<String, String> getProcessingInstructions() {
        return processingInstructions;
    }

    @Override
    public void setProcessingInstructions(Map<String, String> instruction) {
        this.processingInstructions = instruction;
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
    public List<Dependency> getDependencies(boolean transitive) {
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
        return new ProjectClassLoaders(ClassUtil.createClassLoader(
            getProject().getCompileClassPathElements(), getProject().getOutputDirectory().getAbsolutePath())
        );
    }

    @Override
    public Properties getProperties() {
        return project.getProperties();
    }

    @Override
    public String getProperty(String key) {
        return project.getProperties() != null ? project.getProperties().getProperty(key) : null;
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
        return Optional.ofNullable(server.getConfiguration())
            .filter(c -> c.containsKey(key))
            .map(c -> c.get(key).toString())
            .orElse(null);
    }
}
