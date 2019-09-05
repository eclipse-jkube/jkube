package io.jshift.kit.build.api;

import java.io.IOException;
import java.util.Map;

import io.jshift.kit.config.image.ImageConfiguration;


/**
 * @author roland
 * @since 16.10.18
 */
public interface BuildService {
    void buildImage(ImageConfiguration imageConfig, BuildContext buildContext, Map<String, String> buildArgs)
        throws IOException;
}
