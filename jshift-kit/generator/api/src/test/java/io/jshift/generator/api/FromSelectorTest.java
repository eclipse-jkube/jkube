package io.jshift.generator.api;

import java.util.Map;

import io.jshift.kit.common.KitLogger;
import io.jshift.kit.config.image.build.OpenShiftBuildStrategy;
import io.jshift.kit.config.resource.RuntimeMode;
import io.jshift.kit.config.resource.ProcessorConfig;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static io.jshift.kit.config.image.build.OpenShiftBuildStrategy.s2i;
import static io.jshift.kit.config.image.build.OpenShiftBuildStrategy.docker;
import static io.jshift.kit.config.resource.RuntimeMode.openshift;
import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 12/08/16
 */
public class FromSelectorTest {

    @Mocked
    MavenProject project;

    @Mocked
    Plugin plugin;

    @Mocked
    KitLogger logger;

    @Test
    public void simple() {
        final Object[] data = new Object[] {
                openshift, s2i, "1.2.3.redhat-00009", "redhat-s2i-prop", "redhat-istag-prop",
                openshift, docker, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, s2i, "1.2.3.fuse-00009", "redhat-s2i-prop", "redhat-istag-prop",
                openshift, docker, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, s2i, "1.2.3.foo-00009", "s2i-prop", "istag-prop",
                openshift, docker, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                openshift, s2i, "1.2.3", "s2i-prop", "istag-prop",
                openshift, docker, "1.2.3", "docker-prop", "istag-prop",
                null, s2i, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, docker, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, s2i, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, docker, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, s2i, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                null, docker, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                null, s2i, "1.2.3", "docker-prop", "istag-prop",
                null, docker, "1.2.3", "docker-prop", "istag-prop",
                openshift, null, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, null, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, null, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                openshift, null, "1.2.3", "docker-prop", "istag-prop"
        };

        for (int i = 0; i < data.length; i += 5) {
            GeneratorContext ctx = new GeneratorContext.Builder()
                    .project(project)
                    .config(new ProcessorConfig())
                    .logger(logger)
                    .runtimeMode((RuntimeMode) data[i])
                    .strategy((OpenShiftBuildStrategy) data[i + 1])
                    .build();

            final String version = (String) data[i + 2];
            new Expectations() {{
                plugin.getVersion(); result = version;
            }};

            FromSelector selector = new FromSelector.Default(ctx, "test");
            assertEquals(data[i + 3], selector.getFrom());
            Map<String, String> fromExt = selector.getImageStreamTagFromExt();
            assertEquals(fromExt.size(),3);
            assertEquals(fromExt.get(OpenShiftBuildStrategy.SourceStrategy.kind.key()), "ImageStreamTag");
            assertEquals(fromExt.get(OpenShiftBuildStrategy.SourceStrategy.namespace.key()), "openshift");
            assertEquals(fromExt.get(OpenShiftBuildStrategy.SourceStrategy.name.key()), data[i + 4]);
        }
    }

}
