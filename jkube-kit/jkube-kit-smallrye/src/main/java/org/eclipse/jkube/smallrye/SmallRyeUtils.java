/*
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
package org.eclipse.jkube.smallrye;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

public class SmallRyeUtils {
  private static final String SMALLRYE_GROUPID = "io.smallrye";
  private static final String SMALLRYE_HEALTH_ARTIFACTID = "smallrye-health";

  private SmallRyeUtils() { }

  public static boolean hasSmallRyeDependency(JavaProject javaProject) {
    return JKubeProjectUtil.hasDependency(javaProject, SMALLRYE_GROUPID , SMALLRYE_HEALTH_ARTIFACTID);
  }
}