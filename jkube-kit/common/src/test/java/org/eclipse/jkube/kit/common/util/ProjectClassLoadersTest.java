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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ProjectClassLoadersTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    private URLClassLoader compileClassLoader;
    @Before
    public void setUp() throws Exception {
        File applicationProp =  new File(getClass().getResource("/util/spring-boot-application.properties").toURI());
        File targetFolder = temporaryFolder.newFolder("target");
        File classesInTarget = new File(targetFolder, "classes");
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application.properties");
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        compileClassLoader = ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()), classesInTarget.getAbsolutePath());
    }

    @Test
    public void testIsClassInCompileClasspathWhenTrue() {
        //Given
        boolean all = true;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all);

        //Then
        assertTrue(result);
    }
    @Test
    public void testIsClassInCompileClasspathWhenFalse() {
        //Given
        boolean all = false;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all,"ProjectClassLoadersTest","UserConfigurationCompare");

        //Then
        assertFalse(result);
    }

    @Test
    public void testIsClassInCompileClasspathWhenHasAllClassesTrue() {
        //Given
        boolean all = true;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all,"ProjectClassLoadersTest","UserConfigurationCompare");

        //Then
        assertFalse(result);
    }

    @Test
    public void testIsClassInCompileClasspathWhenHasAllClassesFalse() {
        //Given
        boolean all = false;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all);

        //Then
        assertFalse(result);
    }
}


