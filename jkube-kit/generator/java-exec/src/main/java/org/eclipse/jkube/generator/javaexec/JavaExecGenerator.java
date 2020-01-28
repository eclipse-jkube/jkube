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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.build.core.config.JkubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JkubeBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.JkubeProjectAssembly;
import org.eclipse.jkube.kit.common.util.JkubeProjectUtil;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;


/**
 * @author roland
 * @since 21/09/16
 */

public class JavaExecGenerator extends BaseGenerator {

    // Environment variable used for specifying a main class
    static final String JAVA_MAIN_CLASS_ENV_VAR = "JAVA_MAIN_CLASS";
    private static final String JAVA_OPTIONS = "JAVA_OPTIONS";

    // Plugins indicating a plain java build
    private static final String[][] JAVA_EXEC_MAVEN_PLUGINS = new String[][] {
        new String[] { "org.codehaus.mojo", "exec-maven-plugin" },
        new String[] { "org.apache.maven.plugins", "maven-shade-plugin" }
    };

    private final FatJarDetector fatJarDetector;
    private final MainClassDetector mainClassDetector;

    public JavaExecGenerator(GeneratorContext context) {
        this(context, "java-exec");
    }

    protected JavaExecGenerator(GeneratorContext context, String name) {
        super(context, name, new FromSelector.Default(context, "java"));
        fatJarDetector = new FatJarDetector(getProject().getBuildDirectory());
        mainClassDetector = new MainClassDetector(getConfig(Config.mainClass),
                                                  new File(getProject().getOutputDirectory()),
                                                  context.getLogger());
    }

    public enum Config implements Configs.Key {
        // Webport to expose. Set to 0 if no port should be exposed
        webPort        {{ d = "8080"; }},

        // Jolokia from the base image to expose. Set to 0 if no such port should be exposed
        jolokiaPort    {{ d = "8778"; }},

        // Prometheus port from base image. Set to 0 if no required
        prometheusPort {{ d = "9779"; }},

        // Basedirectory where to put the application data into (within the Docker image
        targetDir {{d = "/deployments"; }},

        // The name of the main class for non-far jars. If not speficied it is tried
        // to find a main class within target/classes.
        mainClass,

        // Reference to a predefined assembly descriptor to use. By defult it is tried to be detected
        assemblyRef;

        public String def() { return d; } protected String d;
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        if (shouldAddImageConfiguration(configs)) {
            // If a main class is configured, we always kick in
            if (getConfig(Config.mainClass) != null) {
                return true;
            }
            // Check for the existing of plugins indicating a plain java exec app
            for (String[] plugin : JAVA_EXEC_MAVEN_PLUGINS) {
                if (JkubeProjectUtil.hasPlugin(getProject(), plugin[0], plugin[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
        final ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();
        final JkubeBuildConfiguration.Builder buildBuilder = new JkubeBuildConfiguration.Builder();

        buildBuilder.ports(extractPorts());

        addSchemaLabels(buildBuilder, log);
        addFrom(buildBuilder);
        if (!prePackagePhase) {
            // Only add assembly if not in a pre-package phase where the referenced files
            // won't be available.
            buildBuilder.assembly(createAssembly());
        }
        Map<String, String> envMap = getEnv(prePackagePhase);
        envMap.put("JAVA_APP_DIR", getConfig(Config.targetDir));
        buildBuilder.env(envMap);
        addLatestTagIfSnapshot(buildBuilder);
        imageBuilder
            .name(getImageName())
            .registry(getRegistry())
            .alias(getAlias())
            .buildConfig(buildBuilder.build());
        configs.add(imageBuilder.build());
        return configs;
    }

    /**
     * Hook for adding extra environment vars
     *
     * @return map with environment variables to use
     * @param prePackagePhase
     */
    protected Map<String, String> getEnv(boolean prePackagePhase) {
        Map<String, String> ret = new HashMap<>();
        if (!isFatJar()) {
            String mainClass = getConfig(Config.mainClass);
            if (mainClass == null) {
                mainClass = mainClassDetector.getMainClass();
                if (mainClass == null) {
                    if (!prePackagePhase) {
                        throw new IllegalStateException("Cannot extract main class to startup");
                    }
                }
            }
            if (mainClass != null) {
                log.verbose("Detected main class %s", mainClass);
                ret.put(JAVA_MAIN_CLASS_ENV_VAR, mainClass);
            }
        }
        List<String> javaOptions = getExtraJavaOptions();
        if (javaOptions.size() > 0) {
            ret.put(JAVA_OPTIONS, StringUtils.join(javaOptions.iterator(), " "));
        }
        return ret;
    }

    protected List<String> getExtraJavaOptions() {
        return new ArrayList<>();
    }

    protected JkubeAssemblyConfiguration createAssembly() {
        final JkubeAssemblyConfiguration.Builder builder = new JkubeAssemblyConfiguration.Builder();
        builder.targetDir(getConfig(Config.targetDir));
        addAssembly(builder);
        return builder.build();
    }

    protected void addAssembly(JkubeAssemblyConfiguration.Builder builder) {
        String assemblyRef = getConfig(Config.assemblyRef);
        if (assemblyRef != null) {
            builder.descriptorRef(assemblyRef);
        } else {
            List<JkubeProjectAssembly> assemblies = new ArrayList<>();
            assemblies.addAll(addAdditionalFiles(getProject()));
            if (isFatJar()) {
                FatJarDetector.Result fatJar = detectFatJar();
                JkubeProject project = getProject();
                if (fatJar != null) {
                    JkubeProjectAssembly fileSet = getOutputDirectoryFileSet(fatJar, project);
                    assemblies.add(fileSet);
                }
            } else {
                builder.descriptorRef("artifact-with-dependencies");
            }
            builder.assemblyDef(assemblies);
        }
    }

    private List<JkubeProjectAssembly> addAdditionalFiles(JkubeProject project) {
        return Arrays.asList((createFileSet(project, "src/main/jkube-includes/bin","0755")),
                createFileSet(project, "src/main/jkube-includes","0644"));
    }

    private JkubeProjectAssembly getOutputDirectoryFileSet(FatJarDetector.Result fatJar, JkubeProject project) {
        File buildDir = new File(project.getBuildDirectory());
        return new JkubeProjectAssembly(buildDir, Arrays.asList(getRelativePath(project.getBaseDirectory(), buildDir).getPath(),
                getRelativePath(buildDir, fatJar.getArchiveFile()).getPath()), "0640");
    }

    private JkubeProjectAssembly createFileSet(JkubeProject project, String sourceDir, String fileMode) {
        return new JkubeProjectAssembly(project.getBaseDirectory(), Arrays.asList(sourceDir), fileMode);
    }

    protected boolean isFatJar() {
        return !hasMainClass() && detectFatJar() != null;
    }

    protected boolean hasMainClass() {
        return getConfig(Config.mainClass) != null;
    }

    public FatJarDetector.Result detectFatJar() {
        return fatJarDetector.scan();
    }

    protected List<String> extractPorts() {
        // TODO would rock to look at the base image and find the exposed ports!
        List<String> answer = new ArrayList<>();
        addPortIfValid(answer, getConfig(Config.webPort));
        addPortIfValid(answer, getConfig(Config.jolokiaPort));
        addPortIfValid(answer, getConfig(Config.prometheusPort));
        return answer;
    }

    protected void addPortIfValid(List<String> list, String port) {
        if (StringUtils.isNotBlank(port) && Integer.parseInt(port) > 0) {
            list.add(port);
        }
    }
}