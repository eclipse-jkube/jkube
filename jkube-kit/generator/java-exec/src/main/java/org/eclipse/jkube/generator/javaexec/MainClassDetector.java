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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author roland
 * @since 11/11/16
 */
class MainClassDetector {

    private String mainClass = null;
    private final File classesDir;
    private final KitLogger log;

    MainClassDetector(String mainClass, File classesDir, KitLogger log) {
        this.mainClass = mainClass;
        this.classesDir = classesDir;
        this.log = log;
    }

    String getMainClass() throws MojoExecutionException {
        if (mainClass != null) {
            return mainClass;
        }

        // Try to detect a single main class from target/classes
        try {
            List<String> foundMainClasses = ClassUtil.findMainClasses(classesDir);
            if (foundMainClasses.size() == 0) {
                return mainClass = null;
            } else if (foundMainClasses.size() == 1) {
                return mainClass = foundMainClasses.get(0);
            } else {
                log.warn("Found more than one main class : %s. Ignoring ....",  foundMainClasses);
                return mainClass = null;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can not examine main classes: " + e, e);
        }
    }
}
