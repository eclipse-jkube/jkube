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

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.lang.reflect.Method;

public class PlexusContainerHelper {
    private final PlexusContainer plexusContainer;

    public PlexusContainerHelper(PlexusContainer pc) {
        this.plexusContainer = pc;
    }

    public String decryptString(String input) {
        try {
            // Done by reflection since I have classloader issues otherwise
            if (plexusContainer != null) {
                Object secDispatcher = plexusContainer.lookup(SecDispatcher.ROLE, "maven");
                Method method = secDispatcher.getClass().getMethod("decrypt", String.class);
                return (String) method.invoke(secDispatcher, input);
            } else {
                return input;
            }
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Error looking security dispatcher",e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot decrypt password: " + e.getCause(),e);
        }
    }
}
