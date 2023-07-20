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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.ServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ServiceHandlerTest {

    private final ServiceHandler serviceHandler = new ServiceHandler();

    //created services for output
    private List<Service> services;

    //created ports for input
    private List<ServiceConfig.Port> ports;

    //created serviceConfigs for input
    private List<ServiceConfig> serviceConfigs;
    private ServiceConfig.Port port;
    private ServiceConfig serviceconfig;

    @BeforeEach
    void setUp() {
        services = new ArrayList<>();
        ports = new ArrayList<>();
        serviceConfigs = new ArrayList<>();
    }

    @Test
    void getServices_withPortsAndWithoutHeadless() {
        //first scenario
        //*adding a basic port case with all values (with ports, without headless)
        port = ServiceConfig.Port.builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services).isNotNull()
            .first()
            .satisfies(s -> assertThat(s.getSpec())
                .hasFieldOrPropertyWithValue("type", "NodePort")
                .extracting(ServiceSpec::getPorts).asList()
                .first()
                .hasFieldOrPropertyWithValue("protocol", "TCP")
                .hasFieldOrPropertyWithValue("name", "port-test")
                .hasFieldOrPropertyWithValue("nodePort", 50)
                .hasFieldOrPropertyWithValue("targetPort.intVal", 80)
                .hasFieldOrPropertyWithValue("port", 8080)
            )
            .satisfies(s -> assertThat(s.getMetadata())
                .hasFieldOrPropertyWithValue("name", "service-test")
                .extracting(ObjectMeta::getLabels)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("expose", "true")
            );
    }

    @Test
    void getServices_withoutPort_shouldBeEmpty() {
        //second scenario
        //*create a service without ports
        serviceconfig = ServiceConfig.builder()
                .name("service-test").expose(true).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services).isEmpty();
    }

    @Test
    void getServices_withoutPortAndWithHeadless() {
        //third scenario
        //*create a service without ports and with headless service
        serviceconfig = ServiceConfig.builder()
                .name("service-test").expose(true).type("NodePort").headless(true).build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services).isNotEmpty()
            .first()
            .satisfies(s -> assertThat(s.getSpec())
                .hasFieldOrPropertyWithValue("type", "NodePort")
                .hasFieldOrPropertyWithValue("clusterIP", "None")
            )
            .satisfies(s -> assertThat(s.getMetadata())
                .hasFieldOrPropertyWithValue("name", "service-test")
                .extracting(ObjectMeta::getLabels)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("expose", "true")
            );
    }

    @Test
    void getServices_withPortAndHeadless() {
        port = ServiceConfig.Port.builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);
        //fourth scenario
        //*create a service with ports and with headless service
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).type("NodePort").headless(true).build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services).first()
            .satisfies(s -> assertThat(s.getSpec())
                .hasFieldOrPropertyWithValue("type", "NodePort")
                .hasFieldOrPropertyWithValue("clusterIP", "None")
                .extracting(ServiceSpec::getPorts).asList()
                .first()
                .hasFieldOrPropertyWithValue("protocol", "TCP")
                .hasFieldOrPropertyWithValue("name", "port-test")
                .hasFieldOrPropertyWithValue("nodePort", 50)
                .hasFieldOrPropertyWithValue("targetPort.intVal", 80)
                .hasFieldOrPropertyWithValue("port", 8080)
            )
            .satisfies(s -> assertThat(s.getMetadata())
                .hasFieldOrPropertyWithValue("name", "service-test")
                .extracting(ObjectMeta::getLabels)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("expose", "true")
            );
    }

    @Test
    void getServices_withNullType_shouldBeNull() {
        port = ServiceConfig.Port.builder()
                .port(8080).targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);

        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services.get(0).getSpec().getType()).isNull();
    }

    @DisplayName("get services with protocol")
    @ParameterizedTest(name = "{index}: with ''{0}'' protocol, should return ''{1}'' protocol")
    @MethodSource("protocols")
    void getServices_withProtocol_shouldReturnApplicableProtocol(String protocol, String expectedProtocol) {
        port = ServiceConfig.Port.builder()
                .port(8080)
                .protocol(protocol)
                .targetPort(80)
                .nodePort(50)
                .name("port-test")
                .build();
        ports.add(port);

        serviceconfig = ServiceConfig.builder()
                .ports(ports)
                .name("service-test")
                .expose(true)
                .headless(false)
                .type("NodePort")
                .build();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services.get(0).getSpec().getPorts().get(0).getProtocol()).isEqualTo(expectedProtocol);
    }

    static Stream<Arguments> protocols() {
      return Stream.of(
          arguments("tcp", "TCP"),
          arguments("udp", "UDP"),
          arguments(null, "TCP"));
    }
}