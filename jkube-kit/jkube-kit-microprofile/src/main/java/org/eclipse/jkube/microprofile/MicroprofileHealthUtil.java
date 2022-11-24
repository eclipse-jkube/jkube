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
package org.eclipse.jkube.microprofile;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import static org.eclipse.jkube.kit.common.util.SemanticVersionUtil.isVersionAtLeast;

public class MicroprofileHealthUtil {
  private static final String MICROPROFILE_HEALTH_DEPENDENCY = "microprofile-health-api";
  private static final String MICROPROFILE_HEALTH_GROUP = "org.eclipse.microprofile.health";
  private static final String MICROPROFILE_DEPENDENCY = "microprofile";
  private static final String MICROPROFILE_GROUP = "org.eclipse.microprofile";
  private static final int MICROPROFILE_HEALTH_SUPPORTED_VERSION_MAJOR = 3;
  private static final int MICROPROFILE_HEALTH_SUPPORTED_VERSION_MINOR = 1;
  public static final String DEFAULT_READINESS_PATH = "/health/ready";
  public static final String DEFAULT_LIVENESS_PATH = "/health/live";
  public static final String DEFAULT_STARTUP_PATH = "/health/started";

  private MicroprofileHealthUtil() { }

  public static boolean hasMicroProfileHealthDependency(JavaProject javaProject) {
    return JKubeProjectUtil.hasTransitiveDependency(javaProject, MICROPROFILE_HEALTH_GROUP , MICROPROFILE_HEALTH_DEPENDENCY);
  }

  public static boolean hasMicroProfileDependency(JavaProject javaProject) {
    return JKubeProjectUtil.hasTransitiveDependency(javaProject, MICROPROFILE_GROUP , MICROPROFILE_DEPENDENCY);
  }

  public static boolean isStartupEndpointSupported(JavaProject javaProject) {
    String microProfileHealthVersion;
    if (hasMicroProfileDependency(javaProject)) {
      microProfileHealthVersion = getMicroProfileVersionFromArtifactId(javaProject, MICROPROFILE_GROUP, MICROPROFILE_DEPENDENCY);
    } else if (hasMicroProfileHealthDependency(javaProject)) {
      microProfileHealthVersion = getMicroProfileVersionFromArtifactId(javaProject, MICROPROFILE_HEALTH_GROUP, MICROPROFILE_HEALTH_DEPENDENCY);
    } else {
      return false;
    }

    return isVersionAtLeast(MICROPROFILE_HEALTH_SUPPORTED_VERSION_MAJOR, MICROPROFILE_HEALTH_SUPPORTED_VERSION_MINOR, microProfileHealthVersion);
  }

  private static String getMicroProfileVersionFromArtifactId(JavaProject javaProject, String groupId, String artifactId) {
    Dependency microProfileDep = JKubeProjectUtil.getTransitiveDependency(javaProject, groupId, artifactId);
    if (microProfileDep != null) {
      return microProfileDep.getVersion();
    }
    return null;
  }
}