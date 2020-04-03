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
package org.eclipse.jkube.kit.common;
/**
 * Interface for marking object holding a name
 *
 * @author roland
 * @since 24/07/16
 */
public interface Named {
    /**
     * Get name of this object
     * @return String denoting name
     */
    String getName();
}

