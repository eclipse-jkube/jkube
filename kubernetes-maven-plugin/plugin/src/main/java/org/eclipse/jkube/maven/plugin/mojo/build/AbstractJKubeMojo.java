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

import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.utils.logging.MessageUtils;

public abstract class AbstractJKubeMojo extends AbstractMojo {

    private static final String DEFAULT_LOG_PREFIX = "k8s:";

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( skip ) {
            return;
        }
        log = createLogger(" ");
        executeInternal();
    }

    public abstract void executeInternal() throws MojoExecutionException, MojoFailureException;

    protected String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = project.getProperties().getProperty(key);
        }
        return value;
    }

    protected KitLogger createExternalProcessLogger(String prefix) {
        return createLogger(prefix + "[[s]]");
    }

    protected String getLogPrefix() {
        return DEFAULT_LOG_PREFIX;
    }

    protected KitLogger createLogger(String prefix) {
        return new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(), getLogPrefix() + prefix);
    }

    /**
     * Determine whether to enable colorized log messages
     * @return true if log statements should be colorized
     */
    private boolean useColorForLogging() {
        return useColor && MessageUtils.isColorEnabled()
                && !(EnvUtil.isWindows() && !MavenUtil.isMaven350OrLater(session));
    }

    protected ClusterConfiguration getClusterConfiguration() {
        final ClusterConfiguration.Builder clusterConfigurationBuilder = new ClusterConfiguration.Builder(access);

        return clusterConfigurationBuilder.from(System.getProperties())
                .from(project.getProperties()).build();
    }

}

