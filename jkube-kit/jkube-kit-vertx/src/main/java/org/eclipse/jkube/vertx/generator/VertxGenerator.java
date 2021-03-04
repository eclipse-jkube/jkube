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
package org.eclipse.jkube.vertx.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.generator.api.GeneratorContext;

/**
 * Vert.x Generator.
 * <p>
 * Then main part of creating the base image is taken java-exec generator.
 * <p>
 *
 * It detects whether or not it's a Vert.x project by looking if there is the vertx-core dependency associated by the
 * Maven Shader Plugin or if the project use the Vert.x Maven Plugin.
 *
 * To avoid the issue to write file in the current working directory the `cacheDirBase` is configured to `/tmp`.
 *
 * When a cluster manager is detected in the classpath, `-cluster` is automatically appended to the command line.
 *
 * To avoid DNS resolution issue, the async DNS resolver is disabled (falling back to the regular Java resolver)
 *
 * If vertx-dropwizard-metrics is in the classpath, the metrics are enabled and the JMX export is also enabled.
 */
public class VertxGenerator extends JavaExecGenerator {

  public VertxGenerator(GeneratorContext context) {
    super(context, "vertx");
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs)
        && (JKubeProjectUtil.hasPlugin(getProject(), Constants.VERTX_MAVEN_PLUGIN_GROUP, Constants.VERTX_MAVEN_PLUGIN_ARTIFACT)
        || JKubeProjectUtil.hasDependencyWithGroupId(getProject(), Constants.VERTX_GROUPID));
  }

  @Override
  protected List<String> getExtraJavaOptions() {
    List<String> opts = super.getExtraJavaOptions();
    opts.add("-Dvertx.cacheDirBase=/tmp/vertx-cache");

    if (! contains("-Dvertx.disableDnsResolver=", opts)) {
      opts.add("-Dvertx.disableDnsResolver=true");
    }

    if (JKubeProjectUtil.hasDependency(getProject(), Constants.VERTX_GROUPID, Constants.VERTX_DROPWIZARD)) {
      opts.add("-Dvertx.metrics.options.enabled=true");
      opts.add("-Dvertx.metrics.options.jmxEnabled=true");
      opts.add("-Dvertx.metrics.options.jmxDomain=vertx");
    }

    if (! contains("-Djava.net.preferIPv4Stack", opts)  && JKubeProjectUtil.hasDependency(getProject(), Constants.VERTX_GROUPID, Constants.VERTX_INFINIPAN)) {
      opts.add("-Djava.net.preferIPv4Stack=true");
    }

    return opts;
  }

  @Override
  protected Map<String, String> getEnv(boolean prePackagePhase) {
    try {
      Map<String, String> map = super.getEnv(prePackagePhase);

      String args = map.get("JAVA_ARGS");
      if (args == null) {
        args = "";
      }

      if (JKubeProjectUtil.hasResource(getProject(), Constants.CLUSTER_MANAGER_SPI)) {
        if (!args.isEmpty()) {
          args += " ";
        }
        args += "-cluster";
      }

      if (!args.isEmpty()) {
        map.put("JAVA_ARGS", args);
      }
      return map;
    } catch (IOException ioException) {
      throw new IllegalStateException("Error in finding resource", ioException);
    }
  }

  private boolean contains(String prefix, List<String> opts) {
    return opts.stream().anyMatch(val -> val.startsWith(prefix));
  }

  @Override
  protected boolean isFatJar() {
    return !hasMainClass() && isUsingFatJarPlugin() || super.isFatJar();
  }

  private boolean isUsingFatJarPlugin() {
    JavaProject project = getProject();
    return JKubeProjectUtil.hasPlugin(project, Constants.SHADE_PLUGIN_GROUP, Constants.SHADE_PLUGIN_ARTIFACT) ||
           JKubeProjectUtil.hasPlugin(project, Constants.VERTX_MAVEN_PLUGIN_GROUP, Constants.VERTX_MAVEN_PLUGIN_ARTIFACT);
  }

  @Override
  protected List<String> extractPorts() {
    Map<String, Integer> extractedPorts = new VertxPortsExtractor(log).extract(getProject());
    List<String> ports = new ArrayList<>();
    for (Integer p : extractedPorts.values()) {
      ports.add(String.valueOf(p));
    }
    // If there are no specific vertx ports found, we reuse the ports from the JavaExecGenerator
    return ports.isEmpty() ? super.extractPorts() : ports;
  }
}