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

public class JkubeProjectDependencyTest {
    private List<String> dependencyStr = Arrays.asList("org.eclipse.jkube,foo-dependency,0.1.0,jar,compile,/tmp/foo-dependency.jar",
            "org.eclipse.jkube,bar-dependency,0.1.0,jar,compile,/tmp/bar-dependency.jar",
            "org.jolokia,jolokia-core,1.6.2,jar,compile,/tmp/jolokia-core.jar");

    @Test
    public void testDependencyStringParsing() {
        JkubeProjectDependency projectDependency = JkubeProjectDependency.fromString(dependencyStr.get(0));
        assertDependency(projectDependency, "org.eclipse.jkube", "foo-dependency", "0.1.0");

        projectDependency = JkubeProjectDependency.fromString(dependencyStr.get(1));
        assertDependency(projectDependency, "org.eclipse.jkube", "bar-dependency", "0.1.0");

        projectDependency = JkubeProjectDependency.fromString(dependencyStr.get(2));
        assertDependency(projectDependency, "org.jolokia", "jolokia-core", "1.6.2");
    }

    @Test
    public void testDependencyStringListParsing() {
        List<JkubeProjectDependency> projectDependencyList = JkubeProjectDependency.listFromStringDependencies(dependencyStr);

        assertEquals(3, projectDependencyList.size());
        assertDependency(projectDependencyList.get(0), "org.eclipse.jkube", "foo-dependency", "0.1.0");
        assertDependency(projectDependencyList.get(1), "org.eclipse.jkube", "bar-dependency", "0.1.0");
        assertDependency(projectDependencyList.get(2), "org.jolokia", "jolokia-core", "1.6.2");
    }

    private void assertDependency(JkubeProjectDependency dependency, String groupId, String artifactId, String version) {
        assertEquals(groupId, dependency.getGroupId());
        assertEquals(artifactId, dependency.getArtifactId());
        assertEquals(version, dependency.getVersion());
    }
}
