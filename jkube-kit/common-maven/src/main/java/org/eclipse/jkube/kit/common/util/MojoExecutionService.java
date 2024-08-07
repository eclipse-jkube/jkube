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
package org.eclipse.jkube.kit.common.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystemSession;

/**
 * A service for executing goals on configured plugins.
 *
 * Inspired by and partly reused from
 * https://github.com/TimMoore/mojo-executor but adapted to newer Maven versions.
 *
 * @author roland
 * @since 01/07/15
 */
public class MojoExecutionService {

    private final MavenProject project;

    private final MavenSession session;

    private final BuildPluginManager pluginManager;

    public MojoExecutionService(MavenProject project, MavenSession session, BuildPluginManager pluginManager) {
        this.project = project;
        this.session = session;
        this.pluginManager = pluginManager;
    }

    // Call another goal after restart has finished
    public void callPluginGoal(String fullGoal) {
        String[] parts = splitGoalSpec(fullGoal);
        Plugin plugin = project.getPlugin(parts[0]);
        String goal = parts[1];

        if (plugin == null) {
            throw new IllegalStateException("No goal " + fullGoal + " found in pom.xml");
        }

        try {
            String executionId = null;
            if (goal != null && !goal.isEmpty() && goal.indexOf('#') > -1) {
                int pos = goal.indexOf('#');
                executionId = goal.substring(pos + 1);
                goal = goal.substring(0, pos);
            }

            PluginDescriptor pluginDescriptor = getPluginDescriptor(project, plugin);
            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo(goal);
            if (mojoDescriptor == null) {
                throw new MojoExecutionException("Could not find goal '" + goal + "' in plugin "
                                                 + plugin.getGroupId() + ":"
                                                 + plugin.getArtifactId() + ":"
                                                 + plugin.getVersion());
            }
            MojoExecution exec = getMojoExecution(executionId, mojoDescriptor);
            pluginManager.executeMojo(session, exec);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to execute mojo", e);
        }
    }

    private MojoExecution getMojoExecution(String executionId, MojoDescriptor mojoDescriptor) {
        if (executionId != null) {
            return new MojoExecution(mojoDescriptor, executionId);
        } else {
            return new MojoExecution(mojoDescriptor, toXpp3Dom(mojoDescriptor.getMojoConfiguration()));
        }
    }

    PluginDescriptor getPluginDescriptor(MavenProject project, Plugin plugin)
        throws InvocationTargetException, IllegalAccessException, MojoFailureException {

        try {
            Method loadPlugin = pluginManager.getClass().getMethod("loadPluginDescriptor",
                                                            plugin.getClass(),
                                                            project.getClass(),
                                                            session.getClass());
            return (PluginDescriptor) loadPlugin.invoke(pluginManager, plugin, project, session);
        } catch (NoSuchMethodException exp) {
            try {
                // Fallback for older Maven versions
                RepositorySystemSession repositorySession = session.getRepositorySession();
                Method loadPlugin = pluginManager.getClass().getMethod("loadPlugin",
                                                                       Plugin.class,
                                                                       List.class,
                                                                       RepositorySystemSession.class);
                return (PluginDescriptor) loadPlugin.invoke(pluginManager, plugin, project.getRemotePluginRepositories(), repositorySession);
            } catch (NoSuchMethodException exp2) {
                throw new MojoFailureException("Cannot load plugin descriptor for plugin " + plugin.getGroupId() + ":" + plugin.getArtifactId(),exp2);
            }
        }
    }

    private String[] splitGoalSpec(String fullGoal) {
        String[] parts = StringUtils.split(fullGoal, ":");
        if (parts.length != 3) {
            throw new IllegalStateException("Cannot parse " + fullGoal + " as a maven plugin goal. " +
                                           "It must be fully qualified as in <groupId>:<artifactId>:<goal>");
        }
        return new String[]{parts[0] + ":" + parts[1], parts[2]};
    }

    private Xpp3Dom toXpp3Dom(PlexusConfiguration config) {
        Xpp3Dom result = new Xpp3Dom(config.getName());
        result.setValue(config.getValue(null));
        for (String name : config.getAttributeNames()) {
            result.setAttribute(name, config.getAttribute(name));
        }
        for (PlexusConfiguration child : config.getChildren()) {
            result.addChild(toXpp3Dom(child));
        }
        return result;
    }

}
