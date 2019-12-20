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
package org.eclipse.jkube.thorntail.v2.enricher;

import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

import java.net.URLClassLoader;

public class ThorntailV2HealthCheckEnricherTest {

    @Mocked
    protected MavenEnricherContext context;

    private void setupExpectations() {
        new Expectations() {{
            context.getProjectClassLoaders();
            result = new ProjectClassLoaders((URLClassLoader) ThorntailV2HealthCheckEnricherTest.class.getClassLoader());
        }};
    }

    @Test
    @Ignore
    public void configureThorntailHealthPort() {

        setupExpectations();
        final ThorntailV2HealthCheckEnricher thorntailV2HealthCheckEnricher = new ThorntailV2HealthCheckEnricher(context);
        final int port = thorntailV2HealthCheckEnricher.getPort();
        Assert.assertEquals(8082, port);

    }

}
