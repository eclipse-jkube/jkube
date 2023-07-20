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
package org.eclipse.jkube.generator.api;

/**
 * Modes which influence how generators are creating image configurations
 *
 * @author roland
 * @since 03.10.18
 */
public enum GeneratorMode {

    /**
     * Regular build mode. Image will be created which are used in production
     */
    BUILD,

    /**
     * Special generation mode used for watching
     */
    WATCH,

    /**
     * Generate image suitable for remote debugging
     */
    DEBUG
}
