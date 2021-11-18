/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for dealing with profiles.
 *
 * @author roland
 * @since 25/07/16
 */
public class ProfileUtil {

    private ProfileUtil() {}

    private static final Logger log = LoggerFactory.getLogger(ProfileUtil.class);

    // Allowed profile names
    private static final String[] PROFILE_FILENAMES = {"profiles%s.yml", "profiles%s.yaml", "profiles%s"};

    // Default profile which will be always there
    public static final String DEFAULT_PROFILE = "default";

    /**
     * Find a profile. Profiles are looked up at various locations:
     *
     * <ul>
     *     <li>A given directory with the name profiles.yml (and variations, {@link #findProfile(String, File)}</li>
     * </ul>
     * @param profileArg the profile's name
     * @param resourceDir a directory to check for profiles.
     * @return the profile found or the default profile if none of this name is given
     * @throws IOException
     */
    public static Profile findProfile(String profileArg, File resourceDir) throws IOException {
        try {
            String profile = profileArg == null ? DEFAULT_PROFILE : profileArg;
            Profile profileFound = lookup(profile, resourceDir);
            if (profileFound != null) {
                if(profileFound.getParentProfile() != null) {
                    profileFound = inheritFromParentProfile(profileFound, resourceDir);
                    log.info(profileFound + " inheriting resources from " + profileFound.getParentProfile());
                }
                return profileFound;
            } else {
                throw new IllegalArgumentException("No profile '" + profile + "' defined");
            }
        } catch (IOException e) {
            throw new IOException("Error while looking up profile " + profileArg + ": " + e.getMessage(),e);
        }
    }

    private static Profile inheritFromParentProfile(Profile aProfile, File resourceDir) throws IOException {
        Profile aParentProfile = lookup(aProfile.getParentProfile(), resourceDir);
        if(aParentProfile != null) {
            aProfile.setEnricherConfig(ProcessorConfig.mergeProcessorConfigs(aProfile.getEnricherConfig(), aParentProfile.getEnricherConfig()));
            aProfile.setGeneratorConfig(ProcessorConfig.mergeProcessorConfigs(aProfile.getGeneratorConfig(), aParentProfile.getGeneratorConfig()));
            aProfile.setWatcherConfig(ProcessorConfig.mergeProcessorConfigs(aProfile.getWatcherConfig(), aParentProfile.getWatcherConfig()));
        } else {
            throw new IllegalArgumentException("No parent profile '" + aProfile.getParentProfile() + "' defined");
        }
        return aProfile;
    }

    /**
     * Find an enricher or generator config, possibly via a profile and merge it with a given configuration.
     *
     * @param configExtractor how to extract the config from a profile when found
     * @param profile the profile name (can be null, then no profile is used)
     * @param resourceDir resource directory where to lookup the profile (in addition to a classpath lookup)
     * @return the merged configuration which can be empty if no profile is given
     * @param config the provided configuration
     * @throws IOException
     */
    public static ProcessorConfig blendProfileWithConfiguration(ProcessorConfigurationExtractor configExtractor,
                                                                String profile,
                                                                File resourceDir,
                                                                ProcessorConfig config) throws IOException {
        // Get specified profile or the default profile
        ProcessorConfig profileConfig = extractProcesssorConfiguration(configExtractor, profile, resourceDir);

        return ProcessorConfig.mergeProcessorConfigs(config, profileConfig);
    }


    /**
     * Lookup profiles from a given directory and merge it with a profile of the
     * same name found in the classpath
     *
     * @param name name of the profile to lookup
     * @param directory directory to lookup
     * @return Profile found or null
     * @throws IOException if somethings fails during lookup
     */
    public static Profile lookup(String name, File directory) throws IOException {
        // First check from the classpath, these profiles are used as a basis
        List<Profile> profiles = readProfileFromClasspath(name);

        File profileFile = findProfileYaml(directory);
        if (profileFile != null) {
            List<Profile> fileProfiles = fromYaml(new FileInputStream(profileFile));
            for (Profile profile : fileProfiles) {
                if (profile.getName().equals(name)) {
                    profiles.add(profile);
                    break;
                }
            }
        }
        // "larger" orders are "earlier" in the list
        profiles.sort(Collections.reverseOrder());
        return mergeProfiles(profiles);
    }

