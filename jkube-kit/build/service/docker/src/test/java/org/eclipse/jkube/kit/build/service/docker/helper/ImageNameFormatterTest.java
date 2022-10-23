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
package org.eclipse.jkube.kit.build.service.docker.helper;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */
class ImageNameFormatterTest {
    private JavaProject project;
    private final Date now = new Date();
    private ImageNameFormatter formatter;

    @BeforeEach
    void setUp(){
        project = mock(JavaProject.class,RETURNS_DEEP_STUBS);
        formatter = new ImageNameFormatter(project,now);
    }
    @Test
    void simple() {
        assertThat(formatter.format("bla")).isEqualTo("bla");
    }

    @Test
    void invalidFormatChar() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> formatter.format("bla %z"))
                .withMessage("No image name format element '%z' known");
    }

    @Test
    void defaultUserName() {

        final String[] data = {
            "io.fabric8", "fabric8",
            "io.FABRIC8", "fabric8",
            "io.fabric8.", "fabric8",
            "io.fabric8", "fabric8",
            "fabric8....", "fabric8",
            "io.fabric8___", "fabric8__"
        };

        for (int i = 0; i < data.length; i+=2) {
            when(project.getProperties()).thenReturn(new Properties());
            when(project.getGroupId()).thenReturn(data[i]);
            String value = formatter.format("%g");
            assertThat(value).as("Idx. " + i / 2).isEqualTo(data[i+1]);
        }
    }

    @Test
    void artifact() {
        when(project.getArtifactId()).thenReturn("Docker....Maven.....Plugin");
        assertThat(formatter.format("--> %a <--")).isEqualTo("--> docker.maven.plugin <--");
    }

    @Test
    void tagWithProperty() {
        // Given
        final Properties props = new Properties();
        props.put("jkube.image.tag","1.2.3");
        when(project.getProperties()).thenReturn(props);
        // When
        final String result = formatter.format("%t");
        // Then
        assertThat(result).isEqualTo("1.2.3");
    }

    @Test
    void tag() {
        when(project.getArtifactId()).thenReturn("docker-maven-plugin");
        when(project.getGroupId()).thenReturn("io.fabric8");
        when(project.getVersion()).thenReturn("1.2.3-SNAPSHOT");
        when(project.getProperties()).thenReturn(new Properties());
        assertThat(formatter.format("%g/%a:%l")).isEqualTo("fabric8/docker-maven-plugin:latest");
        assertThat(formatter.format("%g/%a:%v")).isEqualTo("fabric8/docker-maven-plugin:1.2.3-SNAPSHOT");
        assertThat(formatter.format("%g/%a:%t")).matches(".*snapshot-[\\d-]+$");
    }

    @Test
    void nonSnapshotArtifact() {
        when(project.getArtifactId()).thenReturn("docker-maven-plugin");
        when(project.getGroupId()).thenReturn("io.fabric8");
        when(project.getVersion()).thenReturn("1.2.3");
        when(project.getProperties()).thenReturn(new Properties());
        assertThat(formatter.format("%g/%a:%l")).isEqualTo("fabric8/docker-maven-plugin:1.2.3");
        assertThat(formatter.format("%g/%a:%v")).isEqualTo("fabric8/docker-maven-plugin:1.2.3");
        assertThat(formatter.format("%g/%a:%t")).isEqualTo("fabric8/docker-maven-plugin:1.2.3");
    }

    @Test
    void groupIdWithProperty() {
        // Given
        Properties props = new Properties();
        props.put("jkube.image.user","this.it..is");
        when(project.getProperties()).thenReturn(props);
        // When
        final String result = formatter.format("%g/name");
        // Then
        assertThat(result).isEqualTo("this.it..is/name");
    }
}
