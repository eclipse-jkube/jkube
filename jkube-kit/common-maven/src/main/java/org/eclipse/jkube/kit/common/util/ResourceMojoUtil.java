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

import org.apache.maven.project.MavenProject;

public class ResourceMojoUtil {

    public static final String DEFAULT_RESOURCE_LOCATION = "META-INF/jkube";
    private static final String[] DEKORATE_CLASSES = new String[]{
            "io.dekorate.annotation.Dekorate"
    };

    private ResourceMojoUtil() {
    }

    public static boolean useDekorate(MavenProject project) {
        return new ProjectClassLoaders(MavenUtil.getCompileClassLoader(project))
                .isClassInCompileClasspath(true, DEKORATE_CLASSES);
    }

}
