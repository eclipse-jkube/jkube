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

import java.util.ArrayList;
import java.util.List;

public class ConfigMap {

    private String name;
    private final List<ConfigMapEntry> entries = new ArrayList<>();

    public void addEntry(ConfigMapEntry configMapEntry) {
        this.entries.add(configMapEntry);
    }

    public List<ConfigMapEntry> getEntries() {
        return entries;
    }

    /**
     * Set the name of ConfigMap.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the name of ConfigMap.
     * @return
     */
    public String getName() {
        return name;
    }
}


