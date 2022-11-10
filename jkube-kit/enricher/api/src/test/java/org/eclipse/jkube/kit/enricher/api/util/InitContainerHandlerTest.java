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
package org.eclipse.jkube.kit.enricher.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

/**
 * @author roland
 */

public class InitContainerHandlerTest {


    KitLogger log;

    InitContainerHandler handler;

    @Before
    public void setUp() {
        log = spy(new KitLogger.SilentLogger());
        handler = new InitContainerHandler(log);
    }

    @Test
    public void simple() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder();
        assertThat(handler.hasInitContainer(builder, "blub")).isFalse();
        Container initContainer = createInitContainer("blub", "foo/blub");
        handler.appendInitContainer(builder, initContainer);
        assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
        verifyBuilder(builder, Collections.singletonList(initContainer));
    }

    @Test
    public void append() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla");
        assertThat(handler.hasInitContainer(builder, "blub")).isFalse();
        Container initContainer = createInitContainer("blub", "foo/blub");
        handler.appendInitContainer(builder, initContainer);
        assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
        verifyBuilder(builder, Arrays.asList(createInitContainer("bla", "foo/bla"), initContainer));
    }

    @Test
    public void removeAll() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla");
        assertThat(handler.hasInitContainer(builder, "bla")).isTrue();
        handler.removeInitContainer(builder, "bla");
        assertThat(handler.hasInitContainer(builder, "bla")).isFalse();
        verifyBuilder(builder, null);
    }

    @Test
    public void removeOne() {
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla", "blub", "foo/blub");
        assertThat(handler.hasInitContainer(builder, "bla")).isTrue();
        assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
        handler.removeInitContainer(builder, "bla");
        assertThat(handler.hasInitContainer(builder, "bla")).isFalse();
        assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
        verifyBuilder(builder, Collections.singletonList(createInitContainer("blub", "foo/blub")));
    }

    @Test
    public void existingSame() {
        doNothing().when(log).warn(anyString(), anyString());
        PodTemplateSpecBuilder builder = getPodTemplateBuilder("blub", "foo/blub");
        assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
        Container initContainer = createInitContainer("blub", "foo/blub");
        handler.appendInitContainer(builder, initContainer);
        assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
        verifyBuilder(builder, Collections.singletonList(initContainer));
    }

    @Test
    public void existingDifferent() {
    PodTemplateSpecBuilder builder = getPodTemplateBuilder("blub", "foo/bla");
    assertThat(handler.hasInitContainer(builder, "blub")).isTrue();
    Container initContainer = createInitContainer("blub", "foo/blub");
    assertThatIllegalArgumentException()
                    .isThrownBy(() -> handler.appendInitContainer(builder, initContainer)).withMessageContaining("blub");
    }

    private void verifyBuilder(PodTemplateSpecBuilder builder, List<Container> initContainers) {
        PodTemplateSpec spec = builder.build();
        List<Container> initContainersInSpec = spec.getSpec().getInitContainers();
        if (initContainersInSpec.size() == 0) {
            assertThat(initContainers).isNull();
        } else {
            assertThat(initContainersInSpec).hasSameSizeAs(initContainers);
            for (int i = 0; i < initContainers.size(); i++) {
                assertThat(initContainersInSpec.get(i)).isEqualTo(initContainers.get(i));
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
        return new ContainerBuilder()
                .withName(name)
                .withImage(image)
                .build();
    }
}
