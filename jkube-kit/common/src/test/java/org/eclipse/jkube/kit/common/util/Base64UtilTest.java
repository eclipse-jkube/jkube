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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Base64UtilTest {

    @Test
    public void testBase64EncodeAndDecode() {
        String raw = "Send reinforcements";
        String encode = "U2VuZCByZWluZm9yY2VtZW50cw==";
        assertEquals(encode, Base64Util.encodeToString(raw));
        assertEquals(raw, Base64Util.decodeToString(encode));
    }
}
