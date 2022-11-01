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

import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.config.resource.ProbeConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ProbeHandlerTest {
    Probe probe;
    ProbeHandler probeHandler = new ProbeHandler();

    ProbeConfig probeConfig;

    @Test
    void getProbeEmptyTest() {
        //EmptyProbeConfig

        probeConfig = null;

        probe = probeHandler.getProbe(probeConfig);

        assertThat(probe).isNull();
    }

    @Test
    void getProbeNullTest() {
        //ProbeConfig without any action

        probeConfig = ProbeConfig.builder().build();

        probe = probeHandler.getProbe(probeConfig);

        assertThat(probe).isNull();
    }

    @Test
    void getHTTPProbeWithHTTPURLTest() {

        //ProbeConfig with HTTPGet Action
        //withUrl
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("http://www.healthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .hasFieldOrPropertyWithValue("exec", null)
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getHttpGet)
            .hasFieldOrPropertyWithValue("host", "www.healthcheck.com")
            .hasFieldOrPropertyWithValue("httpHeaders", null)
            .hasFieldOrPropertyWithValue("path", "/healthz")
            .hasFieldOrPropertyWithValue("port.intVal", 8080)
            .hasFieldOrPropertyWithValue("scheme", "HTTP");
    }

    @Test
    void getHTTPProbeWithInvalidURLTest() {
        // Given
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("httphealthcheck.com:8080/healthz")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
            .isThrownBy(() -> probeHandler.getProbe(probeConfig))
            .withMessageContaining("Invalid URL ");
    }

    @Test
    void getExecProbeWithEmptyExecTest() {
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
    void getExecProbeWithExecTest() {
        //ProbeConfig with Exec Action
        //withExec
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).exec("cat /tmp/probe")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .hasFieldOrPropertyWithValue("httpGet", null)
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getExec).isNotNull()
            .extracting(ExecAction::getCommand).asList()
            .hasSize(2)
            .containsExactly("cat", "/tmp/probe");
    }

    @Test
    void getExecProbeWithInvalidExecTest() {
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
    void getTCPProbeWithoutURLTest() {
        //ProbeConfig with TCP Action
        //withno url, only port
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet", null)
            .hasFieldOrPropertyWithValue("exec", null)
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .extracting(Probe::getTcpSocket).isNotNull()
            .hasFieldOrPropertyWithValue("host", null)
            .hasFieldOrPropertyWithValue("port.intVal", 80);
    }

    @Test
    void getTCPProbeWithHTTPURLAndPortTest() {
        //ProbeConfig with TCP Action
        //withport and url but with http request
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("http://www.healthcheck.com:8080/healthz").tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .hasFieldOrPropertyWithValue("exec", null)
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .extracting(Probe::getHttpGet).isNotNull()
            .hasFieldOrPropertyWithValue("host", "www.healthcheck.com")
            .hasFieldOrPropertyWithValue("httpHeaders", null)
            .hasFieldOrPropertyWithValue("path", "/healthz")
            .hasFieldOrPropertyWithValue("scheme", "HTTP")
            .hasFieldOrPropertyWithValue("port.intVal", 8080);
    }

    @Test
    void getTCPProbeWithNonHTTPURLTest() {
        //ProbeConfig with TCP Action
        //withport and url but with other request and port as int
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .failureThreshold(3).successThreshold(1)
                .getUrl("tcp://www.healthcheck.com:8080/healthz").tcpPort("80")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet", null)
            .hasFieldOrPropertyWithValue("exec", null)
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .hasFieldOrPropertyWithValue("failureThreshold", 3)
            .hasFieldOrPropertyWithValue("successThreshold", 1)
            .extracting(Probe::getTcpSocket).isNotNull()
            .hasFieldOrPropertyWithValue("port.intVal", 80)
            .hasFieldOrPropertyWithValue("host", "www.healthcheck.com");
    }

    @Test
    void getTCPProbeWithNonHTTPURLAndStringPortTest() {
        //ProbeConfig with TCP Action
        //withport and url but with other request and port as string
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("tcp://www.healthcheck.com:8080/healthz").tcpPort("httpPort")
                .successThreshold(1)
                .failureThreshold(3)
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet", null)
            .hasFieldOrPropertyWithValue("exec", null)
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .hasFieldOrPropertyWithValue("failureThreshold", 3)
            .hasFieldOrPropertyWithValue("successThreshold", 1)
            .extracting(Probe::getTcpSocket).isNotNull()
            .hasFieldOrPropertyWithValue("port.strVal", "httpPort")
            .hasFieldOrPropertyWithValue("host", "www.healthcheck.com");
    }

    @Test
    void getTCPWithHTTPURLAndWithoutPort() {
        //ProbeConfig with TCP Action
        //without port and url with http request
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5)
                .getUrl("http://www.healthcheck.com:8080/healthz")
                .build();

        probe = probeHandler.getProbe(probeConfig);
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .hasFieldOrPropertyWithValue("exec", null)
            .hasFieldOrPropertyWithValue("initialDelaySeconds", 5)
            .hasFieldOrPropertyWithValue("timeoutSeconds", 5)
            .extracting(Probe::getHttpGet).isNotNull()
            .hasFieldOrPropertyWithValue("httpHeaders", null)
            .hasFieldOrPropertyWithValue("host", "www.healthcheck.com")
            .hasFieldOrPropertyWithValue("path", "/healthz")
            .hasFieldOrPropertyWithValue("port.intVal", 8080)
            .hasFieldOrPropertyWithValue("scheme", "HTTP");
    }

    @Test
    void getTCPProbeWithInvalidURLTest() {
        //ProbeConfig with TCP Action
        //withInvalidUrl
        probeConfig = ProbeConfig.builder()
                .initialDelaySeconds(5).timeoutSeconds(5).getUrl("healthcheck.com:8080/healthz")
                .tcpPort("80")
                .build();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> probeHandler.getProbe(probeConfig))
            .withMessageContaining("Invalid URL ");
    }

    @Test
    void httpGetProbeWithLocalhostInUrl() {
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
    void testHttpGetProbeWithCustomHeaders() {
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
        assertThat(probe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet.host", "www.example.com")
            .hasFieldOrPropertyWithValue("httpGet.port", new IntOrString(8080))
            .hasFieldOrPropertyWithValue("httpGet.scheme", "HTTPS")
            .satisfies(p -> assertThat(p).extracting("httpGet.httpHeaders").asList().element(0)
                .hasFieldOrPropertyWithValue("name", "Accept")
                .hasFieldOrPropertyWithValue("value", "application/json")
            )
            .satisfies(p -> assertThat(p).extracting("httpGet.httpHeaders").asList().element(1)
                .hasFieldOrPropertyWithValue("name", "User-Agent")
                .hasFieldOrPropertyWithValue("value", "MyUserAgent")
            );
    }
}
