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
package org.eclipse.jkube.maven.enricher.api.util;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenConfigurationExtractorTest {


    @Test
    public void should_parse_simple_types() {

        // Given
        final Plugin fakePlugin = createFakePlugin("<a>a</a><b>b</b>");

        // When
        final Map<String, Object> config = MavenConfigurationExtractor.extract((Xpp3Dom) fakePlugin.getConfiguration());

        // Then
        assertThat(config)
            .containsEntry("a", "a")
            .containsEntry("b", "b");

    }

    @Test
    public void should_parse_inner_objects() {

        // Given
        final Plugin fakePlugin = createFakePlugin("<a>"
            + "<b>b</b>"
            + "</a>");

        // When
        final Map<String, Object> config = MavenConfigurationExtractor.extract((Xpp3Dom) fakePlugin.getConfiguration());

        // Then
        final Map<String, Object> expected = new HashMap<>();
        expected.put("b", "b");
        assertThat(config)
            .containsEntry("a", expected);

    }

    @Test
    public void should_parse_deep_inner_objects() {

        // Given
        final Plugin fakePlugin = createFakePlugin("<a>"
            + "<b>"
            + "<c>"
            + "<d>"
            + "<e>e1</e>"
            + "</d>"
            + "</c>"
            + "</b>"
            + "</a>");

        // When
        final Map<String, Object> config = MavenConfigurationExtractor.extract((Xpp3Dom) fakePlugin.getConfiguration());

        // Then
        final Map<String, Object> e = new HashMap<>();
        e.put("e", "e1");
        final Map<String, Object> d = new HashMap<>();
        d.put("d", e);
        final Map<String, Object> c = new HashMap<>();
        c.put("c", d);
        final Map<String, Object> expected = new HashMap<>();
        expected.put("b", c);
        assertThat(config)
            .containsEntry("a", expected);

    }

    @Test
    public void should_parse_list_of_elements() {

        // Given
        final Plugin fakePlugin = createFakePlugin("<a>"
            + "<b>"
            + "<c>c1</c><c>c2</c>"
            + "</b>"
            + "</a>");

        // When
        final Map<String, Object> config = MavenConfigurationExtractor.extract((Xpp3Dom) fakePlugin.getConfiguration());

        // Then
        final Map<String, Object> expectedC = new HashMap<>();
        expectedC.put("c", Arrays.asList("c1", "c2"));
        final Map<String, Object> expected = new HashMap<>();
        expected.put("b", expectedC);

        assertThat(config)
            .containsEntry("a",expected);

    }

    @Test
    public void should_parse_list_of_mixed_elements() {

        // Given
        final Plugin fakePlugin = createFakePlugin("<a>"
            + "<b>"
            + "<c>c1</c><d>d1</d><c>c2</c>"
            + "</b>"
            + "</a>");

        // When
        final Map<String, Object> config = MavenConfigurationExtractor.extract((Xpp3Dom) fakePlugin.getConfiguration());

        // Then
        final Map<String, Object> expectedC = new HashMap<>();
        expectedC.put("c", Arrays.asList("c1", "c2"));
        expectedC.put("d", "d1");
        final Map<String, Object> expected = new HashMap<>();
        expected.put("b", expectedC);

        assertThat(config)
            .containsEntry("a",expected);

    }

    private Plugin createFakePlugin(String config) {
        Plugin plugin = new Plugin();
        plugin.setArtifactId("jkube-maven-plugin");
        plugin.setGroupId("org.eclipse.jkube");
        String content = "<configuration>"
            + config
            + "</configuration>";
        Xpp3Dom dom;
        try {
            dom = Xpp3DomBuilder.build(new StringReader(content));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        plugin.setConfiguration(dom);

        return plugin;
    }
}
