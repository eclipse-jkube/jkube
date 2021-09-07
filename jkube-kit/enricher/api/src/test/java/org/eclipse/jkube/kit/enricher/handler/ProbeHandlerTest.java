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
package org.eclipse.jkube.kit.enricher.handler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.config.resource.ProbeConfig;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import org.junit.Test;

public class ProbeHandlerTest {
    Probe probe;
    ProbeHandler probeHandler = new ProbeHandler();

    ProbeConfig probeConfig;

    @Test
    public void getProbeEmptyTest() {
        //EmptyProbeConfig

        probeConfig = null;

        probe = probeHandler.getProbe(probeConfig);

        assertNull(probe);
    }

    @Test
    public void getProbeNullTest() {
        //ProbeConfig without any action

        probeConfig = ProbeConfig.builder().build();

        probe = probeHandler.getProbe(probeConfig);

        assertNull(probe);
    }

    @Test
    public void getHTTPProbeWithEmptyURLTest() {
        //ProbeConfig with HTTPGet Action

        //withEmptyUrl
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl(null)
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNull(probe);
    }

    @Test
    public void getHTTPProbeWithHTTPURLTest() {

        //ProbeConfig with HTTPGet Action
        //withUrl
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("http://www.healthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
        assertEquals("www.healthcheck.com",probe.getHttpGet().getHost());
        assertNull(probe.getHttpGet().getHttpHeaders());
        assertEquals("/healthz",probe.getHttpGet().getPath());
        assertEquals(8080,probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("HTTP",probe.getHttpGet().getScheme());
        assertNull(probe.getExec());
        assertNull(probe.getTcpSocket());
    }

    @Test
    public void getHTTPProbeWithoutHTTPURLTest() {
        //ProbeConfig with HTTPGet Action
        //URL Without http Portocol
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("www.healthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertNull(probe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getHTTPProbeWithInvalidURLTest() {
        //ProbeConfig with HTTPGet Action
        //withInvalidUrl
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("httphealthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
    }

    @Test
    public void getExecProbeWithEmptyExecTest() {
        //ProbeConfig with Exec Action
        //withEmptyExec
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).exec("")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNull(probe);
    }

    @Test
    public void getExecProbeWithExecTest() {
        //ProbeConfig with Exec Action
        //withExec
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).exec("cat /tmp/probe")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
        assertNotNull(probe.getExec());
        assertEquals(2,probe.getExec().getCommand().size());
        assertEquals("[cat, /tmp/probe]",probe.getExec().getCommand().toString());
        assertNull(probe.getHttpGet());
        assertNull(probe.getTcpSocket());
    }

    @Test
    public void getExecProbeWithInvalidExecTest() {
        //ProbeConfig with Exec Action
        //withInvalidExec
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).exec("   ")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNull(probe);
    }

    @Test
    public void getTCPProbeWithoutURLTest() {
        //ProbeConfig with TCP Action
        //withno url, only port
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertNull(probe.getHttpGet());
        assertNotNull(probe.getTcpSocket());
        assertEquals(80,probe.getTcpSocket().getPort().getIntVal().intValue());
        assertNull(probe.getTcpSocket().getHost());
        assertNull(probe.getExec());
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
    }

    @Test
    public void getTCPProbeWithHTTPURLAndPortTest() {
        //ProbeConfig with TCP Action
        //withport and url but with http request
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("http://www.healthcheck.com:8080/healthz").tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertNull(probe.getTcpSocket());
        assertNull(probe.getExec());
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
        assertEquals("www.healthcheck.com",probe.getHttpGet().getHost());
        assertNull(probe.getHttpGet().getHttpHeaders());
        assertEquals("/healthz",probe.getHttpGet().getPath());
        assertEquals(8080,probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("HTTP",probe.getHttpGet().getScheme());
    }

    @Test
    public void getTCPProbeWithNonHTTPURLTest() {
        //ProbeConfig with TCP Action
        //withport and url but with other request and port as int
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .failureThreshold(3).successThreshold(1)
                .getUrl("tcp://www.healthcheck.com:8080/healthz").tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertNull(probe.getHttpGet());
        assertNotNull(probe.getTcpSocket());
        assertNull(probe.getExec());
        assertEquals(80,probe.getTcpSocket().getPort().getIntVal().intValue());
        assertEquals("www.healthcheck.com",probe.getTcpSocket().getHost());
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
        assertEquals(3, probe.getFailureThreshold().intValue());
        assertEquals(1, probe.getSuccessThreshold().intValue());
    }

    @Test
    public void getTCPProbeWithNonHTTPURLAndStringPortTest() {
        //ProbeConfig with TCP Action
        //withport and url but with other request and port as string
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("tcp://www.healthcheck.com:8080/healthz").tcpPort("httpPort")
                .successThreshold(1)
                .failureThreshold(3)
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertNull(probe.getHttpGet());
        assertNotNull(probe.getTcpSocket());
        assertNull(probe.getExec());
        assertEquals("httpPort",probe.getTcpSocket().getPort().getStrVal());
        assertEquals("www.healthcheck.com",probe.getTcpSocket().getHost());
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
        assertEquals(3, probe.getFailureThreshold().intValue());
        assertEquals(1, probe.getSuccessThreshold().intValue());
    }

    @Test
    public void getTCPWithHTTPURLAndWithoutPort() {
        //ProbeConfig with TCP Action
        //without port and url with http request
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("http://www.healthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertNull(probe.getTcpSocket());
        assertNull(probe.getExec());
        assertEquals(5,probe.getInitialDelaySeconds().intValue());
        assertEquals(5,probe.getTimeoutSeconds().intValue());
        assertEquals("www.healthcheck.com",probe.getHttpGet().getHost());
        assertNull(probe.getHttpGet().getHttpHeaders());
        assertEquals("/healthz",probe.getHttpGet().getPath());
        assertEquals(8080,probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("HTTP",probe.getHttpGet().getScheme());
    }

    @Test
    public void getTCPProbeWithTCPURLTest() {
        //ProbeConfig with TCP Action
        //without port and url with tcp request
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("tcp://www.healthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        //assertion
        assertNull(probe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTCPProbeWithInvalidURLTest() {
        //ProbeConfig with TCP Action
        //withInvalidUrl
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("healthcheck.com:8080/healthz")
                .tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
    }

    @Test
    public void testHttpGetProbeWithLocalhostInUrl() {
        // Given
        probeConfig = ProbeConfig.builder()
                .getUrl("http://:8080/healthz")
                .build();

        // When
        probe = probeHandler.getProbe(probeConfig);

        // Then
        assertThat(probe)
                .isNotNull()
                .hasFieldOrPropertyWithValue("httpGet.host", "")
                .hasFieldOrPropertyWithValue("httpGet.scheme", "HTTP")
                .hasFieldOrPropertyWithValue("httpGet.port", new IntOrString(8080));
    }

    @Test
    public void testHttpGetProbeWithCustomHeaders() {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "MyUserAgent");
        probeConfig = ProbeConfig.builder()
                .getUrl("https://www.example.com:8080/healthz")
                .httpHeaders(headers)
                .build();

        // When
        probe = probeHandler.getProbe(probeConfig);

        // Then
        assertThat(probe)
                .isNotNull()
                .hasFieldOrPropertyWithValue("httpGet.host", "www.example.com")
                .hasFieldOrPropertyWithValue("httpGet.port", new IntOrString(8080))
                .hasFieldOrPropertyWithValue("httpGet.scheme", "HTTPS")
                .satisfies(p -> assertThat(p).extracting("httpGet.httpHeaders").asList().element(0)
                       .hasFieldOrPropertyWithValue("name", "Accept")
                       .hasFieldOrPropertyWithValue("value", "application/json"))
                .satisfies(p -> assertThat(p).extracting("httpGet.httpHeaders").asList().element(1)
                       .hasFieldOrPropertyWithValue("name", "User-Agent")
                       .hasFieldOrPropertyWithValue("value", "MyUserAgent"));
    }
}
