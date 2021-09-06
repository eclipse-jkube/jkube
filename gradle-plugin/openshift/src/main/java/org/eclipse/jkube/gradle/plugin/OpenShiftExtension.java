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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.gradle.api.provider.Property;

import java.io.File;
import java.nio.file.Paths;

public abstract class OpenShiftExtension extends KubernetesExtension {
  public static final String DEFAULT_OPENSHIFT_MANIFEST = Paths.get("META-INF","jkube","openshift.yml").toString();
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

  @Override
  public RuntimeMode getRuntimeMode() {
    return RuntimeMode.OPENSHIFT;
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
  public File getManifest(KitLogger kitLogger, KubernetesClient kubernetesClient, JavaProject javaProject) {
    if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
      return getOpenShiftManifest().getOrElse(new File(javaProject.getBaseDirectory(), DEFAULT_OPENSHIFT_MANIFEST));
    }
    return getKubernetesManifestOrDefault(javaProject);
  }

  @Override
  public JKubeBuildStrategy getBuildStrategy() {
    return buildStrategy != null ? buildStrategy : JKubeBuildStrategy.s2i;
  }

  @Override
  public boolean isDockerAccessRequired() {
    return false;
  }

  public String getOpenshiftPullSecretOrDefault() {
    return getOpenshiftPullSecret().getOrElse(DEFAULT_OPENSHIFT_PULLSECRET);
  }

  public String getS2iBuildNameSuffixOrDefault() {
    return getS2iBuildNameSuffix().getOrElse(DEFAULT_S2I_BUILDNAME_SUFFIX);
  }

  public boolean getS2iImageStreamLookupPolicyLocalOrDefault() {
    return getS2iImageStreamLookupPolicyLocal().getOrElse(true);
  }

  public String getBuildOutputKindOrDefault() {
    return getBuildOutputKind().getOrElse(DEFAULT_BUILD_OUTPUT_KIND);
  }
}
