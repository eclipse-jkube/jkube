package io.jkube.kit.common.util;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IoUtilTest {

    @Test
    public void findOpenPort() throws IOException {
        int port = IoUtil.getFreeRandomPort();
        try (ServerSocket ss = new ServerSocket(port)) {
        }
    }

    @Test
    public void findOpenPortWhenPortsAreBusy() throws IOException {
        int port = IoUtil.getFreeRandomPort(49152, 60000, 100);
        try (ServerSocket ss = new ServerSocket(port)) {
        }
        int port2 = IoUtil.getFreeRandomPort(port, 65535, 100);
        try (ServerSocket ss = new ServerSocket(port2)) {
        }
        assertTrue(port2 > port);
    }

    @Test
    public void testSanitizeFileName() {
        assertEquals(null, IoUtil.sanitizeFileName(null));
        assertEquals("Hello-World", IoUtil.sanitizeFileName("Hello/&%World"));
        assertEquals("-H-e-l-l-o-", IoUtil.sanitizeFileName(" _H-.-e-.-l-l--.//o()"));
        assertEquals("s2i-env-docker-io-fabric8-java-latest", IoUtil.sanitizeFileName("s2i-env-docker.io/fabric8/java:latest"));
    }

}
