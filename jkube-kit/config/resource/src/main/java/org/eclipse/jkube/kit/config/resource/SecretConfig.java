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
package org.eclipse.jkube.kit.config.resource;


import org.apache.maven.plugins.annotations.Parameter;

public class SecretConfig {

    @Parameter
    private String name;

    @Parameter
    private String dockerServerId;

    @Parameter
    private String namespace;

    public String getName() {
        return name;
    }

    public String getDockerServerId() {
        return dockerServerId;
    }

    public String getNamespace() {
        return namespace;
    }

}
