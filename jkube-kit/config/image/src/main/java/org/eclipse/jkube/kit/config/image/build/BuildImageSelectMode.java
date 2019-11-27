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
package org.eclipse.jkube.kit.config.image.build;

/**
 * How to build source tar files.
 *
 * @author roland
 * @since 26/04/16
 */
public enum BuildImageSelectMode {

    // Pick only the first build configuration
    first,

    // Include all builds with alias names as classifiers
    all;

}
