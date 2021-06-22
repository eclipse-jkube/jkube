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

import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Test;

import java.io.File;

public class ValidationUtilTest {
    @Mocked
    KitLogger log;

    @Test(expected = IllegalStateException.class)
    public void testValidateIfRequiredFailOnValidationTrue() {
        // Given
        File resourceDir = new File(getClass().getResource("/util/validator").getFile());
        ResourceClassifier classifier = ResourceClassifier.KUBERNETES;

        // When
        ValidationUtil.validateIfRequired(resourceDir, classifier, log, false, true);
    }

    @Test
    public void testValidateIfRequiredFailOnValidationFalse() {
        // Given
        File resourceDir = new File(getClass().getResource("/util/validator").getFile());
        ResourceClassifier classifier = ResourceClassifier.KUBERNETES;

        // When
        ValidationUtil.validateIfRequired(resourceDir, classifier, log, false, false);

        // Then
        new Verifications() {{
            log.warn(anyString);
            times = 1;
        }};
    }
}
