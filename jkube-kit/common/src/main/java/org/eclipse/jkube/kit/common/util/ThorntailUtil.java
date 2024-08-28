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

import org.eclipse.jkube.kit.common.JavaProject;

import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.fromApplicationConfig;

public class ThorntailUtil {
    private static final String[] THORNTAIL_APP_CONFIG_FILES_LIST = new String[] {"project-defaults.yml"};

    private ThorntailUtil() {}

    /**
     * Returns the thorntail configuration (supports `project-defaults.yml`)
     * or an empty properties object if not found
     *
     * @param javaProject Java Project
     * @return thorntail configuration properties
     */
    public static Properties getThorntailProperties(JavaProject javaProject) {
        return fromApplicationConfig(javaProject, THORNTAIL_APP_CONFIG_FILES_LIST);
    }
}
