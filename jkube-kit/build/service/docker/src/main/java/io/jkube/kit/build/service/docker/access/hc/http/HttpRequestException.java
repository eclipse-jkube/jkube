package io.jkube.kit.build.service.docker.access.hc.http;

import java.io.IOException;

public class HttpRequestException extends IOException {

    public HttpRequestException(String message) {
        super(message);
    }
}
