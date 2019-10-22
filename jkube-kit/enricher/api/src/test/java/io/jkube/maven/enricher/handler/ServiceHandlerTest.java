/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.Service;
import io.jkube.kit.config.resource.ServiceConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        port = new ServiceConfig.Port.Builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);
        serviceconfig = new ServiceConfig.Builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);

        //Assertion
        assertNotNull(services);
        assertEquals("TCP", services.get(0).getSpec().getPorts().get(0).getProtocol());
        assertEquals("port-test", services.get(0).getSpec().getPorts().get(0).getName());
        assertEquals(50, services.get(0).getSpec().getPorts().get(0).getNodePort().intValue());
        assertEquals(80, services.get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal().intValue());
        assertEquals(8080, services.get(0).getSpec().getPorts().get(0).getPort().intValue());
        assertEquals("NodePort", services.get(0).getSpec().getType());

        assertTrue(services.get(0).getMetadata().getLabels().get("expose").equalsIgnoreCase("true"));
        assertTrue(services.get(0).getMetadata().getName().equalsIgnoreCase("service-test"));
    }

    @Test
    public void getServicesWithoutPortTest() {
        //second scenario
        //*create a service without ports
        serviceconfig = new ServiceConfig.Builder()
                .name("service-test").expose(true).type("NodePort").build();

        serviceConfigs.clear();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertTrue(services.isEmpty());
    }

    @Test
    public void getServicesWithHeadlessServiceTest() {
        //third scenario
        //*create a service without ports and with headless service
        serviceconfig = new ServiceConfig.Builder()
                .name("service-test").expose(true).type("NodePort").headless(true).build();

        serviceConfigs.clear();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);

        //Assertion
        assertFalse(services.isEmpty());
        assertEquals("NodePort",services.get(0).getSpec().getType());
        assertEquals("None",services.get(0).getSpec().getClusterIP());
        assertTrue(services.get(0).getMetadata().getLabels().get("expose").equalsIgnoreCase("true"));
        assertTrue(services.get(0).getMetadata().getName().equalsIgnoreCase("service-test"));
    }

    @Test
    public void getServicesBothPortAndHealessTest() {
        ports.clear();
        port = new ServiceConfig.Port.Builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.add(port);
        //fourth scenario
        //*create a service with ports and with headless service
        serviceconfig = new ServiceConfig.Builder()
                .ports(ports).name("service-test").expose(true).type("NodePort").headless(true).build();

        serviceConfigs.clear();
        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);

        //Assertion
        assertEquals("TCP",services.get(0).getSpec().getPorts().get(0).getProtocol());
        assertEquals("port-test",services.get(0).getSpec().getPorts().get(0).getName());
        assertEquals(50,services.get(0).getSpec().getPorts().get(0).getNodePort().intValue());
        assertEquals(80,services.get(0).getSpec().getPorts().get(0).getTargetPort().getIntVal().intValue());
        assertEquals(8080,services.get(0).getSpec().getPorts().get(0).getPort().intValue());
        assertEquals("NodePort",services.get(0).getSpec().getType());
        assertEquals("None",services.get(0).getSpec().getClusterIP());
        assertTrue(services.get(0).getMetadata().getLabels().get("expose").equalsIgnoreCase("true"));
        assertTrue(services.get(0).getMetadata().getName().equalsIgnoreCase("service-test"));
    }

    @Test
    public void getServicesWithTCPProtocolTest() {

        //checking protocol now
        //TCP
        port = new ServiceConfig.Port.Builder()
                .port(8080).protocol("tcp").targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);
        serviceConfigs.clear();
        serviceconfig = new ServiceConfig.Builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertEquals("TCP",services.get(0).getSpec().getPorts().get(0).getProtocol());
    }

    @Test
    public void getServicesWithUDPProtocolTest() {
        //UDP
        port = new ServiceConfig.Port.Builder()
                .port(8080).protocol("udp").targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);
        serviceconfig = new ServiceConfig.Builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.clear();
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertEquals("UDP",services.get(0).getSpec().getPorts().get(0).getProtocol());
    }

    @Test
    public void getServicesWithDefaultProtocolTest() {
        //DEFAULT
        port = new ServiceConfig.Port.Builder()
                .port(8080).targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);
        serviceconfig = new ServiceConfig.Builder()
                .ports(ports).name("service-test").expose(true).headless(false).type("NodePort").build();

        //creating serviceConfigs of service;
        serviceConfigs.clear();
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertEquals("TCP",services.get(0).getSpec().getPorts().get(0).getProtocol());
    }

    @Test
    public void getServicesWithNullProtocolTest() {
        //checking null type
        port = new ServiceConfig.Port.Builder()
                .port(8080).targetPort(80).nodePort(50).name("port-test").build();
        ports.clear();
        ports.add(port);

        serviceconfig = new ServiceConfig.Builder()
                .ports(ports).name("service-test").expose(true).headless(false).build();

        //creating serviceConfigs of service;
        serviceConfigs.clear();
        serviceConfigs.add(serviceconfig);

        //calling the getServices Method
        services = serviceHandler.getServices(serviceConfigs);
        assertNull(services.get(0).getSpec().getType());

    }
}