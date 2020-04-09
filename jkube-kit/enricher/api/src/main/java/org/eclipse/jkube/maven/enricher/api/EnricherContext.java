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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;

public interface EnricherContext {

    /**
     * Get the current artifact with its parameters
     *
     * @return the artifact
     */
    GroupArtifactVersion getGav();

    /**
     * Get Logger.
     * @return Logger.
     */
    KitLogger getLog();

    /**
     * The configuration specific to the enricher.
     *
     * @return configuration to use
     */
    Configuration getConfiguration();


    Map<String, String> getProcessingInstructions();

    void setProcessingInstructions(Map<String, String> instructions);

    /**
     * Base directory of the project. E.g. for Maven that's the directory
     * where the pom.xml is placed in
     * @return the projects based directory
     */
    File getProjectDirectory();

    /**
     * Get various class loaders used in the projects
     *
     * @return compile and test class loader
     */
    ProjectClassLoaders getProjectClassLoaders();

    /**
     * Check if a given plugin is present
     *
     * @param groupId group id of plugin to check. If null any group will be considered.
     * @param artifactId of plugin to check
     * @return true if a plugin exists, false otherwise.
     */
    boolean hasPlugin(String groupId, String artifactId);

    /**
     * Gets dependencies defined in build tool
     * @param transitive if transitive deps should be returned.
     * @return List of dependencies.
     */
    List<Dependency> getDependencies(boolean transitive);

    /**
     * Checks if given dependency is defined.
     * @param groupId of dependency.
     * @param artifactId of dependency. If null, check if there is any dependency with the given group
     * @return True if present, false otherwise.
     */
    default boolean hasDependency(String groupId, String artifactId) {
        return getDependencyVersion(groupId, artifactId).isPresent();
    }

    /**
     * Gets version of given dependency.
     * @param groupId of the dependency.
     * @param artifactId of the dependency.
     * @return Version number.
     */
    default Optional<String> getDependencyVersion(String groupId, String artifactId) {
        for (Dependency dep : getDependencies(true)) {
            String scope = dep.getScope();
            if ("test".equals(scope) ||
                (artifactId != null && !artifactId.equals(dep.getArtifactId()))) {
                continue;
            }
            if (dep.getGroupId().equals(groupId)) {
                return Optional.of(dep.getVersion());
            }
        }
        return Optional.empty();
    }

    /**
     * Gets a system property used in project.
     *
     * @param key name of property
     * @return value of property if set.
     */
    Object getProperty(String key);
}
