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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class QuarkusGenerator extends JavaExecGenerator {

  public QuarkusGenerator(GeneratorContext context) {
    super(context, "quarkus");
  }

  @AllArgsConstructor
  public enum Config implements Configs.Config {

    // Whether to add native image or plain java image
    NATIVE_IMAGE("nativeImage", "false");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs)
        && JKubeProjectUtil.hasPlugin(getProject(), "io.quarkus", "quarkus-maven-plugin");
  }

  @Override
  protected List<String> extractPorts() {
    // TODO: Check application.properties for a port
    if (isNativeImage()) {
      return new ArrayList<>(Collections.singletonList(
          getConfig(JavaExecGenerator.Config.WEB_PORT)
      ));
    }
    return super.extractPorts();
  }

  @Override
  protected String getFromAsConfigured() {
    if (isNativeImage()) {
      return Optional.ofNullable(super.getFromAsConfigured()).orElse(getNativeFrom());
    }
    return super.getFromAsConfigured();
  }

  @Override
  protected AssemblyConfiguration createAssembly() {
    if (isNativeImage()) {
      return createAssemblyConfiguration("/", getNativeFileToInclude());
    }
    return createAssemblyConfiguration(getConfig(JavaExecGenerator.Config.TARGET_DIR), getJvmFilesToInclude());
  }

  @Override
  protected String getBuildWorkdir() {
    if (isNativeImage()) {
      return "/";
    }
    return getConfig(JavaExecGenerator.Config.TARGET_DIR);
  }

  @Override
  protected Arguments getBuildEntryPoint() {
    if (isNativeImage()) {
      final Arguments.ArgumentsBuilder ab = Arguments.builder();
      ab.execArgument("./" + findSingleFileThatEndsWith("-runner"));
      getExtraJavaOptions().forEach(ab::execArgument);
      return ab.build();
    }
    return null;
  }

  @Override
  protected boolean isFatJar() {
    return false;
  }

  @Override
  protected Map<String, String> getEnv(boolean prePackagePhase) {
    final Map<String, String> env = new HashMap<>();
    env.put(JAVA_OPTIONS, StringUtils.join(getJavaOptions(), " "));
    return env;
  }

  private static List<String> getJavaOptions() {
    return Collections.singletonList("-Dquarkus.http.host=0.0.0.0");
  }

  private boolean isNativeImage() {
    return Boolean.parseBoolean(getConfig(Config.NATIVE_IMAGE));
  }

  private String getNativeFrom() {
    if (getContext().getRuntimeMode() != RuntimeMode.OPENSHIFT) {
      return "registry.access.redhat.com/ubi8/ubi-minimal:8.1";
    }
    return "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0";
  }

  private AssemblyConfiguration createAssemblyConfiguration(String targetDir, AssemblyFileSet jKubeAssemblyFileSet) {
    jKubeAssemblyFileSet.setOutputDirectory(".");
    return AssemblyConfiguration.builder()
        .targetDir(targetDir)
        .excludeFinalOutputArtifact(true)
        .inline(Assembly.builder().fileSet(jKubeAssemblyFileSet).build())
        .build();
  }

  private AssemblyFileSet getJvmFilesToInclude() {
    AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder =
        getFileSetWithFileFromBuildThatEndsWith("-runner.jar");
    fileSetBuilder.include("lib");
    // We also need to exclude default jar file
    File defaultJarFile = JKubeProjectUtil.getFinalOutputArtifact(getContext().getProject());
    if (defaultJarFile != null) {
      fileSetBuilder.exclude(defaultJarFile.getName());
    }
    fileSetBuilder.fileMode("0640");
    return fileSetBuilder.build();
  }

  private AssemblyFileSet getNativeFileToInclude() {
    return getFileSetWithFileFromBuildThatEndsWith("-runner")
        .fileMode("0755")
        .build();
  }

  private AssemblyFileSet.AssemblyFileSetBuilder getFileSetWithFileFromBuildThatEndsWith(String suffix) {
    List<String> relativePaths = new ArrayList<>();

    String fileToInclude = findSingleFileThatEndsWith(suffix);
    if (fileToInclude != null && !fileToInclude.isEmpty()) {
      relativePaths.add(fileToInclude);
    }
    return AssemblyFileSet.builder()
        .directory(FileUtil.getRelativePath(getProject().getBaseDirectory(), getProject().getBuildDirectory()))
        .includes(relativePaths)
        .fileMode("0777");
  }

  private String findSingleFileThatEndsWith(String suffix) {
    File buildDir = getProject().getBuildDirectory();
    String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
    if (file == null || file.length != 1) {
      throw new IllegalStateException("Can't find single file with suffix '" + suffix + "' in " + buildDir + " (zero or more than one files found ending with '" + suffix + "')");
    }
    return file[0];
  }

}