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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
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

  protected enum JDK {
    DEFAULT("java"),
    JDK_11("java11");

    final String imagePrefix;

    JDK(String imagePrefix) {
      this.imagePrefix = imagePrefix;
    }
  }

    private static final String WEB_PORT_DEFAULT = "8080";
    private static final String JOLOKIA_PORT_DEFAULT = "8778";
    private static final String PROMETHEUS_PORT_DEFAULT = "9779";
    // Environment variable used for specifying a main class
    static final String JAVA_MAIN_CLASS_ENV_VAR = "JAVA_MAIN_CLASS";
    protected static final String JAVA_OPTIONS = "JAVA_OPTIONS";

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
        this(context, name, JDK.DEFAULT);
    }
    protected JavaExecGenerator(GeneratorContext context, String name, JDK jdk) {
        super(context, name, new FromSelector.Default(context, jdk.imagePrefix));
        fatJarDetector = new FatJarDetector(getProject().getBuildPackageDirectory());
        mainClassDetector = new MainClassDetector(getConfig(Config.MAIN_CLASS),
                getProject().getOutputDirectory(), context.getLogger());
    }

    @AllArgsConstructor
    public enum Config implements Configs.Config {
        // Webport to expose. Set to 0 if no port should be exposed
        WEB_PORT("webPort", null),

        // Jolokia from the base image to expose. Set to 0 if no such port should be exposed
        JOLOKIA_PORT("jolokiaPort", null),

        // Prometheus port from base image. Set to 0 if no required
        PROMETHEUS_PORT("prometheusPort", null),

        // Basedirectory where to put the application data into (within the Docker image
        TARGET_DIR("targetDir", "/deployments"),

        // The name of the main class for non-fat jars. If not specified it is tried
        // to find a main class within target/classes.
        MAIN_CLASS("mainClass", null);

        @Getter
        protected String key;
        @Getter(AccessLevel.PUBLIC)
        protected String defaultValue;
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        if (shouldAddGeneratedImageConfiguration(configs)) {
            // If a main class is configured, we always kick in
            if (getConfig(Config.MAIN_CLASS) != null) {
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
        imageBuilder
                .name(getImageName())
                .registry(getRegistry())
                .alias(getAlias())
                .build(initImageBuildConfiguration(prePackagePhase).build());
        configs.add(imageBuilder.build());
        return configs;
    }

    protected BuildConfiguration.BuildConfigurationBuilder initImageBuildConfiguration(boolean prePackagePhase) {
      final BuildConfiguration.BuildConfigurationBuilder buildBuilder = BuildConfiguration.builder();
      addSchemaLabels(buildBuilder, log);
      addFrom(buildBuilder);
      if (!prePackagePhase) {
        // Only add assembly if not in a pre-package phase where the referenced files
        // won't be available.
        buildBuilder.assembly(createAssembly());
      }
      getEnv(prePackagePhase).forEach(buildBuilder::putEnv);
      buildBuilder.putEnv("JAVA_APP_DIR", getConfig(Config.TARGET_DIR));

      addWebPort(buildBuilder);
      addJolokiaPort(buildBuilder);
      addPrometheusPort(buildBuilder);

      addLatestTagIfSnapshot(buildBuilder);
      buildBuilder.workdir(getBuildWorkdir());
      buildBuilder.entryPoint(getBuildEntryPoint());
      return buildBuilder;
    }

    /**
     * Hook for adding extra environment vars
     *
     * @param prePackagePhase true if running is Maven's pre-package phase.
     * @return map with environment variables to use.
     */
    protected Map<String, String> getEnv(boolean prePackagePhase) {
        Map<String, String> ret = new HashMap<>();
        if (!isFatJar()) {
            String mainClass = getConfig(Config.MAIN_CLASS);
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
        builder.name("deployments");
        builder.targetDir(getConfig(Config.TARGET_DIR));
        builder.excludeFinalOutputArtifact(isFatJar());
        builder.layer(createDefaultLayer());
        return builder.build();
    }

    private Assembly createDefaultLayer() {
        final List<AssemblyFileSet> fileSets = new ArrayList<>(addAdditionalFiles());
        final FatJarDetector.Result fatJar = detectFatJar();
        if (isFatJar() && fatJar != null) {
            fileSets.add(getOutputDirectoryFileSet(fatJar, getProject()));
        } else {
            log.warn("No fat Jar detected, make sure your image assembly configuration contains all the required" +
                " dependencies for your application to run.");
        }
        return Assembly.builder().fileSets(fileSets).build();
    }

    protected List<AssemblyFileSet> addAdditionalFiles() {
        List<AssemblyFileSet> fileSets = new ArrayList<>();
        fileSets.add(createFileSet("src/main/jkube-includes/bin","bin", "0755"));
        fileSets.add(createFileSet("src/main/jkube-includes",".", "0644"));
        return fileSets;
    }

    private static AssemblyFileSet getOutputDirectoryFileSet(FatJarDetector.Result fatJar, JavaProject project) {
        final File buildDirectory = project.getBuildPackageDirectory();
        return AssemblyFileSet.builder()
                .directory(getRelativePath(project.getBaseDirectory(), buildDirectory))
                .include(getRelativePath(buildDirectory, fatJar.getArchiveFile()).getPath())
                .outputDirectory(new File("."))
                .fileMode("0640")
                .build();
    }

    protected static AssemblyFileSet createFileSet(String sourceDir, String outputDir, String fileMode) {
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
        return getConfig(Config.MAIN_CLASS) != null;
    }

    public FatJarDetector.Result detectFatJar() {
        return fatJarDetector.scan();
    }

    protected String getDefaultWebPort() {
      return WEB_PORT_DEFAULT;
    }

    protected String getDefaultJolokiaPort() {
      return JOLOKIA_PORT_DEFAULT;
    }

    protected String getDefaultPrometheusPort() {
      return PROMETHEUS_PORT_DEFAULT;
    }

    private void addWebPort(BuildConfiguration.BuildConfigurationBuilder buildConfigBuilder) {
      final String webPort = getConfig(Config.WEB_PORT, getDefaultWebPort());
      if (isPortValid(webPort)) {
        buildConfigBuilder.port(webPort);
      }
    }

    private void addJolokiaPort(BuildConfiguration.BuildConfigurationBuilder buildConfigBuilder) {
      final String jolokiaPort = getConfig(Config.JOLOKIA_PORT, getDefaultJolokiaPort());
      if (isPortValid(jolokiaPort)) {
        buildConfigBuilder.port(jolokiaPort);
      } else {
        buildConfigBuilder.putEnv("AB_JOLOKIA_OFF", "true");
      }
    }

    private void addPrometheusPort(BuildConfiguration.BuildConfigurationBuilder buildConfigBuilder) {
      final String jolokiaPort = getConfig(Config.PROMETHEUS_PORT, getDefaultPrometheusPort());
      if (isPortValid(jolokiaPort)) {
        buildConfigBuilder.port(jolokiaPort);
      } else {
        buildConfigBuilder.putEnv("AB_PROMETHEUS_OFF", "true");
      }
    }

    protected static boolean isPortValid(String port) {
      return StringUtils.isNotBlank(port) && port.matches("\\d+") && Integer.parseInt(port) > 0;
    }

    protected String getBuildWorkdir() {
        return null;
    }

    protected Arguments getBuildEntryPoint() {
        return null;
    }
}
