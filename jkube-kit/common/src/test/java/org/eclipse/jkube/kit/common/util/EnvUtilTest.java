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
import mockit.Verifications;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.eclipse.jkube.kit.common.util.EnvUtil.loadTimestamp;
import static org.eclipse.jkube.kit.common.util.EnvUtil.storeTimestamp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class EnvUtilTest {

    @Test
    public void testStoreTimestamp(
            @Mocked Files files, @Mocked File fileToStoreTimestamp, @Mocked File dir) throws IOException {
        // Given
        new Expectations() {{
           fileToStoreTimestamp.exists() ;
           result = false;
           fileToStoreTimestamp.getParentFile();
           result = dir;
           dir.exists();
           result = true;
        }};
        final Date date = new Date(1445385600000L);
        // When
        storeTimestamp(fileToStoreTimestamp, date);
        // Then
        new Verifications() {{
            files.write(withInstanceOf(Path.class), "1445385600000".getBytes(StandardCharsets.US_ASCII));
        }};
    }

    @Test
    public void testLoadTimestampShouldLoadFromFile() throws Exception {
        // Given
        final File file = new File(EnvUtilTest.class.getResource("/util/loadTimestamp.timestamp").getFile());
        // When
        final Date timestamp = loadTimestamp(file);
        // Then
        assertThat(timestamp, equalTo(new Date(1445385600000L)));
    }
}
