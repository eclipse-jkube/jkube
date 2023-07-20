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
package org.eclipse.jkube.kit.config.resource;

/**
 * @author roland
 * @since 22/03/16
 */
public class MetricsConfig {

    // Port to export for prometheus metrics
    private int port;

    // Scheme to export for metrics
    private String scheme;
}
