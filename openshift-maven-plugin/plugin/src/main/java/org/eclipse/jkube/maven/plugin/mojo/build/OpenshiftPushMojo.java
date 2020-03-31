package org.eclipse.jkube.maven.plugin.mojo.build;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.AnsiLogger;

import static org.eclipse.jkube.maven.plugin.mojo.Openshift.DEFAULT_LOG_PREFIX;

@Mojo(name = "push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class OpenshiftPushMojo extends PushMojo {
    @Override
    protected String getLogPrefix() {
        return DEFAULT_LOG_PREFIX;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLogger();
        log.warn("Image is pushed to OpenShift's internal registry during oc:build goal." +
                " Skipping...");
    }

    private KitLogger getLogger() {
        return new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(), getLogPrefix() + " ");
    }
}
