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
package org.eclipse.jkube.kit.build.maven;

import java.util.Properties;

import org.apache.maven.execution.MavenSession;

/**
 * @author roland
 * @since 17.10.18
 */
public class MavenCacheBackend implements ImagePullCache.Backend {

    private MavenSession session;

    public MavenCacheBackend(MavenSession session) {
        this.session = session;
    }

    @Override
    public String get(String key) {
        Properties userProperties = session.getUserProperties();
        return userProperties.getProperty(key);
    }

    @Override
    public void put(String key, String value) {
        Properties userProperties = session.getUserProperties();
        userProperties.setProperty(key, value);
    }
}
