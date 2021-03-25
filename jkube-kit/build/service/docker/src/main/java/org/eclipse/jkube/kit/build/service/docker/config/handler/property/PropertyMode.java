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
package org.eclipse.jkube.kit.build.service.docker.config.handler.property;


/**
 * Identifies how the {@link PropertyConfigHandler} should treat properties vs configuration
 * from POM file in the {@link ValueProvider}.
 *
 * @author Johan Ström
 */
public enum PropertyMode {
    Only,
    Override,
    Fallback,
    Skip;

    /**
     * Given String, parse to a valid property mode.
     *
     * If null, the default Only is given.
     *
     * @param name null or a valid name
     * @return PropertyMode
     */
    static PropertyMode parse(String name) {
        if(name == null) {
            return PropertyMode.Only;
        }

        name = name.toLowerCase();
        for (PropertyMode e : PropertyMode.values()) {
            if (e.name().toLowerCase().equals(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("PropertyMode: invalid mode "+name+". Valid: "+values());
    }
}
