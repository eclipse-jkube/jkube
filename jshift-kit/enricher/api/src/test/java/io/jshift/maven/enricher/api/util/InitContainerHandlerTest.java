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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.jshift.kit.common.KitLogger;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 * @since 07/02/17
 */

public class InitContainerHandlerTest {

    @Mocked
    KitLogger log;

    InitContainerHandler handler;

    @Before
    public void setUp() {
        handler = new InitContainerHandler(log);
    }

    @Test
    public void simple() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder();
        assertFalse(handler.hasInitContainer(builder, "blub"));
        Container initContainer = createInitContainer("blub", "foo/blub");
        handler.appendInitContainer(builder, initContainer);
        assertTrue(handler.hasInitContainer(builder, "blub"));
        verifyBuilder(builder, Arrays.asList(initContainer));
    }

    @Test
    public void append() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla");
        assertFalse(handler.hasInitContainer(builder, "blub"));
        Container initContainer = createInitContainer("blub", "foo/blub");
        handler.appendInitContainer(builder, initContainer);
        assertTrue(handler.hasInitContainer(builder, "blub"));
        verifyBuilder(builder, Arrays.asList(createInitContainer("bla", "foo/bla"), initContainer));
    }

    @Test
    public void removeAll() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla");
        assertTrue(handler.hasInitContainer(builder, "bla"));
        handler.removeInitContainer(builder, "bla");
        assertFalse(handler.hasInitContainer(builder, "bla"));
        verifyBuilder(builder, null);
    }

    @Test
    public void removeOne() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla", "blub", "foo/blub");
        assertTrue(handler.hasInitContainer(builder, "bla"));
        assertTrue(handler.hasInitContainer(builder, "blub"));
        handler.removeInitContainer(builder, "bla");
        assertFalse(handler.hasInitContainer(builder, "bla"));
        assertTrue(handler.hasInitContainer(builder, "blub"));
        verifyBuilder(builder, Arrays.asList(createInitContainer("blub", "foo/blub")));
    }

    @Test
    public void existingSame() {
        new Expectations() {{
            log.warn(anyString, withSubstring("blub"));
        }};

        PodTemplateSpecBuilder builder = getPodTemplateBuilder("blub", "foo/blub");
        assertTrue(handler.hasInitContainer(builder, "blub"));
        Container initContainer = createInitContainer("blub", "foo/blub");
        handler.appendInitContainer(builder, initContainer);
        assertTrue(handler.hasInitContainer(builder, "blub"));
        verifyBuilder(builder, Arrays.asList(initContainer));
    }

    @Test
    public void existingDifferent() {
        try {
            PodTemplateSpecBuilder builder = getPodTemplateBuilder("blub", "foo/bla");
            assertTrue(handler.hasInitContainer(builder, "blub"));
            Container initContainer = createInitContainer("blub", "foo/blub");
            handler.appendInitContainer(builder, initContainer);
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("blub"));
        }
    }

    private void verifyBuilder(PodTemplateSpecBuilder builder, List<Container> initContainers) {
        PodTemplateSpec spec = builder.build();
        List<Container> initContainersInSpec = spec.getSpec().getInitContainers();
        if (initContainersInSpec.size() == 0) {
            assertNull(initContainers);
        } else {
            assertEquals(initContainersInSpec.size(), initContainers.size());
            for (int i = 0; i < initContainers.size(); i++) {
                assertEquals(initContainersInSpec.get(i), initContainers.get(i));
            }
        }
    }

    private PodTemplateSpecBuilder getPodTemplateBuilder(String ... definitions) {
        PodTemplateSpecBuilder ret = new PodTemplateSpecBuilder();
        ret.withNewMetadata().withName("test-pod-templateSpec").endMetadata().withNewSpec().withInitContainers(getInitContainerList(definitions)).endSpec();
        return ret;
    }

    private List<Container> getInitContainerList(String ... definitions) {
        List<Container> ret = new ArrayList<>();
        for (int i = 0; i < definitions.length; i += 2 ) {
            ret.add(createInitContainer(definitions[i], definitions[i+1]));
        }
        return ret;
    }

    private Container createInitContainer(String name, String image) {
        Container initContainer = new ContainerBuilder()
                .withName(name)
                .withImage(image)
                .build();
        return initContainer;
    }
}
