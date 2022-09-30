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

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.config.resource.ProbeConfig;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ProbeHandlerTest {
    Probe probe;
    ProbeHandler probeHandler = new ProbeHandler();

    ProbeConfig probeConfig;

    @Test
    public void getProbeEmptyTest() {
        //EmptyProbeConfig

        probeConfig = null;

        probe = probeHandler.getProbe(probeConfig);

        assertThat(probe).isNull();
    }

    @Test
    public void getProbeNullTest() {
        //ProbeConfig without any action

        probeConfig = ProbeConfig.builder().build();

        probe = probeHandler.getProbe(probeConfig);

        assertThat(probe).isNull();
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
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
        assertThat(probe.getHttpGet().getHost()).isEqualTo("www.healthcheck.com");
        assertThat(probe.getHttpGet().getHttpHeaders()).isNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/healthz");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getTcpSocket()).isNull();
    }

    @Test
    public void getHTTPProbeWithInvalidURLTest() {
        // Given
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("httphealthcheck.com:8080/healthz")
                .build();

        // When
        assertThatIllegalArgumentException()
                .isThrownBy(() -> probeHandler.getProbe(probeConfig)).withMessage("Invalid URL httphealthcheck.com:8080/healthz given for HTTP GET readiness check");
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
        assertThat(probe).isNull();
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
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
        assertThat(probe.getExec()).isNotNull();
        assertThat(probe.getExec().getCommand().size()).isEqualTo(2);
        assertThat(probe.getExec().getCommand()).hasToString("[cat, /tmp/probe]");
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket()).isNull();
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
        assertThat(probe).isNull();
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
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(80);
        assertThat(probe.getTcpSocket().getHost()).isNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
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
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
        assertThat(probe.getHttpGet().getHost()).isEqualTo("www.healthcheck.com");
        assertThat(probe.getHttpGet().getHttpHeaders()).isNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/healthz");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
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

        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getExec()).isNull();
        assertThat( probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(80);
        assertThat(probe.getTcpSocket().getHost()).isEqualTo("www.healthcheck.com");
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
        assertThat( probe.getFailureThreshold().intValue()).isEqualTo(3);
        assertThat( probe.getSuccessThreshold().intValue()).isEqualTo(1);
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
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo("httpPort");
        assertThat(probe.getTcpSocket().getHost()).isEqualTo("www.healthcheck.com");
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
        assertThat(probe.getFailureThreshold().intValue()).isEqualTo(3);
        assertThat(probe.getSuccessThreshold().intValue()).isEqualTo(1);
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
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(5);
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(5);
        assertThat(probe.getHttpGet().getHost()).isEqualTo("www.healthcheck.com");
        assertThat(probe.getHttpGet().getHttpHeaders()).isNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/healthz");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
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
