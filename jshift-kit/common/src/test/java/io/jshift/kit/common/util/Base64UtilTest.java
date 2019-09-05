package io.jshift.kit.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base64UtilTest {

    @Test
    public void testBase64EncodeAndDecode() {
        String raw = "Send reinforcements";
        String encode = "U2VuZCByZWluZm9yY2VtZW50cw==";
        assertEquals(encode, Base64Util.encodeToString(raw));
        assertEquals(raw, Base64Util.decodeToString(encode));
    }
}
