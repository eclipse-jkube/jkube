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
package io.jkube.kit.common.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author roland
 * @since 07/02/17
 */
public class ResourceUtilTest {

    @Test
    public void simple() {
        JsonParser parser = new JsonParser();
        JsonObject first = parser.parse("{first: bla, second: blub}").getAsJsonObject();
        JsonObject same = parser.parse("{second: blub, first: bla   }").getAsJsonObject();
        JsonObject different = parser.parse("{second: blub, first: bla2   }").getAsJsonObject();
        assertTrue(ResourceUtil.jsonEquals(first, same));
        assertFalse(ResourceUtil.jsonEquals(first, different));
    }
}
