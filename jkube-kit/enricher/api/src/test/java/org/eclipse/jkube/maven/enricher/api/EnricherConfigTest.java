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
package org.eclipse.jkube.maven.enricher.api;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 03/06/16
 */
public class EnricherConfigTest {

    private enum Config implements Configs.Key {
        type;

        public String def() { return null; }
    }

    @Test
    public void simple() throws Exception {
        Map<String,TreeMap> configMap = new HashMap<>();
        TreeMap map = new TreeMap();
        map.put("type","LoadBalancer");
        configMap.put("default.service", map);
        EnricherConfig config = new EnricherConfig(
            "default.service",
            new Configuration.Builder().processorConfig(new ProcessorConfig(null, null, configMap)).build());
        assertEquals("LoadBalancer",config.get(Config.type));
    }
}
