package io.jkube.kit.common.util;

import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

/**
 * For java 7 or lower version, java.util doesn't provide a base64 encode/decode way
 */
public class Base64Util {

    public static String encodeToString(byte[] bytes) {
        return DatatypeConverter.printBase64Binary(bytes);
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
        return DatatypeConverter.parseBase64Binary(raw);
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

