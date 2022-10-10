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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectClassLoadersTest {

    private URLClassLoader compileClassLoader;

    @BeforeEach
    void setUp(@TempDir File temporaryFolder) throws Exception {
        File applicationProp =  new File(getClass().getResource("/util/spring-boot-application.properties").getPath());
        File targetFolder = new File(temporaryFolder, "target");
        File classesInTarget = new File(targetFolder, "classes");
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application.properties");
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        compileClassLoader = ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()), classesInTarget.getAbsolutePath());
    }

    @Test
    void testIsClassInCompileClasspathWhenTrue() {
        //Given
        boolean all = true;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all);

        //Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsClassInCompileClasspathWhenFalse() {
        //Given
        boolean all = false;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all,"ProjectClassLoadersTest","UserConfigurationCompare");

        //Then
        assertThat(result).isFalse();
    }

    @Test
    void testIsClassInCompileClasspathWhenHasAllClassesTrue() {
        //Given
        boolean all = true;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all,"ProjectClassLoadersTest","UserConfigurationCompare");

        //Then
        assertThat(result).isFalse();
    }

    @Test
    void testIsClassInCompileClasspathWhenHasAllClassesFalse() {
        //Given
        boolean all = false;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result  =  obj.isClassInCompileClasspath(all);

        //Then
        assertThat(result).isFalse();
    }
}
