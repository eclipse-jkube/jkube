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
package org.eclipse.jkube.kit.enricher.api.util;


public class SecretConstants {
    public static final String KIND = "Secret";
    public static final String DOCKER_CONFIG_TYPE = "kubernetes.io/dockercfg";
    public static final String DOCKER_DATA_KEY = ".dockercfg";

    private SecretConstants() { }
}
