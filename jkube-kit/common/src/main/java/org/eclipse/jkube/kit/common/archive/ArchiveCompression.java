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
package org.eclipse.jkube.kit.common.archive;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Enumeration for determine the compression mode when creating docker
 * build archives.
 *
 * @author roland
 * @since 26/10/15
 */
public enum ArchiveCompression {

    none(TarCompressionMethod.none, "tar"),

    gzip(TarCompressionMethod.gzip,"tar.gz") {
        @Override
        public OutputStream wrapOutputStream(OutputStream out) throws IOException {
            return new ArchiveCompression.GZIPOutputStream(out);
        }
    },

    bzip2(TarCompressionMethod.bzip2,"tar.bz") {
        @Override
        public OutputStream wrapOutputStream(OutputStream out) throws IOException {
            return new BZip2CompressorOutputStream(out);
        }
    };

    // ====================================================================

    private final TarCompressionMethod tarCompressionMethod;
    private final String fileSuffix;

    ArchiveCompression(TarCompressionMethod tarCompressionMethod, String fileSuffix) {
        this.tarCompressionMethod = tarCompressionMethod;
        this.fileSuffix = fileSuffix;
    }

    public TarCompressionMethod getTarCompressionMethod() {
        return tarCompressionMethod;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public OutputStream wrapOutputStream(OutputStream outputStream) throws IOException {
        return outputStream;
    }

    /**
     * Valid Modes for Compression
     */
    public enum TarCompressionMethod {
        none,
        gzip,
        bzip2,
        snappy,
        xz
    }

    public static ArchiveCompression fromFileName(String filename) {
        if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            return ArchiveCompression.gzip;
        }

        if (filename.endsWith(".tar.bz") || filename.endsWith(".tar.bzip2") || filename.endsWith(".tar.bz2")) {
            return ArchiveCompression.bzip2;
        }
        return ArchiveCompression.none;
    }

    private static class GZIPOutputStream extends java.util.zip.GZIPOutputStream {
        private GZIPOutputStream(OutputStream out) throws IOException {
            super(out, 65536);
            // According to https://bugs.openjdk.java.net/browse/JDK-8142920, 3 is a better default
            def.setLevel(3);
        }
    }

}
