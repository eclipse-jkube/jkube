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
package org.eclipse.jkube.wildfly.jar;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import static org.eclipse.jkube.kit.common.util.SemanticVersionUtil.isVersionAtLeast;

public class WildflyJarUtils {

    private WildflyJarUtils() {}
    public static final String BOOTABLE_JAR_GROUP_ID = "org.wildfly.plugins";
    public static final String BOOTABLE_JAR_ARTIFACT_ID = "wildfly-jar-maven-plugin";
    private static final int WILDLY_MAJOR_VERSION_SINCE_STARTUP_CHANGE = 25;
    private static final int WILDLY_MINOR_VERSION_SINCE_STARTUP_CHANGE = 0;

    public static final String DEFAULT_LIVENESS_PATH = "/health/live";
    public static final String DEFAULT_READINESS_PATH = "/health/ready";
    public static final String DEFAULT_STARTUP_PATH = "/health/started";

    /**
     * Check whether given Wildfly version supports startup endpoint or not by checking
     * Wildfly version is greater than 25.0.0.Final
     *
     * @param javaProject current project
     * @return boolean value indicating whether it's supported or not.
     */
    public static boolean isStartupEndpointSupported(JavaProject javaProject) {
        return isVersionAtLeast(WILDLY_MAJOR_VERSION_SINCE_STARTUP_CHANGE,
                WILDLY_MINOR_VERSION_SINCE_STARTUP_CHANGE, findWildflyVersion(javaProject));
    }

    static String findWildflyVersion(JavaProject javaProject) {
        return JKubeProjectUtil.getAnyDependencyVersionWithGroupId(javaProject, BOOTABLE_JAR_GROUP_ID);
    }
}
