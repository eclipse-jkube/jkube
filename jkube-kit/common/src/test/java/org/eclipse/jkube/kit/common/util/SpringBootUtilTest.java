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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringBootUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    JavaProject mavenProject;


    @Test
    public void testGetSpringBootApplicationProperties() throws IOException {

        //Given
        File applicationProp =  new File(getClass().getResource("/util/spring-boot-application.properties").getPath());
        String springActiveProfile = null;
        File targetFolder = temporaryFolder.newFolder("target");
        File classesInTarget = new File(targetFolder, "classes");
        boolean isTargetClassesCreated = classesInTarget.mkdirs();
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application.properties");
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        URLClassLoader urlClassLoader = ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()), classesInTarget.getAbsolutePath());

        //When
        Properties result =  SpringBootUtil.getSpringBootApplicationProperties(springActiveProfile ,urlClassLoader);

        //Then
        assertTrue(isTargetClassesCreated);
        assertEquals("demoservice" ,result.getProperty("spring.application.name"));
        assertEquals("9090" ,result.getProperty("server.port"));
    }

    @Test
    public void testGetSpringBootDevToolsVersion() {

        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        when(mavenProject.getDependencies()).thenReturn(Collections.singletonList(p));

        //when
        Optional<String> result = SpringBootUtil.getSpringBootDevToolsVersion(mavenProject);

        //Then
        assertTrue(result.isPresent());
        assertEquals("1.6.3",result.get());

    }


    @Test
    public void testGetSpringBootVersion() {

        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        when(mavenProject.getDependencies()).thenReturn(Collections.singletonList(p));

        //when
        Optional<String> result = SpringBootUtil.getSpringBootVersion(mavenProject);

        //Then
        assertTrue(result.isPresent());
        assertEquals("1.6.3",result.get());

    }

    @Test
    public void testGetSpringBootActiveProfileWhenNotNull() {

        //Given
        Properties p = new Properties();
        p.put("spring.profiles.active","spring-boot");
        when(mavenProject.getProperties()).thenReturn(p);

        // When
        String result = SpringBootUtil.getSpringBootActiveProfile(mavenProject);

        //Then
        assertEquals("spring-boot",result);
    }

    @Test
    public void testGetSpringBootActiveProfileWhenNull() {

        assertNull(SpringBootUtil.getSpringBootActiveProfile(null));

    }
}
