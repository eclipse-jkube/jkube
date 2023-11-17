/*
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectClassLoadersTest {

    private URLClassLoader compileClassLoader;

    @BeforeEach
    void setUp(@TempDir File temporaryFolder) throws Exception {
        File classesInTarget = new File(new File(temporaryFolder, "target"), "classes");
        FileUtil.createDirectory(classesInTarget);
        compileClassLoader = ClassUtil.createClassLoader(Collections.singletonList(classesInTarget.getAbsolutePath()));
    }

    @Test
    void isClassInCompileClasspathWhenTrue() {
        //Given
        boolean all = true;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result = obj.isClassInCompileClasspath(all);

        //Then
        assertThat(result).isTrue();
    }

    @Test
    void isClassInCompileClasspathWhenFalse() {
        //Given
        boolean all = false;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result = obj.isClassInCompileClasspath(all,"ProjectClassLoadersTest", "UserConfigurationCompare");

        //Then
        assertThat(result).isFalse();
    }

    @Test
    void isClassInCompileClasspathWhenHasAllClassesTrue() {
        //Given
        boolean all = true;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result = obj.isClassInCompileClasspath(all,"ProjectClassLoadersTest", "UserConfigurationCompare");

        //Then
        assertThat(result).isFalse();
    }

    @Test
    void isClassInCompileClasspathWhenHasAllClassesFalse() {
        //Given
        boolean all = false;
        ProjectClassLoaders obj = new ProjectClassLoaders(compileClassLoader);

        //When
        boolean result = obj.isClassInCompileClasspath(all);

        //Then
        assertThat(result).isFalse();
    }
}
