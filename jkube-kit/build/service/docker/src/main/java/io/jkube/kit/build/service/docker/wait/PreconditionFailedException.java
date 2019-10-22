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
package io.jkube.kit.build.service.docker.wait;

/**
 * Wait for a certain amount of time
 *
 * @author roland
 * @since 25/03/2017
 */
public class PreconditionFailedException extends Exception {
    private final long waited;

    PreconditionFailedException(String message, long waited) {
        super(message);
        this.waited = waited;
    }

    public long getWaited() {
        return waited;
    }
}

