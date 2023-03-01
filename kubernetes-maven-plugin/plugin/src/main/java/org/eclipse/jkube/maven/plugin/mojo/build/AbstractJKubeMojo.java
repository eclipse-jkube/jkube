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

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.Component;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.MavenUtil;
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
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.util.Collections;
import java.util.Optional;

import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.updateResourceConfigNamespace;
import static org.eclipse.jkube.kit.config.service.kubernetes.SummaryServiceUtil.handleExceptionAndSummary;
import static org.eclipse.jkube.kit.config.service.kubernetes.SummaryServiceUtil.printSummary;
import static org.eclipse.jkube.kit.config.service.kubernetes.SummaryServiceUtil.printSummaryIfLastExecuting;

public abstract class AbstractJKubeMojo extends AbstractMojo implements KitLoggerProvider {

    protected static final String DEFAULT_LOG_PREFIX = "k8s: ";

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

    @Parameter(property = "jkube.namespace")
    public String namespace;

    @Parameter(property = "jkube.summaryEnabled", defaultValue = "true")
    public boolean summaryEnabled;

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
            handleExceptionAndSummary(jkubeServiceHub, e);
            throw new MojoFailureException(e.getMessage());
        } catch (KubernetesClientException kubernetesClientException) {
            IllegalStateException illegalStateException = KubernetesResourceUtil.handleKubernetesClientException(kubernetesClientException, this.log, jkubeServiceHub.getSummaryService());
            printSummary(jkubeServiceHub);
            throw illegalStateException;
        } finally {
            printSummaryIfLastExecuting(jkubeServiceHub, mojoExecution.getGoal(), MavenUtil.getLastExecutingGoal(session, getLogPrefix().trim()));
        }
    }

    protected void init() throws DependencyResolutionRequiredException {
        log = createLogger(null);
        clusterAccess = new ClusterAccess(log, initClusterConfiguration());
        javaProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        jkubeServiceHub = initJKubeServiceHubBuilder(javaProject).build();
        resources = updateResourceConfigNamespace(namespace, resources);
        jkubeServiceHub.getSummaryService().setSuccessful(true);
        jkubeServiceHub.getSummaryService().setActionType("Goals");
        jkubeServiceHub.getSummaryService().addToActions(String.format("%s\b%s", getLogPrefix(), mojoExecution.getGoal()));
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
            .summaryEnabled(summaryEnabled)
            .platformMode(getRuntimeMode());
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
}

