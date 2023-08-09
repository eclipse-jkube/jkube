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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.ExternalCommand;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.AbstractImageBuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getValueFromProperties;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.SPRING_BOOT_GROUP_ID;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.isSpringBootBuildImageSupported;

public class SpringBuildService extends AbstractImageBuildService {
  private static final String SPRING_BOOT_BUILD_IMAGE_GOAL = "org.springframework.boot:spring-boot-maven-plugin:build-image";
  private static final String SPRING_BOOT_BUILD_IMAGE_TASK = "bootBuildImage";

  private final JKubeConfiguration jKubeConfiguration;
  private final DockerServiceHub dockerServices;
  private final BuildServiceConfig buildServiceConfig;
  private final KitLogger kitLogger;

  public SpringBuildService(JKubeServiceHub jKubeServiceHub) {
    super(jKubeServiceHub);
    this.jKubeConfiguration = Objects.requireNonNull(jKubeServiceHub.getConfiguration(),
        "JKubeConfiguration is required");
    this.dockerServices = Objects.requireNonNull(jKubeServiceHub.getDockerServiceHub(),
        "Docker Service Hub is required");
    this.buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
        "BuildServiceConfig is required");
    this.kitLogger = Objects.requireNonNull(jKubeServiceHub.getLog());
  }

  @Override
  protected void buildSingleImage(ImageConfiguration imageConfiguration) {
    kitLogger.info("Delegating container image building process to Spring Boot");
    ImageNameFormatter imageNameFormatter = new ImageNameFormatter(jKubeConfiguration.getProject(), new Date());
    String defaultName = imageNameFormatter.format(Optional.ofNullable(getValueFromProperties(jKubeConfiguration.getProperties(),
        "jkube.image.name", "jkube.generator.name")).orElse("%g/%a:%l"));
    if (JKubeProjectUtil.hasPlugin(jKubeConfiguration.getProject(), SPRING_BOOT_GROUP_ID, SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID)) {
      executeGradleSpringBootBuildImageTask(defaultName);
    } else {
      executeMavenSpringBootBuildImageTask(defaultName);
    }
  }

  @Override
  protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
    try {
      dockerServices.getRegistryService().pushImage(imageConfiguration, retries, registryConfig, skipTag);
    } catch (IOException ex) {
      throw new JKubeServiceException("Error while trying to push the image: " + ex.getMessage(), ex);
    }
  }

  @Override
  public boolean isApplicable() {
    return buildServiceConfig.getJKubeBuildStrategy() != null &&
        buildServiceConfig.getJKubeBuildStrategy().equals(JKubeBuildStrategy.spring) &&
        isSpringBootBuildImageSupported(jKubeConfiguration.getProject());
  }

  @Override
  public void postProcess() {
    // NOOP
  }

  private void executeMavenSpringBootBuildImageTask(String imageName) {
    MavenSpringBootBuildImageTaskCommand mavenSpringBootBuildImageTaskCommand = new MavenSpringBootBuildImageTaskCommand(kitLogger, jKubeConfiguration.getBasedir(), imageName, jKubeConfiguration.getProject().getCommandExecutionArgs());
    try {
      mavenSpringBootBuildImageTaskCommand.execute();
    } catch (IOException e) {
      throw new IllegalStateException("Failure in executing Spring Boot MAVEN Plugin " + SPRING_BOOT_BUILD_IMAGE_GOAL, e);
    }
  }

  private void executeGradleSpringBootBuildImageTask(String imageName) {
    GradleSpringBootBuildImageTaskCommand gradleSpringBootBuildImageTaskCommand = new GradleSpringBootBuildImageTaskCommand(kitLogger, jKubeConfiguration.getBasedir(), imageName, jKubeConfiguration.getProject().getCommandExecutionArgs());
    try {
      gradleSpringBootBuildImageTaskCommand.execute();
    } catch (IOException e) {
      throw new IllegalStateException("Failure in executing Spring Boot Gradle Plugin " + SPRING_BOOT_BUILD_IMAGE_TASK, e);
    }
  }

  static class MavenSpringBootBuildImageTaskCommand extends SpringBootBuildImageTaskCommand {
    private final String imageName;
    private final File baseDir;
    private final List<String> commandExecutionArgs;

    protected MavenSpringBootBuildImageTaskCommand(KitLogger log, File projectBaseDir, String customImageName, List<String> commandExecutionArgs) {
      super(log, projectBaseDir);
      this.baseDir = projectBaseDir;
      this.imageName = customImageName;
      this.commandExecutionArgs = commandExecutionArgs;
    }

    @Override
    protected String[] getArgs() {
      List<String> args = new ArrayList<>();
      args.add(getApplicableBinary("./mvnw", "mvnw.cmd", "mvn"));
      args.add(SPRING_BOOT_BUILD_IMAGE_GOAL);
      args.add("-f");
      args.add(baseDir.getAbsolutePath());
      args.add("-Dspring-boot.build-image.imageName=" + imageName);
      args.addAll(getFilteredCommandExecutionArgs());
      return args.toArray(new String[0]);
    }

    private List<String> getFilteredCommandExecutionArgs() {
      return commandExecutionArgs.stream()
          .filter(a -> !a.contains(":"))
          .collect(Collectors.toList());
    }
  }

  static class GradleSpringBootBuildImageTaskCommand extends SpringBootBuildImageTaskCommand {
    private final String imageName;
    private final List<String> commandExecutionArgs;

    protected GradleSpringBootBuildImageTaskCommand(KitLogger log, File projectBaseDir, String customImageName, List<String> commandExecutionArgs) {
      super(log, projectBaseDir);
      this.imageName = customImageName;
      this.commandExecutionArgs = commandExecutionArgs;
    }

    @Override
    protected String[] getArgs() {
      List<String> args = new ArrayList<>();
      args.add(getApplicableBinary("./gradlew", "gradlew.bat", "gradle"));
      args.add(SPRING_BOOT_BUILD_IMAGE_TASK);
      args.add("--imageName=" + imageName);
      args.addAll(getFilteredCommandExecutionArgs());
      return args.toArray(new String[0]);
    }

    private Collection<String> getFilteredCommandExecutionArgs() {
      return commandExecutionArgs.stream()
          .filter(a -> a.contains("-"))
          .collect(Collectors.toList());
    }
  }

  private abstract static class SpringBootBuildImageTaskCommand extends ExternalCommand {
    private final KitLogger logger;
    private final File baseDir;

    public SpringBootBuildImageTaskCommand(KitLogger log, File baseDirectory) {
      super(log);
      this.logger = log;
      this.baseDir = baseDirectory;
    }

    @Override
    protected void processLine(String line) {
      logger.info("[[s]]%s", line);
    }

    protected String getApplicableBinary(String wrapperBinaryLinux, String windowsWrapperBinary, String defaultBinary) {
      String wrapperBinary = SystemUtils.IS_OS_WINDOWS ? windowsWrapperBinary : wrapperBinaryLinux;
      File localWrapper = new File(baseDir, wrapperBinary);
      if (localWrapper.exists()) {
        return wrapperBinary;
      }
      return defaultBinary;
    }
  }
}