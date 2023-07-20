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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class ThorntailUtil {

    private ThorntailUtil() {}

    /**
     * Returns the thorntail configuration (supports `project-defaults.yml`)
     * or an empty properties object if not found
     *
     * @param compileClassLoader URLClassLoader for resource access
     * @return thorntail configuration properties
     */
    public static Properties getThorntailProperties(URLClassLoader compileClassLoader) {
        URL ymlResource = compileClassLoader.findResource("project-defaults.yml");
        return YamlUtil.getPropertiesFromYamlResource(ymlResource);
    }
}
