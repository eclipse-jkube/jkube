package io.jkube.kit.build.maven;

import java.util.Properties;

import org.apache.maven.execution.MavenSession;

/**
 * @author roland
 * @since 17.10.18
 */
public class MavenCacheBackend implements ImagePullCache.Backend {

    private MavenSession session;

    public MavenCacheBackend(MavenSession session) {
        this.session = session;
    }

    @Override
    public String get(String key) {
        Properties userProperties = session.getUserProperties();
        return userProperties.getProperty(key);
    }

    @Override
    public void put(String key, String value) {
        Properties userProperties = session.getUserProperties();
        userProperties.setProperty(key, value);
    }
}
