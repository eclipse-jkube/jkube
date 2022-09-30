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

import io.fabric8.kubernetes.api.model.Service;
import org.eclipse.jkube.kit.config.resource.ServiceConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceHandlerTest {

    //create a serviceHandlerObject
    private final ServiceHandler serviceHandler = new ServiceHandler();

    //created services for output
    List<Service> services = new ArrayList<>();

    //created ports for input
    List<ServiceConfig.Port> ports = new ArrayList<>();

    //created serviceConfigs for input
    List<ServiceConfig> serviceConfigs = new ArrayList<>();

    ServiceConfig.Port port;

    ServiceConfig serviceconfig;

    @Test
    public void getServicesTest() {

        //first scenario
        //*adding a basic port case with all values (with ports, without headless)
        port = ServiceConfig.Port.builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);

        //Assertion
        assertThat(services).isNotNull();
        assertThat(services.get(0).getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
        assertThat(services.get(0).getSpec().getPorts().get(0).getName()).isEqualTo("port-test");
        assertThat(services.get(0).getSpec().getPorts().get(0).getNodePort().intValue()).isEqualTo(50);
        assertThat(services.get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal().intValue()).isEqualTo(80);
        assertThat(services.get(0).getSpec().getPorts().get(0).getPort().intValue()).isEqualTo(8080);
        assertThat(services.get(0).getSpec().getType()).isEqualTo("NodePort");

        assertThat(services.get(0).getMetadata().getLabels().get("expose")).isEqualToIgnoringCase("true");
        assertThat(services.get(0).getMetadata().getName()).isEqualToIgnoringCase("service-test");
    }

    @Test
    public void getServicesWithoutPortTest() {
        //second scenario
        //*create a service without ports
        serviceconfig = ServiceConfig.builder()
                .name("service-test").expose(true).type("NodePort").build();

        serviceConfigs.clear();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services).isEmpty();
    }

    @Test
    public void getServicesWithHeadlessServiceTest() {
        //third scenario
        //*create a service without ports and with headless service
        serviceconfig = ServiceConfig.builder()
                .name("service-test").expose(true).type("NodePort").headless(true).build();

        serviceConfigs.clear();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);

        //Assertion
        assertThat(services).isNotEmpty();
        assertThat(services.get(0).getSpec().getType()).isEqualTo("NodePort");
        assertThat(services.get(0).getSpec().getClusterIP()).isEqualTo("None");
        assertThat(services.get(0).getMetadata().getLabels().get("expose")).isEqualToIgnoringCase("true");
        assertThat(services.get(0).getMetadata().getName()).isEqualToIgnoringCase("service-test");
    }

    @Test
    public void getServicesBothPortAndHealessTest() {
        ports.clear();
        port = ServiceConfig.Port.builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);
        //fourth scenario
        //*create a service with ports and with headless service
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).type("NodePort").headless(true).build();

        serviceConfigs.clear();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);

        //Assertion
        assertThat(services.get(0).getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
        assertThat(services.get(0).getSpec().getPorts().get(0).getName()).isEqualTo("port-test");
        assertThat(services.get(0).getSpec().getPorts().get(0).getNodePort().intValue()).isEqualTo(50);
        assertThat(services.get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal().intValue()).isEqualTo(80);
        assertThat(services.get(0).getSpec().getPorts().get(0).getPort().intValue()).isEqualTo(8080);
        assertThat(services.get(0).getSpec().getType()).isEqualTo("NodePort");
        assertThat(services.get(0).getSpec().getClusterIP()).isEqualTo("None");
        assertThat(services.get(0).getMetadata().getLabels().get("expose")).isEqualToIgnoringCase("true");
        assertThat(services.get(0).getMetadata().getName()).isEqualToIgnoringCase("service-test");
    }

    @Test
    public void getServicesWithTCPProtocolTest() {

        //checking protocol now
        //TCP
        port = ServiceConfig.Port.builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);
        serviceConfigs.clear();
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services.get(0).getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    @Test
    public void getServicesWithUDPProtocolTest() {
        //UDP
        port = ServiceConfig.Port.builder()
                .port(8080).protocol("udp").targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.clear();
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services.get(0).getSpec().getPorts().get(0).getProtocol()).isEqualTo("UDP");
    }

    @Test
    public void getServicesWithDefaultProtocolTest() {
        //DEFAULT
        port = ServiceConfig.Port.builder()
                .port(8080).targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);
        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.clear();
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services.get(0).getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    @Test
    public void getServicesWithNullProtocolTest() {
        //checking null type
        port = ServiceConfig.Port.builder()
                .port(8080).targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);

        serviceconfig = ServiceConfig.builder()
                .ports(ports).name("service-test").expose(true).headless(false).build();

        //creating serviceConfigs of service;
        serviceConfigs.clear();
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertThat(services.get(0).getSpec().getType()).isNull();

    }
}