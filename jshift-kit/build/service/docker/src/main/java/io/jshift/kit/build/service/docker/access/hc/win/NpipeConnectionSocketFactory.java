package io.jshift.kit.build.service.docker.access.hc.win;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import io.jshift.kit.build.service.docker.access.hc.util.AbstractNativeSocketFactory;
import io.jshift.kit.common.KitLogger;
import org.apache.http.protocol.HttpContext;

final class NpipeConnectionSocketFactory extends AbstractNativeSocketFactory {

	// Logging
    private final KitLogger log;

    NpipeConnectionSocketFactory(String npipePath, KitLogger log) {
        super(npipePath);
        this.log = log;
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        return new NamedPipe(log);
    }

    @Override
    protected SocketAddress createSocketAddress(String path) {
        return new NpipeSocketAddress(new File(path));
    }
}