    private static ProcessorConfig extractProcesssorConfiguration(ProcessorConfigurationExtractor extractor,
                                                                 String profile,
                                                                 File resourceDir) throws IOException {
        Profile profileFound = findProfile(profile, resourceDir);
        return extractor.extract(profileFound);
    }


    private static Profile mergeProfiles(List<Profile> profiles) {
        Profile ret = null;
        for (Profile profile : profiles) {
            if (profile != null) {
                if (ret == null) {
                    ret = new Profile(profile);
                } else {
                    ret = new Profile(ret, profile);
                }
            }
        }
        return ret;
    }

    // Read all default profiles first, then merge in custom profiles found on the classpath
    private static List<Profile> readProfileFromClasspath(String name) throws IOException {
        List<Profile> ret = new ArrayList<>();
        ret.addAll(readAllFromClasspath(name, "default"));
        ret.addAll(readAllFromClasspath(name, ""));
        return ret;
    }

    /**
     * Read all profiles found in the classpath.
     *
     * @param name name of the profile to lookup
     * @param ext to use (e.g. 'default' for checking 'profile-default.yml'. Can also be null or empty.
     * @return all profiles with this name stored in files with this extension
     *
     * @throws IOException if reading of a profile fails
     */
    public static List<Profile> readAllFromClasspath(String name, String ext) throws IOException {
        List<Profile > ret = new ArrayList<>();
        for (String location : getMetaInfProfilePaths(ext)) {
            for (String url : ClassUtil.getResources(location)) {
                for (Profile profile : fromYaml(new URL(url).openStream())) {
                    if (name.equals(profile.getName())) {
                        ret.add(profile);
                    }
                }
            }
        }
        return ret;
    }

    // ================================================================================

    // check for various variations of profile files
    private static File findProfileYaml(File directory) {
        for (String profileFile : PROFILE_FILENAMES) {
            File ret = new File(directory, String.format(profileFile, ""));
            if (ret.exists()) {
                return ret;
            }
        }
        return null;
    }

    // prepend meta-inf location
    private static List<String> getMetaInfProfilePaths(String ext) {
        List<String> ret = new ArrayList<>(PROFILE_FILENAMES.length);
        for (String p : PROFILE_FILENAMES) {
            ret.add("META-INF/jkube/" + getProfileFileName(p,ext));
        }
        return ret;
    }

    private static String getProfileFileName(String fileName, String ext) {
        return String.format(fileName, StringUtils.isNotBlank(ext) ? "-" + ext : "");
    }

    /**
     * Load a profile from an input stream. This must be in YAML format
     *
     * @param is inputstream to read the profile from
     * @return the de-serialized profile
     * @throws IOException if deserialization fails
     */
    public static List<Profile> fromYaml(InputStream is) throws IOException {
        TypeReference<List<Profile>> typeRef = new TypeReference<List<Profile>>() {};
        return Serialization.yamlMapper().readValue(is, typeRef);
    }

    // ================================================================================

    // Use to select either a generator or enricher config
    public interface ProcessorConfigurationExtractor {
        ProcessorConfig extract(Profile profile);
    }

    /**
     * Get the generator configuration
     */
    public static final ProcessorConfigurationExtractor GENERATOR_CONFIG = Profile::getGeneratorConfig;

    /**
     * Get the enricher configuration
     */
    public static final ProcessorConfigurationExtractor ENRICHER_CONFIG = Profile::getEnricherConfig;

    /**
     * Get the watcher configuration
     */
    public static final ProcessorConfigurationExtractor WATCHER_CONFIG = Profile::getWatcherConfig;

}
