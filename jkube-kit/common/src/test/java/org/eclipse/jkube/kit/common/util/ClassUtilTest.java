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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.eclipse.jkube.kit.common.util.FileUtil.getAbsolutePath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author roland
 * @since 01/08/16
 */

class ClassUtilTest {

    @Test
    void findOne() throws IOException {
        File root = getRelativePackagePath("mainclass/one");
        List<String> ret = ClassUtil.findMainClasses(root);
        assertThat(ret).singleElement()
                .isEqualTo("sub.OneMain");
    }

    @Test
    void findTwo() throws IOException {
        File root = getRelativePackagePath("mainclass/two");
        Set<String> ret = new HashSet<>(ClassUtil.findMainClasses(root));
        assertThat(ret).hasSize(2)
                .contains("OneMain", "another.sub.a.bit.deeper.TwoMain");
    }

    @Test
    void findNone() throws IOException {
        File root = getRelativePackagePath("mainclass/zero");
        List<String> ret = ClassUtil.findMainClasses(root);
        assertThat(ret).isEmpty();
    }

    private File getRelativePackagePath(String subpath) {
    	File parent =        		
            new File(Objects.requireNonNull(getAbsolutePath(this.getClass().getProtectionDomain().getCodeSource().getLocation())));
        String intermediatePath = getClass().getPackage().getName().replace(".","/");
        return new File(new File(parent, intermediatePath),subpath);
    }
}
