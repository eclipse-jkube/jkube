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
package org.eclipse.jkube.kit.build.service.docker.access.hc.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Base class for all clients which access Docker natively
 *
 * @author roland
 * @since 08/08/16
 */
public abstract class AbstractNativeClientBuilder implements ClientBuilder {

    protected final Registry<ConnectionSocketFactory> registry;
    protected final String path;
    protected final KitLogger log;

    private final DnsResolver dnsResolver;
    private final int maxConnections;

    public AbstractNativeClientBuilder(String path, int maxConnections, KitLogger logger) {
        this.maxConnections = maxConnections;
        this.log = logger;
        this.path = path;
        dnsResolver = nullDnsResolver();
        registry = buildRegistry(path);
    }

    protected abstract ConnectionSocketFactory getConnectionSocketFactory();
    protected abstract String getProtocol();

    @Override
    public CloseableHttpClient buildPooledClient() {
        final HttpClientBuilder httpBuilder = HttpClients.custom();
        final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(registry, dnsResolver);
        manager.setDefaultMaxPerRoute(maxConnections);
        httpBuilder.setConnectionManager(manager);
        return httpBuilder.build();
    }

    @Override
    public CloseableHttpClient buildBasicClient() throws IOException {
        BasicHttpClientConnectionManager manager = new BasicHttpClientConnectionManager(registry, null, null, dnsResolver);
        return HttpClients.custom().setConnectionManager(manager).build();
    }

    // =========================================================================================================

    private Registry<ConnectionSocketFactory> buildRegistry(String path) {
        final RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        registryBuilder.register(getProtocol(), getConnectionSocketFactory());
        return registryBuilder.build();
    }

    private DnsResolver nullDnsResolver() {
        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                return new InetAddress[] {null};
            }
        };
    }
}
