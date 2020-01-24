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
package org.eclipse.jkube.kit.build.core.assembly;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

/**
 * @author roland
 * @since 26/06/16
 */
class AllFilesExecCustomizer implements ArchiverCustomizer {
    private KitLogger log;

    AllFilesExecCustomizer(KitLogger logger) {
        this.log = logger;
    }

    @Override
    public TarArchiver customize(TarArchiver archiver) throws IOException {
        log.warn("/--------------------- SECURITY WARNING ---------------------\\");
        log.warn("|You are building a Docker image with normalized permissions.|");
        log.warn("|All files and directories added to build context will have  |");
        log.warn("|'-rwxr-xr-x' permissions. It is recommended to double check |");
        log.warn("|and reset permissions for sensitive files and directories.  |");
        log.warn("\\------------------------------------------------------------/");

        TarArchiver newArchiver = new TarArchiver();
        newArchiver.setDestFile(archiver.getDestFile());
        newArchiver.setLongfile(TarLongFileMode.posix);

        ResourceIterator resources = archiver.getResources();
        while (resources.hasNext()) {
            ArchiveEntry ae = resources.next();
            String fileName = ae.getName();
            PlexusIoResource resource = ae.getResource();
            String name = fileName.replace(File.separator, "/");

            // See docker source:
            // https://github.com/docker/docker/blob/3d13fddd2bc4d679f0eaa68b0be877e5a816ad53/pkg/archive/archive_windows.go#L45
            int mode = ae.getMode() & 0777;
            int newMode = mode;
            newMode &= 0755;
            newMode |= 0111;

            if (newMode != mode) {
                log.debug("Changing permissions of '%s' from %o to %o.", name, mode, newMode);
            }

            newArchiver.addResource(resource, name, newMode);
        }

        archiver = newArchiver;

        return archiver;
    }
}
