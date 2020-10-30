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
package org.eclipse.jkube.quickstart.kit.enricher.istio;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.GatewayBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

public class IstioEnricher extends BaseEnricher {

    public IstioEnricher(JKubeEnricherContext enricherContext) {
        super(enricherContext, "istio-enricher");
    }

    // Available configuration keys
    private enum Config implements Configs.Config {
        // name of the gateway to create
        name;

        public String def() { return d; } protected String d;
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        log.info("Added dummy networking.istio.io/v1alpha3 Gateway");
        builder.addToItems(createGatewayBuilder());
        log.info("Exiting Istio Enricher");
    }

    private GatewayBuilder createGatewayBuilder() {
        return  new GatewayBuilder()
                .withNewMetadata()
                .withName(getGatewayName())
                .endMetadata()
                .withNewSpec()
                .addToSelector("app", "test-app")
                .addNewServer()
                .withNewPort()
                .withNumber(80)
                .withName("http")
                .withProtocol("HTTP")
                .endPort()
                .addNewHost("uk.bookinfo.com")
                .addNewHost("in.bookinfo.com")
                .withNewTls()
                .withHttpsRedirect(true)
                .endTls()
                .endServer()
                .endSpec();
    }

    private String getGatewayName() {
        return getConfig(Config.name);
    }
}
