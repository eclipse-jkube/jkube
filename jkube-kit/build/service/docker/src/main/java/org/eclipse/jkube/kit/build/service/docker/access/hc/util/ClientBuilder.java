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
package org.eclipse.jkube.kit.build.service.docker.access.hc.util;

import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * A client builder know how to build HTTP clients
 *
 * @author roland
 * @since 03/05/16
 */
public interface ClientBuilder {

    /**
     * Create a pooled client
     *
     * @return an HTTP client
     * @throws IOException IO Exception
     */
    CloseableHttpClient buildPooledClient() throws IOException;

    /**
     * Create a basic client with a single connection. This is the client which should be used
     * in long running threads
     *
     * @return an HTTP client
     * @throws IOException IO Exception
     */
    CloseableHttpClient buildBasicClient() throws IOException;

}
