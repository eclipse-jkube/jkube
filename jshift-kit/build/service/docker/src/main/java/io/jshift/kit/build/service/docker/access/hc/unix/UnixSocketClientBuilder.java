package io.jshift.kit.build.service.docker.access.hc.unix;

import io.jshift.kit.build.service.docker.access.hc.util.AbstractNativeClientBuilder;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.build.service.docker.access.hc.util.AbstractNativeClientBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;

public class UnixSocketClientBuilder extends AbstractNativeClientBuilder {

    public UnixSocketClientBuilder(String unixSocketPath, int maxConnections, KitLogger log) {
        super(unixSocketPath, maxConnections, log);
    }

    @Override
    protected ConnectionSocketFactory getConnectionSocketFactory() {
        return new UnixConnectionSocketFactory(path);
    }

    @Override
    protected String getProtocol() {
        return "unix";
    }
}
