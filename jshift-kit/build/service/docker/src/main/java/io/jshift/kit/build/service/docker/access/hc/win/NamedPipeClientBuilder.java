package io.jshift.kit.build.service.docker.access.hc.win;

import io.jshift.kit.build.service.docker.access.hc.util.AbstractNativeClientBuilder;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.build.service.docker.access.hc.util.AbstractNativeClientBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;

public class NamedPipeClientBuilder extends AbstractNativeClientBuilder {
    public NamedPipeClientBuilder(String namedPipePath, int maxConnections, KitLogger log) {
        super(namedPipePath, maxConnections, log);
    }

    @Override
    protected ConnectionSocketFactory getConnectionSocketFactory() {
        return new NpipeConnectionSocketFactory(path, log);
    }

    @Override
    protected String getProtocol() {
        return "npipe";
    }
}
