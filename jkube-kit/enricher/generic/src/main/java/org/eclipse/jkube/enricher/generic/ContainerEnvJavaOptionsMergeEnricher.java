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
package org.eclipse.jkube.enricher.generic;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

import static org.eclipse.jkube.kit.common.Configs.asBoolean;

/**
 * Enricher to merge <code>JAVA_OPTIONS</code> environment variables defined in {@link BuildConfiguration#getEnv()}
 * with those added to {@link io.fabric8.kubernetes.api.model.Container} by other enrichers.
 *
 * <p> This prevents Container environment variables from overriding and hiding the initial JAVA_OPTIONS value
 * possibly added by some <code>Generator</code>.
 */
public class ContainerEnvJavaOptionsMergeEnricher extends BaseEnricher {

  @AllArgsConstructor
  private enum Config implements Configs.Config {
    // What pull policy to use when fetching images
    DISABLE("disable", "false");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  public ContainerEnvJavaOptionsMergeEnricher(JKubeEnricherContext enricherContext) {
    super(enricherContext, "jkube-container-env-java-options");
  }

  @Override
  public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
    if (!asBoolean(getConfig(Config.DISABLE)) && hasImageConfiguration()) {
      builder.accept(new ContainerEnvJavaOptionsMergeVisitor(getImages()));
    }
  }

  static final class ContainerEnvJavaOptionsMergeVisitor extends TypedVisitor<ContainerBuilder> {

    private static final String ENV_KEY = "JAVA_OPTIONS";

    private final List<ImageConfiguration> imageConfigurations;

    public ContainerEnvJavaOptionsMergeVisitor(List<ImageConfiguration> imageConfigurations) {
      this.imageConfigurations = imageConfigurations;
    }

    @Override
    public void visit(ContainerBuilder containerBuilder) {
      imageConfigurations.stream()
          .filter(ic -> ImageEnricher.containerImageName(ic).equals(containerBuilder.getImage()))
          .filter(ic -> ic.getBuild() != null)
          .filter(ic -> ic.getBuild().getEnv() != null)
          .filter(ic -> !ic.getBuild().getEnv().isEmpty())
          .filter(ic -> ic.getBuild().getEnv().containsKey(ENV_KEY))
          .findFirst()
          .ifPresent(ic -> containerBuilder.withEnv(mergeEnv(containerBuilder.buildEnv(), ic)));
    }

    private List<EnvVar> mergeEnv(List<EnvVar> envVars, ImageConfiguration imageConfiguration) {
      final List<EnvVar> ret = new ArrayList<>();
      for (EnvVar env : envVars) {
        if (env.getName().equalsIgnoreCase(ENV_KEY)) {
          final EnvVar merged = new EnvVar();
          merged.setName(env.getName());
          merged.setValueFrom(env.getValueFrom());
          merged.setValue(String.format("%s %s",
              imageConfiguration.getBuild().getEnv().getOrDefault(ENV_KEY, ""),
              env.getValue()
          ));
          ret.add(merged);
        } else {
          ret.add(env);
        }
      }
      return ret;
    }
  }
}
