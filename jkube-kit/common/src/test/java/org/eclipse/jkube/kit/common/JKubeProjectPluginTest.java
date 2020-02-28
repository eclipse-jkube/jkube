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
package org.eclipse.jkube.kit.common;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JKubeProjectPluginTest {
    private static List<AbstractMap.SimpleEntry<String, Map<String, Object>>> projectPluginsAsStr;

    @Before
    public void init() {
        projectPluginsAsStr = new ArrayList<>();
        projectPluginsAsStr.add(new AbstractMap.SimpleEntry<>("org.springframework.boot,spring-boot-maven-plugin,null,null", Collections.emptyMap()));
        projectPluginsAsStr.add(new AbstractMap.SimpleEntry<>("org.eclipse.jkube,k8s-maven-plugin,0.1.0,resource|build|helm", Collections.emptyMap()));
    }

    @Test
    public void testStringToPluginParsing() {
        JKubeProjectPlugin projectPlugin = JKubeProjectPlugin.fromString(projectPluginsAsStr.get(0).getKey(), projectPluginsAsStr.get(0).getValue());
        assertSpringBootPlugin(projectPlugin);

        projectPlugin = JKubeProjectPlugin.fromString(projectPluginsAsStr.get(1).getKey(), projectPluginsAsStr.get(1).getValue());
        assertEclipseJKubePlugin(projectPlugin);
    }

    @Test
    public void testStringToPluginListParsing() {
        List<JKubeProjectPlugin> projectPluginList = JKubeProjectPlugin.listFromStringPlugins(projectPluginsAsStr);
        assertEquals(2, projectPluginList.size());
        assertSpringBootPlugin(projectPluginList.get(0));
        assertEclipseJKubePlugin(projectPluginList.get(1));
    }

    public void assertSpringBootPlugin(JKubeProjectPlugin projectPlugin) {
        assertEquals("org.springframework.boot", projectPlugin.getGroupId());
        assertEquals("spring-boot-maven-plugin", projectPlugin.getArtifactId());
    }

    public void assertEclipseJKubePlugin(JKubeProjectPlugin projectPlugin) {
        assertEquals("org.eclipse.jkube", projectPlugin.getGroupId());
        assertEquals("k8s-maven-plugin", projectPlugin.getArtifactId());
        assertEquals("0.1.0", projectPlugin.getVersion());
        assertNotNull(projectPlugin.getConfiguration());
        assertEquals(3, projectPlugin.getExecutions().size());
        assertEquals(Arrays.asList("resource", "build", "helm"), projectPlugin.getExecutions());
    }
}
