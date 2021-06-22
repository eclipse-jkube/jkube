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

import mockit.Expectations;
import mockit.Mocked;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Test;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PlexusContainerHelperTest {
    @Mocked
    private PlexusContainer plexusContainer;

    @Mocked
    private DefaultSecDispatcher secDispatcher;

    @Test
    public void testDecryptString() throws ComponentLookupException, SecDispatcherException {
        // Given
        new Expectations() {{
            secDispatcher.decrypt(anyString);
            result = "output";

            plexusContainer.lookup(anyString, anyString);
            result = secDispatcher;
        }};
        PlexusContainerHelper plexusContainerHelper = new PlexusContainerHelper(plexusContainer);

        // When
        String result = plexusContainerHelper.decryptString("input");

        // Then
        assertNotNull(result);
        assertEquals("output", result);
    }

    @Test
    public void testDecryptStringNullPlexusContainer() {
        // Given
        PlexusContainerHelper plexusContainerHelper = new PlexusContainerHelper(null);

        // When
        String result = plexusContainerHelper.decryptString("input");

        // Then
        assertEquals("input", result);
    }
}
