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
package org.eclipse.jkube.kit.common.util;


import org.eclipse.jkube.kit.common.JavaProject;

public class DekorateUtil {

    public static final String DEFAULT_RESOURCE_LOCATION = "META-INF/jkube";
    public static final String DEKORATE_GROUP = "io.dekorate";

    private DekorateUtil() {
    }

    public static boolean useDekorate(JavaProject project) {
      return JKubeProjectUtil.hasDependencyWithGroupId(project, DEKORATE_GROUP);
    }

}
