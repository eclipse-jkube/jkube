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

import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpringBootUtilTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetSpringBootApplicationProperties() throws IOException, URISyntaxException {

        //Given
        File applicationProp =  new File(getClass().getResource("/util/spring-boot-application.properties").toURI());
        String springActiveProfile = null;
        File targetFolder = temporaryFolder.newFolder("target");
        File classesInTarget = new File(targetFolder, "classes");
        boolean isTargetClassesCreated = classesInTarget.mkdirs();
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application.properties");
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        URLClassLoader urlClassLoader = ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()), classesInTarget.getAbsolutePath());

        //When
        Properties result = SpringBootUtil.getSpringBootApplicationProperties(springActiveProfile ,urlClassLoader);

        //Then
        assertTrue(isTargetClassesCreated);
        assertEquals("demoservice" ,result.getProperty("spring.application.name"));
        assertEquals("9090" ,result.getProperty("server.port"));
    }

    @Test
    public void testGetSpringBootDevToolsVersion(@Mocked JavaProject maven_project) {

        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        new Expectations() {{
            maven_project.getDependencies();
            result= Collections.singletonList(p);
        }};

        //when
        Optional<String> result = SpringBootUtil.getSpringBootDevToolsVersion(maven_project);

        //Then
        assertTrue(result.isPresent());
        assertEquals("1.6.3",result.get());

    }


    @Test
    public void testGetSpringBootVersion(@Mocked JavaProject maven_project) {

        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        new Expectations() {{
            maven_project.getDependencies();
            result= Collections.singletonList(p);
        }};

        //when
        Optional<String> result = SpringBootUtil.getSpringBootVersion(maven_project);

        //Then
        assertTrue(result.isPresent());
        assertEquals("1.6.3",result.get());

    }

    @Test
    public void testGetSpringBootActiveProfileWhenNotNull(@Mocked JavaProject project) {

        //Given
        Properties p = new Properties();
        p.put("spring.profiles.active","spring-boot");
        new Expectations() {{
            project.getProperties();
            result=p;
        }};

        // When
        String result = SpringBootUtil.getSpringBootActiveProfile(project);

        //Then
        assertEquals("spring-boot",result);
    }

    @Test
    public void testGetSpringBootActiveProfileWhenNull() {

        assertNull(SpringBootUtil.getSpringBootActiveProfile(null));

    }
}
