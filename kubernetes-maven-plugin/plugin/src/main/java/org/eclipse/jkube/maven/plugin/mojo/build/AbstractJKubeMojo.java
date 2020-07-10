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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
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
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;

import java.util.Collections;
import java.util.Optional;

public abstract class AbstractJKubeMojo extends AbstractMojo implements KitLoggerProvider {

    protected static final String DEFAULT_LOG_PREFIX = "k8s: ";

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    // Whether to use color
    @Parameter(property = "jkube.useColor", defaultValue = "true")
    protected boolean useColor;

    // To skip over the execution of the goal
    @Parameter(property = "jkube.skip", defaultValue = "false")
    protected boolean skip;

    // For verbose output
    @Parameter(property = "jkube.verbose", defaultValue = "false")
    protected String verbose;

    // Settings holding authentication info
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter
    protected ClusterConfiguration access;

    protected KitLogger log;
    protected ClusterAccess clusterAccess;

    // The JKube service hub
    protected JKubeServiceHub jkubeServiceHub;

    protected void init() throws DependencyResolutionRequiredException {
        log = createLogger(null);
        clusterAccess = new ClusterAccess(log, initClusterConfiguration());
        final JavaProject javaProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        jkubeServiceHub = JKubeServiceHub.builder()
                .log(log)
                .configuration(JKubeConfiguration.builder()
                        .project(MavenUtil.convertMavenProjectToJKubeProject(project, session))
                        .reactorProjects(Collections.singletonList(javaProject))
                        .build())
                .clusterAccess(clusterAccess)
                .platformMode(RuntimeMode.KUBERNETES)
                .build();
    }

    protected boolean canExecute() {
        return !skip;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            init();
            if (canExecute()) {
                executeInternal();
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoFailureException(e.getMessage());
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

    protected String getLogPrefix() {
        return DEFAULT_LOG_PREFIX;
    }

    protected KitLogger createLogger(String prefix) {
        return new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(),
            getLogPrefix() + Optional.ofNullable(prefix).map(" "::concat).orElse(""));
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

}

