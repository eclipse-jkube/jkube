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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JkubeProjectPluginTest {
    private List<String> projectPluginsAsStr = Arrays.asList("org.springframework.boot,spring-boot-maven-plugin,null,null", "org.eclipse.jkube,k8s-maven-plugin,0.1.0,<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<configuration>\n" +
            "  <resources>\n" +
            "    <labels>\n" +
            "      <all>\n" +
            "        <testProject>spring-boot-sample</testProject>\n" +
            "      </all>\n" +
            "    </labels>\n" +
            "  </resources>\n" +
            "  <generator>\n" +
            "    <includes>\n" +
            "      <include>spring-boot</include>\n" +
            "    </includes>\n" +
            "    <config>\n" +
            "      <spring-boot>\n" +
            "        <color>always</color>\n" +
            "      </spring-boot>\n" +
            "    </config>\n" +
            "  </generator>\n" +
            "  <enricher>\n" +
            "    <excludes>\n" +
            "      <exclude>jkube-expose</exclude>\n" +
            "    </excludes>\n" +
            "    <config>\n" +
            "      <jkube-service>\n" +
            "        <type>NodePort</type>\n" +
            "      </jkube-service>\n" +
            "    </config>\n" +
            "  </enricher>\n" +
            "</configuration>,resource|build|helm");

    @Test
    public void testStringToPluginParsing() {
        JkubeProjectPlugin projectPlugin = JkubeProjectPlugin.fromString(projectPluginsAsStr.get(0));
        assertSpringBootPlugin(projectPlugin);

        projectPlugin = JkubeProjectPlugin.fromString(projectPluginsAsStr.get(1));
        assertEclipseJkubePlugin(projectPlugin);
    }

    @Test
    public void testStringToPluginListParsing() {
        List<JkubeProjectPlugin> projectPluginList = JkubeProjectPlugin.listFromStringPlugins(projectPluginsAsStr);
        assertEquals(2, projectPluginList.size());
        assertSpringBootPlugin(projectPluginList.get(0));
        assertEclipseJkubePlugin(projectPluginList.get(1));
    }

    public void assertSpringBootPlugin(JkubeProjectPlugin projectPlugin) {
        assertEquals("org.springframework.boot", projectPlugin.getGroupId());
        assertEquals("spring-boot-maven-plugin", projectPlugin.getArtifactId());
    }

    public void assertEclipseJkubePlugin(JkubeProjectPlugin projectPlugin) {
        assertEquals("org.eclipse.jkube", projectPlugin.getGroupId());
        assertEquals("k8s-maven-plugin", projectPlugin.getArtifactId());
        assertEquals("0.1.0", projectPlugin.getVersion());
        assertNotNull(projectPlugin.getConfiguration());
        assertEquals(3, projectPlugin.getExecutions().size());
        assertEquals(Arrays.asList("resource", "build", "helm"), projectPlugin.getExecutions());
    }
}
