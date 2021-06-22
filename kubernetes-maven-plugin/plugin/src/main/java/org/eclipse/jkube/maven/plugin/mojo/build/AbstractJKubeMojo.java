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
package org.eclipse.jkube.maven.plugin.mojo.build;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.EnricherManager;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;
import org.fusesource.jansi.Ansi;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public abstract class AbstractJKubeMojo extends AbstractMojo implements KitLoggerProvider {

    protected static final String DEFAULT_LOG_PREFIX = "k8s: ";

    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    // Resource specific configuration for this plugin
    @Parameter
    protected ResourceConfig resources;

    @Parameter(defaultValue = "${session}", readonly = true)
    public MavenSession session;

    // Whether to use color
    @Parameter(property = "jkube.useColor", defaultValue = "true")
    public boolean useColor;

    // To skip over the execution of the goal
    @Parameter(property = "jkube.skip", defaultValue = "false")
    public boolean skip;

    // For verbose output
    @Parameter(property = "jkube.verbose", defaultValue = "false")
    public String verbose;

    @Parameter(property = "jkube.offline", defaultValue = "false")
    protected boolean offline;

    // Settings holding authentication info
    @Parameter(defaultValue = "${settings}", readonly = true)
    public Settings settings;

    @Parameter
    public ClusterConfiguration access;

    public KitLogger log;
    public ClusterAccess clusterAccess;

    // The JKube service hub
    public JKubeServiceHub jkubeServiceHub;

    // Mode which is resolved, also when 'auto' is set
    public RuntimeMode runtimeMode;

    protected void init() throws DependencyResolutionRequiredException {
        log = createLogger(null);
        clusterAccess = new ClusterAccess(log, initClusterConfiguration());
        final JavaProject javaProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        runtimeMode = getRuntimeMode();
        jkubeServiceHub = initJKubeServiceHubBuilder(javaProject).build();
    }

    protected boolean canExecute() {
        return !skip;
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        final boolean ansiRestore = Ansi.isEnabled();
        try {
            init();
            if (canExecute()) {
                executeInternal();
            }
        } catch (DependencyResolutionRequiredException e) {
            logException(e);
            throw new MojoFailureException(e.getMessage());
        } catch (MojoExecutionException | MojoFailureException exp) {
            logException(exp);
            throw exp;
        } finally {
            Optional.ofNullable(jkubeServiceHub).ifPresent(JKubeServiceHub::close);
            Ansi.setEnabled(ansiRestore);
        }
    }

    public abstract void executeInternal() throws MojoExecutionException, MojoFailureException;

    @Override
    public KitLogger getKitLogger() {
        return log;
    }

    protected String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = project.getProperties().getProperty(key);
        }
        return value;
    }

    public RuntimeMode getRuntimeMode() {
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
     *
     * @return true if log statements should be colorized
     */
    protected boolean useColorForLogging() {
        return useColor && MessageUtils.isColorEnabled()
                && !(EnvUtil.isWindows() && !MavenUtil.isMaven350OrLater(session));
    }

    protected ClusterConfiguration initClusterConfiguration() {
        return ClusterConfiguration.from(access, System.getProperties(), project.getProperties()).build();
    }

    protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder(JavaProject javaProject) {
        return JKubeServiceHub.builder()
                .log(log)
                .configuration(initJKubeConfiguration(javaProject))
                .platformMode(getRuntimeMode())
                .clusterAccess(clusterAccess)
                .offline(offline)
                .platformMode(getRuntimeMode());
    }

    public ResourceConfig getResources() {
        return resources;
    }

    public JKubeConfiguration initJKubeConfiguration(JavaProject javaProject) {
        return JKubeConfiguration.getJKubeConfiguration(javaProject, null, null, null, null);
    }

    public void logException(Exception exp) {
        if (exp.getCause() != null) {
            log.error("%s [%s]", exp.getMessage(), exp.getCause().getMessage());
        } else {
            log.error("%s", exp.getMessage());
        }
    }

    // Get generator config
    public ProcessorConfig extractGeneratorConfig(String profile, File resourceDir, ProcessorConfig generator) {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, profile, resourceDir, generator);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
        }
    }

    // Get enricher config
    public ProcessorConfig extractEnricherConfig(String profile, File resourceDir, ProcessorConfig enricher) {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG, profile, resourceDir, enricher);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract enricher config: " + e, e);
        }
    }

    public void enricherTask(KubernetesListBuilder kubernetesListBuilder, EnricherManager enricherManager) {
        enricherManager.enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    }

    public void attachArtifacts(MavenProject project, MavenProjectHelper projectHelper, ResourceFileType resourceFileType, String classifier, File destFile) {
        if (destFile.exists()) {
            projectHelper.attachArtifact(project, resourceFileType.getArtifactType(), classifier, destFile);
        }
    }
}

