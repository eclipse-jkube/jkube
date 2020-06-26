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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;


/**
 * @author roland
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
                getProject().getOutputDirectory(), context.getLogger());
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

        // The name of the main class for non-fat jars. If not specified it is tried
        // to find a main class within target/classes.
        mainClass,

        // Reference to a predefined assembly descriptor to use. By default it is tried to be detected
        assemblyRef;

        public String def() { return d; } protected String d;
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        if (shouldAddGeneratedImageConfiguration(configs)) {
            // If a main class is configured, we always kick in
            if (getConfig(Config.mainClass) != null) {
                return true;
            }
            // Check for the existing of plugins indicating a plain java exec app
            for (String[] plugin : JAVA_EXEC_MAVEN_PLUGINS) {
                if (JKubeProjectUtil.hasPlugin(getProject(), plugin[0], plugin[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
        final ImageConfiguration.ImageConfigurationBuilder imageBuilder = ImageConfiguration.builder();
        final BuildConfiguration.BuildConfigurationBuilder buildBuilder = BuildConfiguration.builder();

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
                .build(buildBuilder.build());
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
                if (mainClass == null && !prePackagePhase) {
                    throw new IllegalStateException("Cannot extract main class to startup");
                }
            }
            if (mainClass != null) {
                log.verbose("Detected main class %s", mainClass);
                ret.put(JAVA_MAIN_CLASS_ENV_VAR, mainClass);
            }
        }
        List<String> javaOptions = getExtraJavaOptions();
        if (!javaOptions.isEmpty()) {
            ret.put(JAVA_OPTIONS, StringUtils.join(javaOptions.iterator(), " "));
        }
        return ret;
    }

    protected List<String> getExtraJavaOptions() {
        return new ArrayList<>();
    }

    protected AssemblyConfiguration createAssembly() {
        final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder();
        builder.targetDir(getConfig(Config.targetDir));
        addAssembly(builder);
        builder.name("deployments");
        return builder.build();
    }

    protected void addAssembly(AssemblyConfiguration.AssemblyConfigurationBuilder builder) {
        String assemblyRef = getConfig(Config.assemblyRef);
        if (assemblyRef != null) {
            builder.descriptorRef(assemblyRef);
        } else {
            final List<AssemblyFileSet> fileSets = new ArrayList<>(addAdditionalFiles());
            if (isFatJar()) {
                FatJarDetector.Result fatJar = detectFatJar();
                if (fatJar != null) {
                    fileSets.add(getOutputDirectoryFileSet(fatJar, getProject()));
                }
            } else {
                builder.descriptorRef("artifact-with-dependencies");
            }
            builder.inline(Assembly.builder().fileSets(fileSets).build());
        }
    }

    public List<AssemblyFileSet> addAdditionalFiles() {
        List<AssemblyFileSet> fileSets = new ArrayList<>();
        fileSets.add(createFileSet("src/main/jkube-includes/bin","bin", "0755"));
        fileSets.add(createFileSet("src/main/jkube-includes",".", "0644"));
        return fileSets;
    }

    public AssemblyFileSet getOutputDirectoryFileSet(FatJarDetector.Result fatJar, JavaProject project) {
        final File buildDirectory = project.getBuildDirectory();
        return AssemblyFileSet.builder()
                .directory(getRelativePath(project.getBaseDirectory(), buildDirectory))
                .include(getRelativePath(buildDirectory, fatJar.getArchiveFile()).getPath())
                .outputDirectory(new File("."))
                .fileMode("0640")
                .build();
    }

    public AssemblyFileSet createFileSet(String sourceDir, String outputDir, String fileMode) {
        return AssemblyFileSet.builder()
                .directory(new File(sourceDir))
                .outputDirectory(new File(outputDir))
                .fileMode(fileMode)
                .build();
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