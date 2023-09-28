/*
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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.resource.service.DefaultResourceService;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.eclipse.jkube.kit.common.ResourceFileType.yaml;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.updateResourceConfigNamespace;

public abstract class AbstractJKubeMojo extends AbstractMojo implements KitLoggerProvider {

    protected static final String DEFAULT_LOG_PREFIX = "k8s: ";

    @Component(role = MavenFileFilter.class, hint = "default")
    private MavenFileFilter mavenFileFilter;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    // Resource specific configuration for this plugin
    @Parameter
    protected ResourceConfig resources;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution mojoExecution;

    @Parameter(property = "jkube.build.strategy")
    protected JKubeBuildStrategy buildStrategy;

    // Whether to use color
    @Parameter(property = "jkube.useColor", defaultValue = "true")
    protected boolean useColor;

    // To skip over the execution of the goal
    @Parameter(property = "jkube.skip", defaultValue = "false")
    protected boolean skip;

    // For verbose output
    @Parameter(property = "jkube.verbose", defaultValue = "false")
    protected String verbose;

    @Parameter(property = "jkube.offline", defaultValue = "false")
    protected boolean offline;

    // Settings holding authentication info
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    /**
     * The JKube working directory
     */
    @Parameter(property = "jkube.workDir", defaultValue = "${project.build.directory}/jkube-temp")
    protected File workDir;

    @Parameter(property = "jkube.namespace")
    public String namespace;

    /**
     * Folder where to find project specific files
     */
    @Parameter(property = "jkube.resourceDir", defaultValue = "${basedir}/src/main/jkube")
    protected File resourceDir;

    /**
     * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, plugin will look at src/main/jkube/dev
     * Same applies for resourceDirOpenShiftOverride property.
     */
    @Parameter(property = "jkube.environment")
    protected String environment;

    /**
     * The artifact type for attaching the generated resource file to the project.
     * Can be either 'json' or 'yaml'
     */
    @Parameter(property = "jkube.resourceType")
    protected ResourceFileType resourceFileType = yaml;

    /**
     * The generated kubernetes and openshift manifests
     */
    @Parameter(property = "jkube.targetDir", defaultValue = "${project.build.outputDirectory}/META-INF/jkube")
    protected File targetDir;

    @Parameter(property="jkube.interpolateTemplateParameters", defaultValue = "true")
    protected Boolean interpolateTemplateParameters;

    @Parameter
    protected ClusterConfiguration access;

    @Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
    protected SecDispatcher securityDispatcher;

    protected KitLogger log;

    protected ClusterAccess clusterAccess;

    // The JKube service hub
    protected JKubeServiceHub jkubeServiceHub;

    protected JavaProject javaProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            init();
            if (shouldSkip()) {
                log.info("`%s` goal is skipped.", mojoExecution.getMojoDescriptor().getFullGoalName());
                return;
            }
            executeInternal();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void init() throws DependencyResolutionRequiredException {
        log = createLogger(null);
        clusterAccess = new ClusterAccess(initClusterConfiguration());
        javaProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        jkubeServiceHub = initJKubeServiceHubBuilder(javaProject).build();
        resources = updateResourceConfigNamespace(namespace, resources);
    }

    protected boolean shouldSkip() {
        return skip;
    }

    public abstract void executeInternal() throws MojoExecutionException, MojoFailureException;

    @Override
    public KitLogger getKitLogger() {
        return log;
    }

    protected RuntimeMode getRuntimeMode() {
        return RuntimeMode.KUBERNETES;
    }

    protected String getLogPrefix() {
        return DEFAULT_LOG_PREFIX;
    }

    protected KitLogger createLogger(String prefix) {
        return new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(),
            getLogPrefix() + Optional.ofNullable(prefix).map(" "::concat).orElse(""));
    }

    protected Settings getSettings() {
        return settings;
    }

    /**
     * Determine whether to enable colorized log messages
     * @return true if log statements should be colorized
     */
    private boolean useColorForLogging() {
        return useColor && MessageUtils.isColorEnabled()
                && !(EnvUtil.isWindows() && !MavenUtil.isMaven350OrLater(session));
    }

    protected ClusterConfiguration initClusterConfiguration() {
        return ClusterConfiguration.from(access, System.getProperties(), project.getProperties()).build();
    }

    private ResourceServiceConfig initResourceServiceConfig() {
        return ResourceServiceConfig.builder()
          .project(javaProject)
          .resourceDirs(ResourceUtil.getFinalResourceDirs(resourceDir, environment))
          .targetDir(targetDir)
          .resourceFileType(resourceFileType)
          .resourceConfig(resources)
          .resourceFilesProcessor(resourceFiles -> mavenFilterFiles(resourceFiles, workDir))
          .interpolateTemplateParameters(interpolateTemplateParameters)
          .build();
    }

    protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder(JavaProject javaProject) {
        return JKubeServiceHub.builder()
            .log(log)
            .configuration(JKubeConfiguration.builder()
                .project(javaProject)
                .reactorProjects(Collections.singletonList(javaProject))
                .registryConfig(RegistryConfig.builder()
                    .settings(MavenUtil.getRegistryServerFromMavenSettings(settings))
                    .passwordDecryptionMethod(this::decrypt)
                    .build())
                .build())
            .clusterAccess(clusterAccess)
            .offline(offline)
            .platformMode(getRuntimeMode())
            .resourceServiceConfig(initResourceServiceConfig())
            .resourceService(new LazyBuilder<>(hub -> new DefaultResourceService(hub.getResourceServiceConfig())));
    }

    public ResourceConfig getResources() {
        return resources;
    }

    String decrypt(String password) {
        try {
            return securityDispatcher.decrypt(password);
        } catch (SecDispatcherException e) {
            getKitLogger().error("Failure in decrypting password");
        }
        return password;
    }

    private File[] mavenFilterFiles(File[] resourceFiles, File outDir) throws IOException {
        if (resourceFiles == null) {
            return new File[0];
        }
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create working dir " + outDir);
        }
        File[] ret = new File[resourceFiles.length];
        int i = 0;
        for (File resource : resourceFiles) {
            File targetFile = new File(outDir, resource.getName());
            try {
                mavenFileFilter.copyFile(resource, targetFile, true,
                  project, null, false, "utf8", session);
                ret[i++] = targetFile;
            } catch (MavenFilteringException exp) {
                throw new IOException(
                  String.format("Cannot filter %s to %s", resource, targetFile), exp);
            }
        }
        return ret;
    }
}

