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
package org.eclipse.jkube.kit.config.image.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.eclipse.jkube.kit.config.image.build.CleanupMode.NONE;
import static org.eclipse.jkube.kit.config.image.build.CleanupMode.REMOVE;
import static org.eclipse.jkube.kit.config.image.build.CleanupMode.TRY_TO_REMOVE;

/**
 * @author roland
 * @since 01/03/16
 */
class CleanupModeTest {

    @Test
    void parse() {

        Object[] data = {
            null, TRY_TO_REMOVE,
            "try", TRY_TO_REMOVE,
            "FaLsE", NONE,
            "NONE", NONE,
            "true", REMOVE,
            "removE", REMOVE
        };

        for (int i = 0; i < data.length; i += 2) {
            assertThat(CleanupMode.parse((String) data[i])).isEqualTo(data[i + 1]);
        }
    }

    @Test
    void invalid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CleanupMode.parse("blub"))
                .withMessageContainingAll("blub", "try", "none", "remove");
    }
}
