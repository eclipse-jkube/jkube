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

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.eclipse.jkube.generator.webapp.handler.CustomAppServerHandler;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

/**
 * A generator for WAR apps
 *
 * @author kameshs
 */
public class WebAppGenerator extends BaseGenerator {

  @SuppressWarnings("squid:S00115")
  private enum Config implements Configs.Key {
    // App server to use (like 'tomcat', 'jetty', 'wildfly'
    server,

    // Directory where to deploy to
    targetDir,

    // Unix user under which the war should be installed. If null, the default image user is used
    user,

    // Command to execute. If null, the base image default command is used
    cmd,

    // Context path under which the app will be available
    path {{ d = "/"; }},

    // Ports to expose as a command separated list
    ports,

    // If from base image supports S2I builds in OpenShift cluster
    supportsS2iBuild {{ d = "false"; }};

    protected String d;

    public String def() { return d; }
  }

  public WebAppGenerator(GeneratorContext context) {
    super(context, "webapp");
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs) &&
            JKubeProjectUtil.hasPlugin(getProject(), "org.apache.maven.plugins", "maven-war-plugin");
  }

  @Override
  public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
    final AppServerHandler handler = getAppServerHandler(getContext());
    if (getContext().getRuntimeMode() == RuntimeMode.OPENSHIFT &&
        getContext().getStrategy() == OpenShiftBuildStrategy.s2i &&
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
      return new AppServerDetector(context).detect(getConfig(Config.server));
    }
  }

  private AppServerHandler createCustomAppServerHandler(String from) {
    final String deploymentDir = getConfig(Config.targetDir, "/deployments");
    final String command = getConfig(Config.cmd);
    final String user = getConfig(Config.user);
    final List<String> ports = Arrays.asList(getConfig(Config.ports, "8080").split("\\s*,\\s*"));
    final boolean supportsS2iBuild = Configs.asBoolean(getConfig(Config.supportsS2iBuild));
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
    if (getConfig(Config.path).equals("/")) {
      targetFilename = String.format("ROOT.%s",  FilenameUtils.getExtension(sourceFile.getName()));
    } else {
      targetFilename =  sourceFile.getName();
    }
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder();
    builder
        .descriptorRef("webapp")
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
    return getConfig(Config.cmd, handler.getCommand());
  }

  private String getDeploymentDir(AppServerHandler handler) {
    return getConfig(Config.targetDir, handler.getDeploymentDir());
  }

  private String getUser(AppServerHandler handler) {
    return getConfig(Config.user, handler.getUser());
  }
}
