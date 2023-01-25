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
package org.eclipse.jkube.kit.common.util;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SpringBootUtilTest {

    private JavaProject mavenProject;

    @BeforeEach
    public void setUp() {
        mavenProject = mock(JavaProject.class);
    }

    @Test
    void testGetSpringBootApplicationProperties(@TempDir File temporaryFolder) throws IOException {
        //Given
        File applicationProp =  new File(getClass().getResource("/util/spring-boot-application.properties").getPath());
        String springActiveProfile = null;
        File targetFolder = new File(temporaryFolder, "target");
        File classesInTarget = new File(targetFolder, "classes");
        boolean isTargetClassesCreated = classesInTarget.mkdirs();
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application.properties");
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        URLClassLoader urlClassLoader = ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()), classesInTarget.getAbsolutePath());

        //When
        Properties result =  SpringBootUtil.getSpringBootApplicationProperties(springActiveProfile ,urlClassLoader);

        //Then
        assertThat(isTargetClassesCreated).isTrue();
        assertThat(result).containsOnly(
                entry("spring.application.name", "demoservice"),
                entry("server.port", "9090")
        );
    }

    @Test
    void testGetSpringBootDevToolsVersion() {
        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        when(mavenProject.getDependencies()).thenReturn(Collections.singletonList(p));

        //when
        Optional<String> result = SpringBootUtil.getSpringBootDevToolsVersion(mavenProject);

        //Then
        assertThat(result).isPresent().contains("1.6.3");
    }


    @Test
    void testGetSpringBootVersion() {
        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        when(mavenProject.getDependencies()).thenReturn(Collections.singletonList(p));

        //when
        Optional<String> result = SpringBootUtil.getSpringBootVersion(mavenProject);

        //Then
        assertThat(result).isPresent().contains("1.6.3");
    }

    @Test
    void testGetSpringBootActiveProfileWhenNotNull() {
        //Given
        Properties p = new Properties();
        p.put("spring.profiles.active","spring-boot");
        when(mavenProject.getProperties()).thenReturn(p);

        // When
        String result = SpringBootUtil.getSpringBootActiveProfile(mavenProject);

        //Then
        assertThat(result).isEqualTo("spring-boot");
    }

    @Test
    void testGetSpringBootActiveProfileWhenNull() {
        assertThat(SpringBootUtil.getSpringBootActiveProfile(null)).isNull();
    }
}
