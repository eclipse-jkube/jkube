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

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;


/**
 * Enricher to merge <code>JAVA_OPTIONS</code> environment variables defined in {@link BuildConfiguration#getEnv()}
 * with those added to {@link io.fabric8.kubernetes.api.model.Container} by other enrichers.
 *
 * <p> This prevents Container environment variables from overriding and hiding the initial JAVA_OPTIONS value
 * possibly added by some <code>Generator</code>.
 */
public class ContainerEnvJavaOptionsMergeEnricher extends AbstractContainerEnvMergeEnricher {

  public static final String ENV_KEY = "JAVA_OPTIONS";
    
  public ContainerEnvJavaOptionsMergeEnricher(JKubeEnricherContext enricherContext) {
    super(enricherContext, "jkube-container-env-java-options");
  }

  @Override
  protected String getKey() {
    return ENV_KEY;
  }
}
