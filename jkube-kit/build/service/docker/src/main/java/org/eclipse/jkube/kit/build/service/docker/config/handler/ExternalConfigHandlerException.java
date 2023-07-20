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
package org.eclipse.jkube.kit.build.service.docker.config.handler;

public class ExternalConfigHandlerException extends RuntimeException
{
    private static final long serialVersionUID = -2742743075207582636L;

    public ExternalConfigHandlerException(String message)
    {
        super(message);
    }

    public ExternalConfigHandlerException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
