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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * For java 7 or lower version, java.util doesn't provide a base64 encode/decode way
 */
public class Base64Util {

    public static String encodeToString(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }

    public static byte[] encode(byte[] bytes) {
        return encodeToString(bytes).getBytes(StandardCharsets.UTF_8);
    }

    public static String encodeToString(String raw) {
        return encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encode(String raw) {
        return encode(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] decode(String raw) {
        return Base64.getDecoder().decode(raw);
    }

    public static byte[] decode(byte[] bytes) {
        return decode(new String(bytes));
    }

    public static String decodeToString(String raw) {
        return new String(decode(raw));
    }

    public static String decodeToString(byte[] bytes) {
        return new String(decode(bytes));
    }
}

