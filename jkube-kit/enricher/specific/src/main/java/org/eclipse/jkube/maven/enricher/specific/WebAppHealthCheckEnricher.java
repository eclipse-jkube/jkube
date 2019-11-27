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
package org.eclipse.jkube.maven.enricher.specific;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;
import org.apache.commons.lang3.StringUtils;

public class WebAppHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public WebAppHealthCheckEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-webapp");
    }

    // Available configuration keys
    private enum Config implements Configs.Key {

        scheme {{
            d = "HTTP";
        }},
        port {{
            d = "8080";
        }},
        path {{
            d = "";
        }},
        initialReadinessDelay {{
            d = "10";
        }},
        initialLivenessDelay {{
            d = "180";
        }};

        protected String d;

        public String def() {
            return d;
        }
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
        return getContext()
            .hasPlugin("org.apache.maven.plugins", "maven-war-plugin") &&
            StringUtils.isNotEmpty(Configs.asString(getConfig(Config.path)));
    }

    private Probe getProbe(boolean readiness) {
        if (!isApplicable()) {
            return null;
        }

        Integer port = getPort();
        String scheme = getScheme().toUpperCase();
        String path = getPath();

        int delay = readiness ? getInitialReadinessDelay() : getInitialLivenessDelay();

        return new ProbeBuilder().
            withNewHttpGet().withNewPort(port).withPath(path).withScheme(scheme).endHttpGet().
            withInitialDelaySeconds(delay).build();

    }

    private int getInitialReadinessDelay() {
        return Configs.asInt(getConfig(Config.initialReadinessDelay));
    }

    private int getInitialLivenessDelay() {
        return  Configs.asInt(getConfig(Config.initialLivenessDelay));
    }

    protected String getScheme() {
        return Configs.asString(getConfig(Config.scheme));
    }

    protected int getPort() {
        return Configs.asInt(getConfig(Config.port));
    }

    protected String getPath() {
        return Configs.asString(getConfig(Config.path));
    }
}
