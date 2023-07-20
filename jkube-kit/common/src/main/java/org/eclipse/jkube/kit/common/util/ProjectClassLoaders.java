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

import java.net.URLClassLoader;

public class ProjectClassLoaders {

    private final URLClassLoader compileClassLoader;

    public ProjectClassLoaders(URLClassLoader compileClassLoader) {
        this.compileClassLoader = compileClassLoader;
    }

    public URLClassLoader getCompileClassLoader() {
        return compileClassLoader;
    }

    /**
     * Returns if class is in compile classpath.
     * @param all True if all of them must be there.
     * @param clazz fully qualified class name.
     * @return True if present, false otherwise.
     */
    public boolean isClassInCompileClasspath(boolean all, String... clazz) {
        if (all) {
            return hasAllClasses(clazz);
        } else {
            return hasAnyClass(clazz);
        }
    }

    private boolean hasAnyClass(String... classNames) {
        for (String className : classNames) {
            try {
                compileClassLoader.loadClass(className);
                return true;
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        return false;
    }

    private boolean hasAllClasses(String... classNames) {
        for (String className : classNames) {
            try {
                compileClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                // ignore message
                return false;
            }
        }
        return true;
    }
}
