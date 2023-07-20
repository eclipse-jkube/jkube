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
package org.eclipse.jkube.kit.build.api.assembly;

import org.eclipse.jkube.kit.common.AssemblyFileEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collection of assembly files which need to be monitored for checking when
 * to rebuild an image.
 *
 * @author roland
 */
public class AssemblyFiles {

    private final File assemblyDirectory;
    private final List<AssemblyFileEntry> entries = new ArrayList<>();

    /**
     * Create a collection of assembly files
     *
     * @param assemblyDirectory directory into which the files are copied
     */
    public AssemblyFiles(File assemblyDirectory) {
        this.assemblyDirectory = assemblyDirectory;
    }

    /**
     * Add an entry to the list of assembly files which possible should be monitored
     *
     * @param assemblyFileEntry to monitor.
     */
    public void addEntry(AssemblyFileEntry assemblyFileEntry) {
        entries.add(assemblyFileEntry);
    }

    /**
     * Get the list of all updated entries i.e. all entries which have modification date
     * which is newer than the last time check. ATTENTION: As a side effect this method also
     * updates the timestamp of updated entries.
     *
     * @return list of all entries which has been updated since the last call to this method or an empty list
     */
    public List<AssemblyFileEntry> getUpdatedEntriesAndRefresh() {
        return entries.stream().filter(AssemblyFileEntry::isUpdated).collect(Collectors.toList());
    }

    /**
     * Returns true if there are no entries
     *
     * @return boolean value whether empty or not
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Return destination directory where the files are copied into
     *
     * @return top-level directory holding the assembled files
     */
    public File getAssemblyDirectory() {
        return assemblyDirectory;
    }

}
