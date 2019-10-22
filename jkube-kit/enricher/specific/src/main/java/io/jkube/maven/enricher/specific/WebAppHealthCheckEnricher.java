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
package io.jkube.maven.enricher.specific;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.jkube.kit.common.Configs;
import io.jkube.maven.enricher.api.MavenEnricherContext;
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
