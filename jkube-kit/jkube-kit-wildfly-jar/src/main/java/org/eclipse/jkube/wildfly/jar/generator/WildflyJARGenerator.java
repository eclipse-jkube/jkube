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

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.BOOTABLE_JAR_ARTIFACT_ID;
import static org.eclipse.jkube.wildfly.jar.WildflyJarUtils.BOOTABLE_JAR_GROUP_ID;

@SuppressWarnings("unchecked")
public class WildflyJARGenerator extends JavaExecGenerator {
    static final String JBOSS_MAVEN_DIST = "jboss-maven-dist";
    static final String JBOSS_MAVEN_REPO = "jboss-maven-repo";
    static final String PLUGIN_OPTIONS = "plugin-options";

    final Path localRepoCache;
    public WildflyJARGenerator(GeneratorContext context) {
        super(context, "wildfly-jar");
        JavaProject project = context.getProject();
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

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs)
                && JKubeProjectUtil.hasPlugin(getProject(), BOOTABLE_JAR_GROUP_ID, BOOTABLE_JAR_ARTIFACT_ID);
    }

    @Override
    protected Map<String, String> getEnv(boolean isPrepackagePhase) {
        Map<String, String> ret = super.getEnv(isPrepackagePhase);
        // Switch off Prometheus agent until logging issue with WildFly Swarm is resolved
        // See:
        // - https://github.com/fabric8io/fabric8-maven-plugin/issues/1173
        // - https://issues.jboss.org/browse/THORN-1859
        ret.put("AB_PROMETHEUS_OFF", "true");
        ret.put("AB_OFF", "true");

        // In addition, there is no proper fix in Jolokia to detect that the Bootable JAR is started.
        // Can be workarounded by setting JAVA_OPTIONS to contain -Djboss.modules.system.pkgs=org.jboss.byteman
        ret.put("AB_JOLOKIA_OFF", "true");
        return ret;
    }

    @Override
    protected List<AssemblyFileSet> addAdditionalFiles() {
        List<AssemblyFileSet> set = super.addAdditionalFiles();
        if (localRepoCache != null) {
            Path parentDir;
            Path repoDir = localRepoCache;
            if (localRepoCache.isAbsolute()) {
                parentDir = localRepoCache.getParent();
            } else {
                repoDir = getProject().getBaseDirectory().toPath().resolve(localRepoCache);
                parentDir = repoDir.getParent();
            }
            if (Files.notExists(repoDir)) {
               throw new RuntimeException("Error, WildFly bootable JAR generator can't retrieve "
                       + "generated maven local cache, directory " + repoDir + " doesn't exist.");
            }
            set.add(AssemblyFileSet.builder()
                    .directory(parentDir.toFile())
                    .include(localRepoCache.getFileName().toString())
                    .outputDirectory(new File("."))
                    .fileMode("0640")
                    .build());
        }
        return set;
    }

    @Override
    protected List<String> getExtraJavaOptions() {
        List<String> properties = new ArrayList<>();
        properties.add("-Djava.net.preferIPv4Stack=true");
        if (localRepoCache != null) {
            properties.add("-Dmaven.repo.local=/deployments/" + localRepoCache.getFileName().toString());
        }
        return properties;
    }
}
