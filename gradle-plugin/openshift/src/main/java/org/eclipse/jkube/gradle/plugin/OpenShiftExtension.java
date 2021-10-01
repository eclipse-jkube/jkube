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
package org.eclipse.jkube.gradle.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.gradle.api.provider.Property;

public abstract class OpenShiftExtension extends KubernetesExtension {

  public static final Path DEFAULT_OPENSHIFT_MANIFEST = Paths.get("META-INF","jkube","openshift.yml");
  public static final String DEFAULT_LOG_PREFIX = "oc: ";
  private static final String DEFAULT_OPENSHIFT_PULLSECRET = "pullsecret-jkube";
  private static final String DEFAULT_S2I_BUILDNAME_SUFFIX = "-s2i";
  public static final String DEFAULT_BUILD_OUTPUT_KIND = "ImageStreamTag";

  public abstract Property<File> getOpenShiftManifest();

  public abstract Property<String> getOpenshiftPullSecret();

  public abstract Property<String> getS2iBuildNameSuffix();

  public abstract Property<Boolean> getS2iImageStreamLookupPolicyLocal();

  public abstract Property<String> getBuildOutputKind();

  public abstract Property<String> getOpenshiftPushSecret();

  public abstract Property<File> getImageStreamManifest();

  @Override
  public RuntimeMode getRuntimeMode() {
    return RuntimeMode.OPENSHIFT;
  }

  @Override
  public boolean isDockerAccessRequired() {
    return false;
  }

  @Override
  public PlatformMode getPlatformMode() {
    return PlatformMode.openshift;
  }

  @Override
  public ResourceClassifier getResourceClassifier() {
    return ResourceClassifier.OPENSHIFT;
  }

  @Override
  public File getManifest(KitLogger kitLogger, KubernetesClient kubernetesClient) {
    if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
      return getOpenShiftManifestOrDefault();
    }
    return getKubernetesManifestOrDefault();
  }

  @Override
  public JKubeBuildStrategy getBuildStrategyOrDefault() {
    return getProperty("jkube.build.strategy", JKubeBuildStrategy::valueOf)
        .orElse(buildStrategy != null ? buildStrategy : JKubeBuildStrategy.s2i);
  }

  public String getOpenshiftPullSecretOrDefault() {
    return getOrDefaultString("jkube.build.pullSecret", this::getOpenshiftPullSecret, DEFAULT_OPENSHIFT_PULLSECRET);
  }

  public String getS2iBuildNameSuffixOrDefault() {
    return getOrDefaultString("jkube.s2i.buildNameSuffix", this::getS2iBuildNameSuffix, DEFAULT_S2I_BUILDNAME_SUFFIX);
  }

  public boolean getS2iImageStreamLookupPolicyLocalOrDefault() {
    return getOrDefaultBoolean("jkube.s2i.imageStreamLookupPolicyLocal", this::getS2iImageStreamLookupPolicyLocal, true);
  }

  public String getBuildOutputKindOrDefault() {
    return getOrDefaultString("jkube.build.buildOutput.kind", this::getBuildOutputKind, DEFAULT_BUILD_OUTPUT_KIND);
  }

  @Override
  public boolean getProcessTemplatesLocallyOrDefault() {
    return getOrDefaultBoolean("jkube.deploy.processTemplatesLocally", this::getProcessTemplatesLocally, false);
  }

  @Override
  public boolean isSupportOAuthClients() {
    return true;
  }

  public File getOpenShiftManifestOrDefault() {
    return getOrDefaultFile("jkube.openshiftManifest", this::getOpenShiftManifest, javaProject.getOutputDirectory().toPath().resolve(DEFAULT_OPENSHIFT_MANIFEST).toFile());
  }

  public File getImageStreamManifestOrDefault() {
    return getOrDefaultFile("jkube.openshiftImageStreamManifest", this::getImageStreamManifest, javaProject.getBuildDirectory().toPath().resolve(Paths.get(javaProject.getArtifactId() + "-is.yml")).toFile());
  }
}
