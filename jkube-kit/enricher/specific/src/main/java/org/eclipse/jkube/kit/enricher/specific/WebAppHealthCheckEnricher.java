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
package org.eclipse.jkube.kit.enricher.specific;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.apache.commons.lang3.StringUtils;

public class WebAppHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public WebAppHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-webapp");
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        SCHEME("scheme", "HTTP"),
        PORT("port", "8080"),
        PATH("path", ""),
        INITIAL_READINESS_DELAY("initialReadinessDelay", "10"),
        INITIAL_LIVENESS_DELAY("initialLivenessDelay", "180");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    @Override
    protected Probe getLivenessProbe() {
        return getProbe(false);
    }

    @Override
    protected Probe getReadinessProbe() {
        return getProbe(true);
    }

    private boolean isApplicable() {
        return getContext().hasPlugin("org.apache.maven.plugins", "maven-war-plugin")
            && StringUtils.isNotEmpty(getConfig(Config.PATH));
    }

    private Probe getProbe(boolean readiness) {
        if (!isApplicable()) {
            return null;
        }

        SummaryUtil.addToEnrichers(getName());
        Integer port = getPort();
        String scheme = getScheme().toUpperCase();
        String path = getPath();

        int delay = readiness ? getInitialReadinessDelay() : getInitialLivenessDelay();

        return new ProbeBuilder().
            withNewHttpGet().withNewPort(port).withPath(path).withScheme(scheme).endHttpGet().
            withInitialDelaySeconds(delay).build();

    }

    private int getInitialReadinessDelay() {
        return Configs.asInt(getConfig(Config.INITIAL_READINESS_DELAY));
    }

    private int getInitialLivenessDelay() {
        return  Configs.asInt(getConfig(Config.INITIAL_LIVENESS_DELAY));
    }

    protected String getScheme() {
        return getConfig(Config.SCHEME);
    }

    protected int getPort() {
        return Configs.asInt(getConfig(Config.PORT));
    }

    protected String getPath() {
        return getConfig(Config.PATH);
    }
}
