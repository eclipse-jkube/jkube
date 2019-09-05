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
package io.jshift.maven.enricher.api.util;

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
        plugin.setArtifactId("jshift-maven-plugin");
        plugin.setGroupId("io.jshift");
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
