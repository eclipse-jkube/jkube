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
 * Enricher to merge <code>JAVA_OPTS</code> environment variables defined in {@link BuildConfiguration#getEnv()}
 * with those added to {@link io.fabric8.kubernetes.api.model.Container} by other enrichers.
 *
 * <p> This prevents Container environment variables from overriding and hiding the initial JAVA_OPTS value
 * possibly added by some <code>Generator</code>.
 */
public class ContainerEnvJavaOptsMergeEnricher extends AbstractContainerEnvMergeEnricher {
 
  private static final String ENV_KEY = "JAVA_OPTS";

  public ContainerEnvJavaOptsMergeEnricher(JKubeEnricherContext enricherContext) {
    super(enricherContext, "jkube-container-env-java-opts");
  }
 
  @Override
  protected String getKey() {
    return ENV_KEY;
  }
}
