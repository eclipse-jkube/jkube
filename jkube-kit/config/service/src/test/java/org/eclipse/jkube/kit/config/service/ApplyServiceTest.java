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

package org.eclipse.jkube.kit.config.service;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.openshift.WebServerEventCollector;
import org.junit.Before;
import org.junit.Test;

import static java.net.HttpURLConnection.*;
import static org.junit.Assert.assertEquals;

public class ApplyServiceTest {

    @Mocked
    private KitLogger log;

    private OpenShiftMockServer mockServer = new OpenShiftMockServer(false);

    private ApplyService applyService;

    @Before
    public void setUp() {
        applyService = new ApplyService(mockServer.createOpenShiftClient(), log);
    }

    @Test
    public void testCreateRoute() throws Exception {
        Route route = buildRoute();

        WebServerEventCollector<OpenShiftMockServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();
        mockServer.expect().post()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes")
                .andReply(collector.record("new-route").andReturn(HTTP_CREATED, route))
                .once();

        applyService.apply(route, "route.yml");

        collector.assertEventsRecordedInOrder("get-route", "new-route");
    }

    @Test
    public void testUpdateRoute() throws Exception {
        Route oldRoute = buildRoute();
        Route newRoute = new RouteBuilder()
                .withNewMetadataLike(oldRoute.getMetadata())
                    .addToAnnotations("haproxy.router.openshift.io/balance", "roundrobin")
                .endMetadata()
                .withSpec(oldRoute.getSpec())
                .build();

        WebServerEventCollector<OpenShiftMockServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_OK, oldRoute))
                .always();
        mockServer.expect().patch()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("patch-route")
                        .andReturn(HTTP_OK, new RouteBuilder()
                                .withMetadata(newRoute.getMetadata())
                                .withSpec(oldRoute.getSpec())
                                .build()))
                .once();

        applyService.apply(newRoute, "route.yml");

        collector.assertEventsRecordedInOrder("get-route", "patch-route");
    }

    @Test
    public void testCreateRouteInServiceOnlyMode() throws Exception {
        Route route = buildRoute();

        WebServerEventCollector<OpenShiftMockServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();

        applyService.setServicesOnlyMode(true);
        applyService.apply(route, "route.yml");

        collector.assertEventsNotRecorded("get-route");
        assertEquals(1, mockServer.getRequestCount());
    }

    @Test
    public void testCreateRouteNotAllowed() throws Exception {
        Route route = buildRoute();

        WebServerEventCollector<OpenShiftMockServer> collector = new WebServerEventCollector<>(mockServer);
        mockServer.expect().get()
                .withPath("/apis/route.openshift.io/v1/namespaces/default/routes/route")
                .andReply(collector.record("get-route").andReturn(HTTP_NOT_FOUND, ""))
                .always();

        applyService.setAllowCreate(false);
        applyService.apply(route, "route.yml");

        collector.assertEventsRecordedInOrder("get-route");
        assertEquals(2, mockServer.getRequestCount());
    }

    private Route buildRoute() {
        return new RouteBuilder()
                .withNewMetadata()
                    .withNamespace("default")
                    .withName("route")
                .endMetadata()
                .withNewSpec()
                    .withHost("www.example.com")
                    .withNewTo()
                        .withKind("Service")
                        .withName("frontend")
                    .endTo()
                .endSpec()
                .build();
    }

}