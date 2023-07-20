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
package org.eclipse.jkube.kit.config.service;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class JKubeServiceException extends Exception {

    public JKubeServiceException() {
    }

    public JKubeServiceException(String message) {
        super(message);
    }

    public JKubeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public JKubeServiceException(Throwable cause) {
        super(cause);
    }

    public JKubeServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
