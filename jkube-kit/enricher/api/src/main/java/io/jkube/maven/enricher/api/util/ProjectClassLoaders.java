/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.maven.enricher.api.util;

import java.net.URLClassLoader;

public class ProjectClassLoaders {

    private URLClassLoader compileClassLoader;

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
