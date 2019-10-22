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
package io.jkube.kit.build.service.docker.helper;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author roland
 * @since 19.10.14
 */
public class SuffixFileFilter implements FilenameFilter {

    final public static FilenameFilter PEM_FILTER = new SuffixFileFilter(".pem");

    private String suffix;

    public SuffixFileFilter(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(suffix);
    }
}
