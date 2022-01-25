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
package org.eclipse.jkube.generator.webapp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.eclipse.jkube.generator.webapp.handler.CustomAppServerHandler;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * A generator for WAR apps
 *
 * @author kameshs
 */
public class WebAppGenerator extends BaseGenerator {

  @AllArgsConstructor
  private enum Config implements Configs.Config {
    // App server to use (like 'tomcat', 'jetty', 'wildfly'
    SERVER("server", null),

    // Directory where to deploy to
    TARGET_DIR("targetDir", "/deployments"),

    // Unix user under which the war should be installed. If null, the default image user is used
    USER("user", null),

    // Command to execute. If null, the base image default command is used
    CMD("cmd", null),

    // Context path under which the app will be available
    PATH("path", "/"),

    // Ports to expose as a command separated list
    PORTS("ports", "8080"),

    // If from base image supports S2I builds in OpenShift cluster
    SUPPORTS_S2I_BUILD("supportsS2iBuild", "false");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  public WebAppGenerator(GeneratorContext context) {
    super(context, "webapp");
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
      return shouldAddGeneratedImageConfiguration(configs) &&
              (JKubeProjectUtil.hasPlugin(getProject(), "org.apache.maven.plugins", "maven-war-plugin")
                      || JKubeProjectUtil.hasGradlePlugin(getProject(), "org.gradle.api.plugins.WarPlugin"));
  }

  @Override
  public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
    final AppServerHandler handler = getAppServerHandler(getContext());
    if (getContext().getRuntimeMode() == RuntimeMode.OPENSHIFT &&
        getContext().getStrategy() == JKubeBuildStrategy.s2i &&
        !prePackagePhase &&
        !handler.supportsS2iBuild()
    ) {
      throw new IllegalArgumentException("S2I not yet supported for the webapp-generator. Use " +
          "-Djkube.build.strategy=docker for OpenShift mode. Please refer to the reference manual at " +
          "https://www.eclipse.org/jkube/docs for details about build modes.");
    }


    log.info("Using %s as base image for webapp", handler.getFrom());

    final ImageConfiguration.ImageConfigurationBuilder imageBuilder = ImageConfiguration.builder();

    final BuildConfiguration.BuildConfigurationBuilder buildBuilder = BuildConfiguration.builder();

    buildBuilder.from(getFrom(handler))
        .ports(handler.exposedPorts())
        .cmd(Arguments.builder().shell(getDockerRunCommand(handler)).build())
        .env(getEnv(handler));

    handler.runCmds().forEach(buildBuilder::runCmd);

    addSchemaLabels(buildBuilder, log);
    if (!prePackagePhase) {
      buildBuilder.assembly(createAssembly(handler));
    }
    addLatestTagIfSnapshot(buildBuilder);
    imageBuilder
        .name(getImageName())
        .alias(getAlias())
        .build(buildBuilder.build());
    configs.add(imageBuilder.build());

    return configs;
  }

  private AppServerHandler getAppServerHandler(GeneratorContext context) {
    String from = super.getFromAsConfigured();
    if (from != null) {
      // If a base image is provided use this exclusively and dont do a custom lookup
      return createCustomAppServerHandler(from);
    } else {
      return new AppServerDetector(context).detect(getConfig(Config.SERVER));
    }
  }

  private AppServerHandler createCustomAppServerHandler(String from) {
    final String deploymentDir = getConfig(Config.TARGET_DIR);
    final String command = getConfig(Config.CMD);
    final String user = getConfig(Config.USER);
    final List<String> ports = Arrays.asList(getConfig(Config.PORTS).split("\\s*,\\s*"));
    final boolean supportsS2iBuild = Configs.asBoolean(getConfig(Config.SUPPORTS_S2I_BUILD));
    return new CustomAppServerHandler(from, deploymentDir, command, user, ports, supportsS2iBuild);
  }

  protected Map<String, String> getEnv(AppServerHandler handler) {
    Map<String, String> defaultEnv = new HashMap<>();
    defaultEnv.put("DEPLOY_DIR", getDeploymentDir(handler));
    defaultEnv.putAll(handler.getEnv());
    return defaultEnv;
  }

  private AssemblyConfiguration createAssembly(AppServerHandler handler) {
    final File sourceFile = Objects.requireNonNull(JKubeProjectUtil.getFinalOutputArtifact(getProject()),
        "Final output artifact file was not detected");
    final String targetFilename;
    final String extension = FilenameUtils.getExtension(sourceFile.getName());
    final String path = getConfig(Config.PATH);
    if (path.equals("/") || isBlank(path)) {
      targetFilename = String.format("ROOT.%s", extension);
    } else {
      targetFilename = String.format("%s.%s", path.replaceAll("[\\\\/]", ""), extension);
    }
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder();
    builder
        .name(handler.getAssemblyName())
        .targetDir(getDeploymentDir(handler))
        .excludeFinalOutputArtifact(true)
        .inline(Assembly.builder()
            .file(AssemblyFile.builder()
                .source(sourceFile)
                .destName(targetFilename)
                .outputDirectory(new File("."))
                .build())
            .build());

    String user = getUser(handler);
    if (user != null) {
      builder.user(user);
    }
    return builder.build();
  }

  // To be called **only** from customize() as they require an already
  // initialized appServerHandler:
  protected String getFrom(AppServerHandler handler) {
    return handler.getFrom();
  }

  private String getDockerRunCommand(AppServerHandler handler) {
    return getConfig(Config.CMD, handler.getCommand());
  }

  private String getDeploymentDir(AppServerHandler handler) {
    return getConfig(Config.TARGET_DIR, handler.getDeploymentDir());
  }

  private String getUser(AppServerHandler handler) {
    return getConfig(Config.USER, handler.getUser());
  }
}
