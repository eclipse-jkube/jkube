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
package io.jkube.kit.build.service.docker.access.hc.http;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import io.jkube.kit.build.service.docker.access.KeyStoreUtil;
import io.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import io.jkube.kit.build.service.docker.access.KeyStoreUtil;
import io.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

/**
 * @author roland
 * @since 05/06/15
 */
public class HttpClientBuilder implements ClientBuilder {

    private final String certPath;
    private final int maxConnections;

    public HttpClientBuilder(String certPath, int maxConnections) {
        this.certPath = certPath;
        this.maxConnections = maxConnections;
    }

    public CloseableHttpClient buildPooledClient() throws IOException {
        org.apache.http.impl.client.HttpClientBuilder builder = HttpClients.custom();
        HttpClientConnectionManager manager = getPooledConnectionFactory(certPath, maxConnections);
        builder.setConnectionManager(manager);
        // TODO: For push-redirects working for 301, the redirect strategy should be relaxed (see #351)
        // However not sure whether we should do it right now and whether this is correct, since normally
        // a 301 should only occur when the image name is invalid (e.g. containing "//" in which case a redirect
        // happens to the URL with a single "/")
        // builder.setRedirectStrategy(new LaxRedirectStrategy());

        // TODO: Tune client if needed (e.g. add pooling factoring .....
        // But I think, that's not really required.

        return builder.build();
    }

    public CloseableHttpClient buildBasicClient() throws IOException {
        return HttpClients.custom().setConnectionManager(getBasicConnectionFactory(certPath)).build();
    }

    private static HttpClientConnectionManager getPooledConnectionFactory(String certPath, int maxConnections) throws IOException {
        PoolingHttpClientConnectionManager ret = certPath != null ?
                new PoolingHttpClientConnectionManager(getSslFactoryRegistry(certPath)) :
                new PoolingHttpClientConnectionManager();
        ret.setDefaultMaxPerRoute(maxConnections);
        ret.setMaxTotal(maxConnections);
        return ret;
    }

    private static HttpClientConnectionManager getBasicConnectionFactory(String certPath) throws IOException {
        return certPath != null ?
            new BasicHttpClientConnectionManager(getSslFactoryRegistry(certPath)) :
            new BasicHttpClientConnectionManager();
    }

    private static Registry<ConnectionSocketFactory> getSslFactoryRegistry(String certPath) throws IOException {
        try
        {
            KeyStore keyStore = KeyStoreUtil.createDockerKeyStore(certPath);

            SSLContext sslContext =
                    SSLContexts.custom()
                               .setProtocol(SSLConnectionSocketFactory.TLS)
                               .loadKeyMaterial(keyStore, "docker".toCharArray())
                               .loadTrustMaterial(keyStore, null)
                               .build();
            String tlsVerify = System.getenv("DOCKER_TLS_VERIFY");
            SSLConnectionSocketFactory sslsf =
                    tlsVerify != null && !tlsVerify.equals("0") && !tlsVerify.equals("false") ?
                            new SSLConnectionSocketFactory(sslContext) :
                            new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            return RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslsf).build();
        }
        catch (GeneralSecurityException e) {
            // this isn't ideal but the net effect is the same
            throw new IOException(e);
        }
    }
}
