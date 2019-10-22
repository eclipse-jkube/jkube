/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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
