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
package org.eclipse.jkube.maven.enricher.api.util;

/**
 * Some constants for Java Debugging on Kubernetes
 */
public class DebugConstants {
    public static final String ENV_VAR_JAVA_DEBUG = "JAVA_ENABLE_DEBUG";
    public static final String ENV_VAR_JAVA_DEBUG_SUSPEND = "JAVA_DEBUG_SUSPEND";
    public static final String ENV_VAR_JAVA_DEBUG_SESSION = "JAVA_DEBUG_SESSION";
    public static final String ENV_VAR_JAVA_DEBUG_PORT = "JAVA_DEBUG_PORT";
    public static final String ENV_VAR_JAVA_DEBUG_PORT_DEFAULT = "5005";
}

