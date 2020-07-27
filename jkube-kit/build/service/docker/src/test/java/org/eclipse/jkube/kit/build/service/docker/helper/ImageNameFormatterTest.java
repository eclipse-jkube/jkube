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

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Test;

import java.util.Date;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/**
 * @author roland
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class ImageNameFormatterTest {

    @Injectable
    private JavaProject project;

    @Injectable
    private Date now = new Date();

    @Tested
    private ImageNameFormatter formatter;

    @Test
    public void simple() {
        assertThat(formatter.format("bla"),equalTo("bla"));
    }

    @Test
    public void invalidFormatChar() {
        final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {
            formatter.format("bla %z");
            fail();
        });
        assertThat("Doesnt match", result.getMessage(), containsString("%z"));
    }

    @Test
    public void defaultUserName() {

        final String[] data = {
            "io.fabric8", "fabric8",
            "io.FABRIC8", "fabric8",
            "io.fabric8.", "fabric8",
            "io.fabric8", "fabric8",
            "fabric8....", "fabric8",
            "io.fabric8___", "fabric8__"
        };

        for (int i = 0; i < data.length; i+=2) {

            final int finalI = i;
            new Expectations() {{
                project.getProperties(); result = new Properties();
                project.getGroupId(); result = data[finalI];
            }};

            String value = formatter.format("%g");
            assertThat("Idx. " + i / 2,value, equalTo(data[i+1]));
        }
    }

    @Test
    public void artifact() {
        new Expectations() {{
            project.getArtifactId(); result = "Docker....Maven.....Plugin";
        }};

        assertThat(formatter.format("--> %a <--"),equalTo("--> docker.maven.plugin <--"));
    }

    @Test
    public void tagWithProperty() {
        // Given
        final Properties props = new Properties();
        props.put("jkube.image.tag","1.2.3");
        // @formatter:off
        new Expectations() {{
            project.getProperties(); result = props;
        }};
        // @formatter:on
        // When
        final String result = formatter.format("%t");
        // Then
        assertThat(result , equalTo("1.2.3"));
    }

    @Test
    public void tag() {
        new Expectations() {{
            project.getArtifactId(); result = "docker-maven-plugin";
            project.getGroupId(); result = "io.fabric8";
            project.getVersion(); result = "1.2.3-SNAPSHOT";
            project.getProperties(); result = new Properties();
        }};
        assertThat(formatter.format("%g/%a:%l"), equalTo("fabric8/docker-maven-plugin:latest"));
        assertThat(formatter.format("%g/%a:%v"), equalTo("fabric8/docker-maven-plugin:1.2.3-SNAPSHOT"));
        assertThat(formatter.format("%g/%a:%t").matches(".*snapshot-[\\d-]+$"), is(true));
    }

    @Test
    public void nonSnapshotArtifact() {
        new Expectations() {{
            project.getArtifactId(); result = "docker-maven-plugin";
            project.getGroupId(); result = "io.fabric8";
            project.getVersion(); result = "1.2.3";
            project.getProperties(); result = new Properties();
        }};

        assertThat(formatter.format("%g/%a:%l"), equalTo("fabric8/docker-maven-plugin:1.2.3"));
        assertThat(formatter.format("%g/%a:%v"), equalTo("fabric8/docker-maven-plugin:1.2.3"));
        assertThat(formatter.format("%g/%a:%t"), equalTo("fabric8/docker-maven-plugin:1.2.3"));
    }

    @Test
    public void groupIdWithProperty() {
        // Given
        Properties props = new Properties();
        props.put("jkube.image.user","this.it..is");
        // @formatter:off
        new Expectations() {{
            project.getProperties(); result = props;
        }};
        // @formatter:on
        // When
        final String result = formatter.format("%g/name");
        // Then
        assertThat(result ,equalTo("this.it..is/name"));
    }
}
