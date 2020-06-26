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
package org.eclipse.jkube.generator.karaf;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.apache.commons.lang3.StringUtils;

public class KarafGenerator extends BaseGenerator {

  private static final String KARAF = "karaf";
  private static final String KARAF_MAVEN_PLUGIN_ARTIFACT_ID = "karaf-maven-plugin";

  public KarafGenerator(GeneratorContext context) {
    super(context, KARAF, new FromSelector.Default(context,KARAF));
  }

  private enum Config implements Configs.Key {
    baseDir        {{ d = "/deployments"; }},
    webPort        {{ d = "8181"; }};

    public String def() { return d; } protected String d;
  }

  @Override
  public boolean isApplicable(List<ImageConfiguration> configs) {
    return shouldAddGeneratedImageConfiguration(configs) &&
        JKubeProjectUtil.hasPluginOfAnyArtifactId(getProject(), KARAF_MAVEN_PLUGIN_ARTIFACT_ID);
  }

  @Override
  public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
    final ImageConfiguration.ImageConfigurationBuilder imageBuilder = ImageConfiguration.builder();
    final BuildConfiguration.BuildConfigurationBuilder buildBuilder = BuildConfiguration.builder();

    buildBuilder.ports(extractPorts());
    buildBuilder
        .putEnv("DEPLOYMENTS_DIR", getConfig(Config.baseDir))
        .putEnv("KARAF_HOME", "/deployments/karaf");

    addSchemaLabels(buildBuilder, log);
    addFrom(buildBuilder);
    if (!prePackagePhase) {
      buildBuilder.assembly(createDefaultAssembly());
    }
    addLatestTagIfSnapshot(buildBuilder);
    imageBuilder
        .name(getImageName())
        .alias(getAlias())
        .build(buildBuilder.build());
    configs.add(imageBuilder.build());
    return configs;
  }


  protected List<String> extractPorts() {
    return Stream.of(getConfig(Config.webPort))
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
  }

  private AssemblyConfiguration createDefaultAssembly() {
    return AssemblyConfiguration.builder()
        .targetDir(getConfig(Config.baseDir))
        .descriptorRef(KARAF)
        .name("deployments")
        .inline(Assembly.builder()
            .fileSet(AssemblyFileSet.builder()
                .directory(new File(getProject().getBuildDirectory(), "assembly"))
                .outputDirectory(new File(KARAF))
                .exclude("bin/**")
                .directoryMode("0775")
                .build())
            .fileSet(AssemblyFileSet.builder()
                .directory(getProject().getBuildDirectory().toPath().resolve("assembly").resolve("bin").toFile())
                .outputDirectory(new File(KARAF, "bin"))
                .exclude("contrib/**")
                .exclude("*.bat")
                .directoryMode("0775")
                .fileMode("0777")
                .build())
            .build())
        .build();
  }
}