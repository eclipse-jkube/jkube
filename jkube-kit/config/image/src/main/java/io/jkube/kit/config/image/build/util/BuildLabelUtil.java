package io.jkube.kit.config.image.build.util;

import io.jkube.kit.common.PrefixedLogger;
import io.jkube.kit.common.util.GitUtil;
import io.jkube.kit.config.image.build.BuildConfiguration;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class BuildLabelUtil {

    private static String getDocumentationUrl (MavenProject project) {
        while (project != null) {
            DistributionManagement distributionManagement = project.getDistributionManagement();
            if (distributionManagement != null) {
                Site site = distributionManagement.getSite();
                if (site != null) {
                    return site.getUrl();
                }
            }
            project = project.getParent();
        }
        return null;
    }

    public static void addSchemaLabels (BuildConfiguration.Builder buildBuilder, MavenProject project, PrefixedLogger log) {
        String LABEL_SCHEMA_VERSION = "1.0";
        String GIT_REMOTE = "origin";
        String docURL = getDocumentationUrl(project);
        Map<String, String> labels = new HashMap<>();

        labels.put(BuildLabelAnnotations.BUILD_DATE.value(), LocalDateTime.now().toString());
        labels.put(BuildLabelAnnotations.NAME.value(), project.getName());
        labels.put(BuildLabelAnnotations.DESCRIPTION.value(), project.getDescription());
        if (docURL != null) {
            labels.put(BuildLabelAnnotations.USAGE.value(), docURL);
        }
        if (project.getUrl() != null) {
            labels.put(BuildLabelAnnotations.URL.value(), project.getUrl());
        }
        if (project.getOrganization() != null && project.getOrganization().getName() != null) {
            labels.put(BuildLabelAnnotations.VENDOR.value(), project.getOrganization().getName());
        }
        labels.put(BuildLabelAnnotations.VERSION.value(), project.getVersion());
        labels.put(BuildLabelAnnotations.SCHEMA_VERSION.value(), LABEL_SCHEMA_VERSION);

        try {
            Repository repository = GitUtil.getGitRepository(project.getBasedir());
            if (repository != null) {
                String commitID = GitUtil.getGitCommitId(repository);
                labels.put(BuildLabelAnnotations.VCS_REF.value(), commitID);
                String gitRemoteUrl = repository.getConfig().getString("remote", GIT_REMOTE, "url");
                if (gitRemoteUrl != null) {
                    labels.put(BuildLabelAnnotations.VCS_URL.value(), gitRemoteUrl);
                } else {
                    log.verbose("Could not detect any git remote");
                }
            }
        } catch (IOException | GitAPIException | NullPointerException e) {
            log.error("Cannot extract Git information: " + e, e);
        } finally {
            buildBuilder.labels(labels);
        }
    }
}
