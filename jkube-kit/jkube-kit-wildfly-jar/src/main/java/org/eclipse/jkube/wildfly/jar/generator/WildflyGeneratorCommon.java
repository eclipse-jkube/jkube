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
package org.eclipse.jkube.wildfly.jar.generator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import static org.eclipse.jkube.wildfly.jar.enricher.WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID;
import static org.eclipse.jkube.wildfly.jar.enricher.WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID;

/**
 *
 * @author jdenise
 */
public class WildflyGeneratorCommon {

    public static final String JBOSS_MAVEN_DIST = "jboss-maven-dist";
    public static final String JBOSS_MAVEN_REPO = "jboss-maven-repo";
    public static final String PLUGIN_OPTIONS = "plugin-options";
    public static final String MAVEN_REPO_DIR = "server-maven-repo";

    public static final String SERVER_OPTION = "server";
    public static final String ENABLED = "enabled";

    private final Path localRepoCache;
    private final JavaProject project;
    private final String targetDir;

    public WildflyGeneratorCommon(String targetDir, GeneratorContext context) {
        this.targetDir = targetDir;
        this.project = context.getProject();
        Plugin plugin = JKubeProjectUtil.getPlugin(project, BOOTABLE_JAR_GROUP_ID, BOOTABLE_JAR_ARTIFACT_ID);
        localRepoCache = Optional.ofNullable(plugin).
                map(Plugin::getConfiguration).
                map(c -> (Map<String, Object>) c.get(PLUGIN_OPTIONS)).
                map(options -> options.containsKey(JBOSS_MAVEN_DIST) && options.containsKey(JBOSS_MAVEN_REPO) ? options : null).
                map(options -> {
                    String dist = (String) options.get(JBOSS_MAVEN_DIST);
                    return dist == null || "true".equals(dist) ? (String) options.get(JBOSS_MAVEN_REPO) : null;
                }).map(Paths::get).orElse(null);
    }

    public List<AssemblyFileSet> getAdditionalFiles() {
        List<AssemblyFileSet> set = new ArrayList<>();
        if (localRepoCache != null) {
            Path repoDir = localRepoCache;
            if (!localRepoCache.isAbsolute()) {
                repoDir = project.getBaseDirectory().toPath().resolve(localRepoCache);
            }
            if (Files.notExists(repoDir)) {
                throw new RuntimeException("Error, WildFly bootable JAR generator can't retrieve "
                        + "generated maven local cache, directory " + repoDir + " doesn't exist.");
            }
            set.add(AssemblyFileSet.builder()
                    .directory(repoDir.toFile())
                    .include("**")
                    .outputDirectory(new File(MAVEN_REPO_DIR))
                    .fileMode("0640")
                    .build());
        }
        return set;
    }

    public List<String> getOptions() {
        List<String> properties = new ArrayList<>();
        if (localRepoCache != null) {
            properties.add("-Dmaven.repo.local=" + targetDir + "/" + MAVEN_REPO_DIR);
        }
        return properties;
    }
    
    public boolean isServerEnabled() {
        Plugin plugin = JKubeProjectUtil.getPlugin(project, BOOTABLE_JAR_GROUP_ID, BOOTABLE_JAR_ARTIFACT_ID);
        return Optional.ofNullable(plugin).
                map(Plugin::getConfiguration).
                map(c -> normalizeValue(c)).
                map(server -> {
                    String enabled = (String) server.get(ENABLED);
                    return enabled == null || "true".equals(enabled);
                }).orElse(Boolean.FALSE);
    }
    
    private Map<String, Object> normalizeValue(Map<String, Object> config) {
        if (config.containsKey(SERVER_OPTION)) {
            Map<String, Object> val = (Map<String, Object>) config.get(SERVER_OPTION);
            if (val == null) {
                val = new HashMap<>();
                val.put(ENABLED, "true");
            }
            return val;
        } else {
            return null;
        }
    }
}
