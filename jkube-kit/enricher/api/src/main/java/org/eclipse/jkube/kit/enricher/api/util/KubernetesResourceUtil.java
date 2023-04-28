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
package org.eclipse.jkube.kit.enricher.api.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KindFilenameMapperUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.InitContainerConfig;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceVersioning;

import com.fasterxml.jackson.core.type.TypeReference;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.FILENAME_PATTERN;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.convertToEnvVarList;

/**
 * Utility class for handling Kubernetes resource descriptors
 *
 * @author roland
 */
public class KubernetesResourceUtil {
    private KubernetesResourceUtil() { }


    public static final String API_VERSION = "v1";
    public static final String EXTENSIONS_VERSION = "extensions/v1beta1";
    public static final String API_APPS_VERSION = "apps/v1";
    public static final String API_NETWORKING_VERSION = "networking.k8s.io/v1";
    public static final String JOB_VERSION = "batch/v1";
    public static final String OPENSHIFT_V1_VERSION = "apps.openshift.io/v1";
    public static final String CRONJOB_VERSION = "batch/v1beta1";
    public static final String RBAC_VERSION = "rbac.authorization.k8s.io/v1";
    public static final String API_EXTENSIONS_VERSION = "apiextensions.k8s.io/v1";

    public static final ResourceVersioning DEFAULT_RESOURCE_VERSIONING = ResourceVersioning.builder()
        .withCoreVersion(API_VERSION)
        .withExtensionsVersion(EXTENSIONS_VERSION)
        .withAppsVersion(API_APPS_VERSION)
        .withNetworkingVersion(API_NETWORKING_VERSION)
        .withJobVersion(JOB_VERSION)
        .withOpenshiftV1version(OPENSHIFT_V1_VERSION)
        .withRbacVersion(RBAC_VERSION)
        .withCronJobVersion(CRONJOB_VERSION)
        .withApiExtensionsVersion(API_EXTENSIONS_VERSION)
        .build();

    private static final Set<Class<?>> SIMPLE_FIELD_TYPES = new HashSet<>();

    private static final String CONTAINER_NAME_REGEX = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$";

    static {
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(String.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Double.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Float.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Long.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Integer.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Short.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Character.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Byte.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(double.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(float.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(long.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(int.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(short.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(char.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(byte.class);
    }

    static final Map<String,String> FILENAME_TO_KIND_MAPPER = new HashMap<>();
    static final Map<String,String> KIND_TO_FILENAME_MAPPER = new HashMap<>();
    static final Set<String> EXCLUDED_RESOURCE_FILENAMES = new HashSet<>();

    static {
        initializeKindFilenameMapper();
        initializeExcludedResourceFilenames();
    }

    /**
     * Read all Kubernetes resource fragments from a directory and create a {@link KubernetesListBuilder} which
     * can be adapted later.
     *
     * @param platformMode platform whether it's Kubernetes/OpenShift
     * @param apiVersions the api versions to use
     * @param defaultName the default name to use when none is given
     * @param resourceFiles files to add.
     * @return the list builder
     * @throws IOException in case file is not found
     */
    public static KubernetesListBuilder readResourceFragmentsFrom(
        PlatformMode platformMode, ResourceVersioning apiVersions, String defaultName, File[] resourceFiles)
        throws IOException {

        final KubernetesListBuilder builder = new KubernetesListBuilder();
        if (resourceFiles != null) {
            List<File> filteredResourceFiles = getFilteredResourceFiles(resourceFiles);
            for (File file : filteredResourceFiles) {
                builder.addToItems(getResource(platformMode, apiVersions, file, defaultName));
            }
        }
        return builder;
    }

    /**
     * Read a Kubernetes resource fragment and add meta information extracted from the filename
     * to the resource descriptor. I.e. the following elements are added if not provided in the fragment:
     *
     * <ul>
     *     <li>name - Name of the resource added to metadata</li>
     *     <li>kind - Resource's kind</li>
     *     <li>apiVersion - API version (given as parameter to this method)</li>
     * </ul>
     *
     * @param platformMode Platform whether it's Kubernetes/OpenShift
     * @param apiVersions the API versions to add if not given.
     * @param file file to read.
     * @param appName resource name specifying resources belonging to this application
     * @return HasMetadata object for resource
     * @throws IOException in case file loading is failed
     */
    public static HasMetadata getResource(PlatformMode platformMode, ResourceVersioning apiVersions,
                                          File file, String appName) throws IOException {
        final Map<String, Object> fragment = readAndEnrichFragment(platformMode, apiVersions, file, appName);
        try {
            return Serialization.convertValue(fragment, HasMetadata.class);
        } catch (ClassCastException exp) {
            throw new IllegalArgumentException(String.format("Resource fragment %s has an invalid syntax (%s)", file.getPath(), exp.getMessage()));
        }
    }

    protected static void initializeKindFilenameMapper() {
        final Map<String, List<String>> mappings = KindFilenameMapperUtil.loadMappings();
        updateKindFilenameMapper(mappings);
    }

    private static void initializeExcludedResourceFilenames() {
        EXCLUDED_RESOURCE_FILENAMES.add(".helm.yaml");
    }

    protected static void remove(String kind, String filename) {
        FILENAME_TO_KIND_MAPPER.remove(filename);
        KIND_TO_FILENAME_MAPPER.remove(kind);
    }

    public static void updateKindFilenameMappings(List<MappingConfig> mappings) {
        if (mappings != null) {
            final Map<String, List<String>> mappingKindFilename = new HashMap<>();
            for (MappingConfig mappingConfig : mappings) {
                if (mappingConfig.isValid()) {
                    mappingKindFilename.put(mappingConfig.getKind(), Arrays.asList(mappingConfig.getFilenamesAsArray()));
                } else {
                    throw new IllegalArgumentException(String.format("Invalid mapping for Kind %s and Filename Types %s",
                      mappingConfig.getKind(), mappingConfig.getFilenameTypes()));
                }
            }
            updateKindFilenameMapper(mappingKindFilename);
        }
    }

    public static void updateKindFilenameMapper(final Map<String, List<String>> mappings) {

        final Set<Map.Entry<String, List<String>>> entries = mappings.entrySet();

        for (Map.Entry<String, List<String>> entry : entries) {

            final List<String> filenameTypes = entry.getValue();
            final String kind = entry.getKey();
            for (String filenameType : filenameTypes) {
                FILENAME_TO_KIND_MAPPER.put(filenameType, kind);
            }

            // In previous version, last one wins, so we do the same.
            KIND_TO_FILENAME_MAPPER.put(kind, filenameTypes.get(filenameTypes.size() - 1));

        }

    }

    // Read fragment and add default values
    private static Map<String, Object> readAndEnrichFragment(
        PlatformMode platformMode, ResourceVersioning apiVersions, File file, String appName) throws IOException {

        Matcher matcher = FILENAME_PATTERN.matcher(file.getName());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    String.format("Resource file name '%s' does not match pattern <name>-<type>.(yaml|yml|json)", file.getName()));
        }
        String name = matcher.group("name");
        String type = matcher.group("type");

        final Map<String,Object> fragment = Serialization.unmarshal(file, new TypeReference<Map<String, Object>>() {});

        final String kind;
        if (type != null) {
            kind = getAndValidateKindFromType(file, type);
        } else {
            // Try name as type
            kind = FILENAME_TO_KIND_MAPPER.get(name.toLowerCase());
            if (kind != null) {
                // Name is in fact the type, so lets erase the name.
                name = null;
            }
        }

        addKind(fragment, kind, file.getName());

        String apiVersion = apiVersions.getCoreVersion();

        switch ((String)fragment.get("kind")) {
            case "Ingress" :
                apiVersion = apiVersions.getExtensionsVersion();
                break;

            case "StatefulSet" :
            case "Deployment" :
                apiVersion = apiVersions.getAppsVersion();
                break;

            case "NetworkPolicy" :
                apiVersion = apiVersions.getNetworkingVersion();
                break;

            case "Job" :
                apiVersion = apiVersions.getJobVersion();
                break;

            case "DeploymentConfig" :
                apiVersion = platformMode == PlatformMode.openshift ? apiVersions.getOpenshiftV1version(): apiVersion;
                break;

            case "CronJob" :
                apiVersion = apiVersions.getCronJobVersion();
                break;

            case "CustomResourceDefinition":
                apiVersion = apiVersions.getApiExtensionsVersion();
                break;
            case "ClusterRole" :
            case "ClusterRoleBinding" :
            case "Role" :
            case "RoleBinding" :
                apiVersion = apiVersions.getRbacVersion();
                break;
        }

        fragment.putIfAbsent("apiVersion", apiVersion);

        Map<String, Object> metaMap = getMetadata(fragment);
        // No name means: generated app name should be taken as resource name
        String value = StringUtils.isNotBlank(name) ? name : appName;
        metaMap.putIfAbsent("name", value);

        return fragment;
    }

    private static String getAndValidateKindFromType(File file, String type) {
        String kind;
        kind = FILENAME_TO_KIND_MAPPER.get(type.toLowerCase());
        if (kind == null) {
            throw new IllegalArgumentException(
                    String.format("Unknown type '%s' for file %s. Must be one of : %s",
                            type, file.getName(), StringUtils.join(FILENAME_TO_KIND_MAPPER.keySet().iterator(), ", ")));
        }
        return kind;
    }

    private static void addKind(Map<String, Object> fragment, String kind, String fileName) {
        if (kind == null && !fragment.containsKey("kind")) {
            throw new IllegalArgumentException(
                    "No type given as part of the file name (e.g. 'app-rc.yml') " +
                            "and no 'Kind' defined in resource descriptor " + fileName);
        }
        fragment.putIfAbsent("kind", kind);
    }

    public static void removeItemFromKubernetesBuilder(KubernetesListBuilder builder, HasMetadata item) {
        List<HasMetadata> items = builder.buildItems();
        List<HasMetadata> newListItems = new ArrayList<>();
        for(HasMetadata listItem : items) {
            if(!listItem.equals(item)) {
                newListItems.add(listItem);
            }
        }
        builder.withItems(newListItems);
    }
    // ===============================================================================================

    private static Map<String, Object> getMetadata(Map<String, Object> fragment) {
        Object mo = fragment.get("metadata");
        Map<String, Object> meta;
        if (mo == null) {
            meta = new HashMap<>();
            fragment.put("metadata", meta);
            return meta;
        } else if (mo instanceof Map) {
            return (Map<String, Object>) mo;
        } else {
            throw new IllegalArgumentException("Metadata is expected to be a Map, not a " + mo.getClass());
        }
    }

    public static String getNameWithSuffix(String name, String kind) {
        String suffix =  KIND_TO_FILENAME_MAPPER.get(kind);
        return String.format("%s-%s", name, suffix != null ? suffix : "cr");
    }

    public static String extractContainerName(GroupArtifactVersion groupArtifactVersion, ImageConfiguration imageConfig) {
        String alias = imageConfig.getAlias();
        String containerName =  alias != null ? alias : extractImageUser(imageConfig.getName(), groupArtifactVersion.getGroupId()) + "-" + groupArtifactVersion.getArtifactId();
        if (!containerName.matches(CONTAINER_NAME_REGEX)) {
            return sanitizeName(containerName);
        }
        return containerName;
    }

    private static String extractImageUser(String image, String groupId) {
        ImageName name = new ImageName(image);
        String imageUser = name.getUser();
        if(imageUser != null) {
            return imageUser;
        } else {
            return groupId;
        }
    }

    public static boolean checkForKind(KubernetesListBuilder builder, String... kinds) {
        Set<String> kindSet = new HashSet<>(Arrays.asList(kinds));
        for (HasMetadata item : builder.buildItems()) {
            if (kindSet.contains(item.getKind())) {
                return true;
            }
        }
        return false;
    }


    public static void validateKubernetesMasterUrl(URL masterUrl) {
        if (masterUrl == null || StringUtils.isBlank(masterUrl.toString())) {
            throw new IllegalStateException("Cannot find Kubernetes master URL. Are you sure if you're connected to a remote cluster via `kubectl`?");
        }
    }

    public static void handleKubernetesClientException(KubernetesClientException e, KitLogger logger) {
        Throwable cause = e.getCause();
        if (cause instanceof UnknownHostException) {
            logger.error( "Could not connect to kubernetes cluster!");
            logger.error( "Connection error: %s", cause);

            String message = "Could not connect to kubernetes cluster. Are you sure if you're connected to a remote cluster via `kubectl`? Error: " + cause;
            throw new IllegalStateException(message, e);
        } else {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Uses reflection to copy over default values from the defaultValues object to the targetValues
     * object similar to the following:
     *
     * <code>
     * if( values.get${FIELD}() == null ) {
     *   values.(with|set){FIELD}(defaultValues.get${FIELD});
     * }
     * </code>
     *
     * Only fields that which use primitives, boxed primitives, or String object are copied.
     *
     * @param targetValues Object of target values
     * @param defaultValues Object of default values
     */
    public static void mergeSimpleFields(Object targetValues, Object defaultValues) {
        Class<?> tc = targetValues.getClass();
        Class<?> sc = defaultValues.getClass();
        for (Method targetGetMethod : tc.getMethods()) {
            if (!targetGetMethod.getName().startsWith("get")) {
                continue;
            }

            Class<?> fieldType = targetGetMethod.getReturnType();
            if (!isSimpleFieldType(fieldType)) {
                continue;
            }

            String fieldName = targetGetMethod.getName().substring(3);
            Method withMethod = null;
            try {
                withMethod = tc.getMethod("with" + fieldName, fieldType);
            } catch (NoSuchMethodException e) {
                try {
                    withMethod = tc.getMethod("set" + fieldName, fieldType);
                } catch (NoSuchMethodException e2) {
                    continue;
                }
            }

            Method sourceGetMethod = null;
            try {
                sourceGetMethod = sc.getMethod("get" + fieldName);
            } catch (NoSuchMethodException e) {
                continue;
            }

            try {
                if (targetGetMethod.invoke(targetValues) == null) {
                    withMethod.invoke(targetValues, sourceGetMethod.invoke(defaultValues));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }
    public static String mergePodSpec(PodSpecBuilder builder, PodSpec defaultPodSpec, String defaultName) {
        return mergePodSpec(builder, defaultPodSpec, defaultName, false);
    }

    public static String mergePodSpec(PodSpecBuilder builder, PodSpec defaultPodSpec, String defaultName, boolean sidecarEnabled) {
        // The default application container name is needed by plugin in order
        // to add ImageChange triggers in case of DeploymentConfig
        String defaultApplicationContainerName = null;
        List<Container> containers = builder.buildContainers();
        List<Container> defaultContainers = defaultPodSpec.getContainers();
        int size = defaultContainers.size();
        if (size > 0) {
            if (containers == null || containers.isEmpty()) {
                builder.addToContainers(defaultContainers.toArray(new Container[size]));
            } else {
                int idx = 0;
                for (Container defaultContainer : defaultContainers) {
                    Container container = null;
                    if(sidecarEnabled) { // Consider container as sidecar
                        for (Container fragmentContainer : containers) {
                            if (fragmentContainer.getName() == null || fragmentContainer.getName().equals(defaultContainer.getName())) {
                                container = fragmentContainer;
                                defaultApplicationContainerName = defaultContainer.getName();
                                break;
                            }
                        }
                        if (container == null) {
                            container = new Container();
                            containers.add(container);
                        }
                    } else { // Old behavior
                        if (idx < containers.size()) {
                            container = containers.get(idx);
                        } else {
                            container = new Container();
                            containers.add(container);
                        }
                    }
                    // If default container name is not set, add first found
                    // container as default application container from resource
                    // fragment, if not present set default application container
                    // name from JKube generated PodSpec
                    if (defaultApplicationContainerName == null) {
                        if (container.getName() != null) { // Pick from fragment
                            defaultApplicationContainerName = container.getName();
                        } else if (defaultContainer.getName() != null) { // Pick from default opinionated PodSpec
                            defaultApplicationContainerName = defaultContainer.getName();
                        }
                    }

                    mergeSimpleFields(container, defaultContainer);
                    List<EnvVar> defaultEnv = defaultContainer.getEnv();
                    if (defaultEnv != null) {
                        for (EnvVar envVar : defaultEnv) {
                            ensureHasEnv(container, envVar);
                        }
                    }
                    List<ContainerPort> defaultPorts = defaultContainer.getPorts();
                    if (defaultPorts != null) {
                        for (ContainerPort port : defaultPorts) {
                            ensureHasPort(container, port);
                        }
                    }
                    if (container.getReadinessProbe() == null) {
                        container.setReadinessProbe(defaultContainer.getReadinessProbe());
                    }
                    if (container.getLivenessProbe() == null) {
                        container.setLivenessProbe(defaultContainer.getLivenessProbe());
                    }
                    if (container.getSecurityContext() == null) {
                        container.setSecurityContext(defaultContainer.getSecurityContext());
                    }
                    idx++;
                }
                builder.withContainers(containers);
            }
        } else if (!containers.isEmpty()) {
            // lets default the container name if there's none specified in the custom yaml file
            for (Container container : containers) {
                if (StringUtils.isBlank(container.getName())) {
                    container.setName(defaultName);
                    break; // do it for one container only, but not necessarily the first one
                }
            }
            builder.withContainers(containers);
        }
        return defaultApplicationContainerName; // Return the main application container's name.
    }

    private static void ensureHasEnv(Container container, EnvVar envVar) {
        List<EnvVar> envVars = container.getEnv();
        if (envVars == null) {
            envVars = new ArrayList<>();
            container.setEnv(envVars);
        }
        for (EnvVar envVariable : envVars) {
            if (Objects.equals(envVariable.getName(), envVar.getName())) {
                // lets replace the object so that we can update the value or valueFrom
                envVars.remove(envVariable);
                envVars.add(envVar);
                return;
            }
        }
        envVars.add(envVar);
    }

    private static void ensureHasPort(Container container, ContainerPort port) {
        List<ContainerPort> ports = container.getPorts();
        if (ports == null) {
            ports = new ArrayList<>();
            container.setPorts(ports);
        }
        for (ContainerPort cp : ports) {
            String n1 = cp.getName();
            String n2 = port.getName();
            if (n1 != null && n2 != null && n1.equals(n2)) {
                return;
            }
            Integer p1 = cp.getContainerPort();
            Integer p2 = port.getContainerPort();
            if (p1 != null && p2 != null && p1.intValue() == p2.intValue()) {
                return;
            }
        }
        ports.add(port);
    }

    private static boolean isSimpleFieldType(Class<?> type) {
        return SIMPLE_FIELD_TYPES.contains(type);
    }

    /**
     * Merges the given resources together into a single resource.
     *
     * If switchOnLocalCustomisation is false then the overrides from item2 are merged into item1
     *
     * @param item1 item one
     * @param item2 item two
     * @param log KitLogger
     * @param switchOnLocalCustomisation boolean value for local customization
     * @return the newly merged resources
     */
    public static HasMetadata mergeResources(HasMetadata item1, HasMetadata item2, KitLogger log, boolean switchOnLocalCustomisation) {
        if (item1 instanceof Deployment && item2 instanceof Deployment) {
            return mergeDeployments((Deployment) item1, (Deployment) item2, log, switchOnLocalCustomisation);
        }
        if (item1 instanceof ConfigMap && item2 instanceof ConfigMap) {
            ConfigMap cm1 = (ConfigMap) item1;
            ConfigMap cm2 = (ConfigMap) item2;
            return mergeConfigMaps(cm1, cm2, log, switchOnLocalCustomisation);
        }
        mergeMetadata(item1, item2);
        return item1;
    }

    /**
     * Create a ConfigMap entry based on file contents
     *
     * @param key key for entry
     * @param file file path whose contents would be used in value of entry
     * @return an entry containing key and value
     * @deprecated Should be replaced with Fabric8 Kubernetes Client's methods
     * @throws IOException in case of error while reading file
     */
    @Deprecated
    public static Map.Entry<String, String> createConfigMapEntry(final String key, final Path file) throws IOException {
        final byte[] bytes = Files.readAllBytes(file);
        if (isFileWithBinaryContent(file)) {
            final String value = Base64.getEncoder().encodeToString(bytes);
            return new AbstractMap.SimpleEntry<>(key, value);
        } else {
            return new AbstractMap.SimpleEntry<>(key, new String(bytes));
        }
    }

    /**
     * Whether a file is binary file or not
     *
     * @param file file to check
     * @return boolean value indicating whether file is binary file or not
     * @deprecated Should be replaced with Fabric8 Kubernetes Client's methods
     * @throws IOException in case of failure while reading file
     */
    @Deprecated
    public static boolean isFileWithBinaryContent(final Path file) throws IOException {
        final byte[] bytes = Files.readAllBytes(file);
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes));
            return false;
        } catch (CharacterCodingException e) {
            return true;
        }
    }

    /**
     * Add ConfigMap entries from a directory to current ConfigMap
     * @param configMapBuilder ConfigMap builder object
     * @param path path to directory
     * @deprecated Should be replaced with Fabric8 Kubernetes Client's methods
     * @throws IOException in case of failure while reading directory
     */
    @Deprecated
    public static void addNewEntriesFromDirectoryToExistingConfigMap(ConfigMapBuilder configMapBuilder, final Path path)
        throws IOException {
        try (Stream<Path> files = Files.list(path)) {
            files.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).forEach(file -> {
                try {
                    addNewEntryToExistingConfigMap(configMapBuilder, createConfigMapEntry(file.getFileName().toString(), file), file);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            });
        }
    }

    /**
     * Add single entry to ConfigMap
     *
     * @param configMapBuilder ConfigMap builder object
     * @param entry key value pair which will be added to data/binaryData
     * @param file file which needs to be processed
     * @deprecated Should be replaced with Fabric8 Kubernetes Client's methods
     * @throws IOException in case of failure while reading file
     */
    @Deprecated
    public static void addNewEntryToExistingConfigMap(ConfigMapBuilder configMapBuilder, Map.Entry<String, String> entry, final Path file)
        throws IOException {
        if (isFileWithBinaryContent(file)) {
            configMapBuilder.addToBinaryData(entry.getKey(), entry.getValue());
        } else {
            configMapBuilder.addToData(entry.getKey(), entry.getValue());
        }
    }

    public static void addNewConfigMapEntriesToExistingConfigMap(ConfigMapBuilder configMapBuilder, String key, Path filePath) throws IOException {
        if (Files.isDirectory(filePath, LinkOption.NOFOLLOW_LINKS)) {
            addNewEntriesFromDirectoryToExistingConfigMap(configMapBuilder, filePath);
        } else {
            addNewEntryToExistingConfigMap(configMapBuilder, createConfigMapEntry(key, filePath), filePath);
        }
    }

    public static boolean hasInitContainer(PodTemplateSpecBuilder builder, String name) {
        return getInitContainer(builder, name) != null;
    }

    public static Container getInitContainer(PodTemplateSpecBuilder builder, String name) {
        if (Boolean.TRUE.equals(builder.hasSpec())) {
            List<Container> initContainerList = builder.buildSpec().getInitContainers();
            for(Container initContainer : initContainerList) {
                if(initContainer.getName().equals(name)) {
                    return initContainer;
                }
            }
        }
        return null;
    }

    public static void removeInitContainer(PodTemplateSpecBuilder builder, String initContainerName) {
        Container initContainer = getInitContainer(builder, initContainerName);
        if (initContainer != null) {
            List<Container> initContainers = builder.buildSpec().getInitContainers();
            initContainers.remove(initContainer);
            builder.editSpec().withInitContainers(initContainers).endSpec();
        }
    }

    public static void appendInitContainer(PodTemplateSpecBuilder builder, Container initContainer, KitLogger log) {
        String name = initContainer.getName();
        Container existing = getInitContainer(builder, name);
        if (existing != null) {
            if (existing.equals(initContainer)) {
                log.warn("Trying to add init-container %s a second time. Ignoring ....", name);
                return;
            } else {
                throw new IllegalArgumentException(
                    String.format("PodSpec %s already contains a different init container with name %s but can not add a second one with the same name. " +
                            "Please choose a different name for the init container",
                        builder.build().getMetadata().getName(), name));
            }
        }

        ensureSpec(builder);
        builder.editSpec().addToInitContainers(initContainer).endSpec();
    }

    public static List<Container> createNewInitContainersFromConfig(List<InitContainerConfig> initContainerConfigs) {
        List<Container> initContainers = new ArrayList<>();
        for (InitContainerConfig initContainerConfig : initContainerConfigs) {
            initContainers.add(createNewInitContainerFromConfig(initContainerConfig));
        }
        return initContainers;
    }

    public static Container createNewInitContainerFromConfig(InitContainerConfig initContainerConfig) {
        ContainerBuilder containerBuilder =  new ContainerBuilder();
        if (StringUtils.isNotBlank(initContainerConfig.getName())) {
            containerBuilder.withName(initContainerConfig.getName());
        }
        if (StringUtils.isNotBlank(initContainerConfig.getImageName())) {
            containerBuilder.withImage(initContainerConfig.getImageName());
        }
        if (StringUtils.isNotBlank(initContainerConfig.getImagePullPolicy())) {
            containerBuilder.withImagePullPolicy(initContainerConfig.getImagePullPolicy());
        }
        if (initContainerConfig.getCmd() != null) {
            containerBuilder.withCommand(initContainerConfig.getCmd().asStrings());
        }
        if (initContainerConfig.getVolumes() != null && !initContainerConfig.getVolumes().isEmpty()) {
            containerBuilder.withVolumeMounts(createVolumeMountsFromConfig(initContainerConfig.getVolumes()));
        }
        if (initContainerConfig.getEnv() != null && !initContainerConfig.getEnv().isEmpty()) {
            containerBuilder.withEnv(convertToEnvVarList(initContainerConfig.getEnv()));
        }

        return containerBuilder.build();
    }

    public static boolean isContainerImage(ImageConfiguration imageConfig, ControllerResourceConfig config) {
        return imageConfig.getBuildConfiguration() != null && !isInitContainerImage(imageConfig, config);
    }

    public static boolean isInitContainerImage(ImageConfiguration imageConfiguration, ControllerResourceConfig config) {
        if (config.getInitContainers() != null && !config.getInitContainers().isEmpty()) {
            return config.getInitContainers()
                .stream()
                .map(InitContainerConfig::getImageName)
                .collect(Collectors.toSet())
                .contains(imageConfiguration.getName());
        }
        return false;
    }

    private static void ensureSpec(PodTemplateSpecBuilder obj) {
        if (obj.buildSpec() == null) {
            obj.withNewSpec().endSpec();
        }
    }

    private static List<VolumeMount> createVolumeMountsFromConfig(List<VolumeConfig> volumeConfigs) {
        List<VolumeMount> volumeMounts = new ArrayList<>();
        for (VolumeConfig vc : volumeConfigs) {
            VolumeMountBuilder volumeMountBuilder = new VolumeMountBuilder();
            if (StringUtils.isNotBlank(vc.getName())) {
                volumeMountBuilder.withName(vc.getName());
            }
            if (StringUtils.isNotBlank(vc.getPath())) {
                volumeMountBuilder.withMountPath(vc.getPath());
            }
            volumeMounts.add(volumeMountBuilder.build());
        }
        return volumeMounts;
    }

    protected static HasMetadata mergeConfigMaps(ConfigMap cm1, ConfigMap cm2, KitLogger log, boolean switchOnLocalCustomisation) {
        ConfigMap cm1OrCopy = cm1;
        if (!switchOnLocalCustomisation) {
            // lets copy the original to avoid modifying it
            cm1OrCopy = new ConfigMapBuilder(cm1OrCopy).build();
        }

        log.info("Merging 2 resources for " + KubernetesHelper.getKind(cm1OrCopy) + " " + KubernetesHelper.getName(cm1OrCopy));
        cm1OrCopy.setData(mergeMapsAndRemoveEmptyStrings(cm2.getData(), cm1OrCopy.getData()));
        mergeMetadata(cm1OrCopy, cm2);
        return cm1OrCopy;
    }

    protected static HasMetadata mergeDeployments(Deployment resource1, Deployment resource2, KitLogger log, boolean switchOnLocalCustomisation) {
        Deployment resource1OrCopy = resource1;
        if (!switchOnLocalCustomisation) {
            // lets copy the original to avoid modifying it
            resource1OrCopy = new DeploymentBuilder(resource1OrCopy).build();
        }
        HasMetadata answer = resource1OrCopy;
        DeploymentSpec spec1 = resource1OrCopy.getSpec();
        DeploymentSpec spec2 = resource2.getSpec();
        if (spec1 == null) {
            resource1OrCopy.setSpec(spec2);
        } else {
            PodTemplateSpec template1 = spec1.getTemplate();
            PodTemplateSpec template2 = null;
            if (spec2 != null) {
                template2 = spec2.getTemplate();
            }
            if (template1 != null && template2 != null) {
                mergeMetadata(template1, template2);
            }
            if (template1 == null) {
                spec1.setTemplate(template2);
            } else {
                PodSpec podSpec1 = template1.getSpec();
                PodSpec podSpec2 = null;
                if (template2 != null) {
                    podSpec2 = template2.getSpec();
                }
                if (podSpec1 == null) {
                    template1.setSpec(podSpec2);
                } else {
                    String defaultName = null;
                    PodTemplateSpec updateTemplate = template1;
                    if (switchOnLocalCustomisation) {
                        HasMetadata override = resource2;
                        if (isLocalCustomisation(podSpec1)) {
                            updateTemplate = template2;
                            PodSpec tmp = podSpec1;
                            podSpec1 = podSpec2;
                            podSpec2 = tmp;
                        } else {
                            answer = resource2;
                            override = resource1OrCopy;
                        }
                        mergeMetadata(answer, override);
                    } else {
                        mergeMetadata(resource1OrCopy, resource2);
                    }
                    if (updateTemplate != null) {
                        if (podSpec2 == null) {
                            updateTemplate.setSpec(podSpec1);
                        } else {
                            PodSpecBuilder podSpecBuilder = new PodSpecBuilder(podSpec1);
                            mergePodSpec(podSpecBuilder, podSpec2, defaultName);
                            updateTemplate.setSpec(podSpecBuilder.build());
                        }
                    }
                    return answer;
                }
            }
        }
        log.info("Merging 2 resources for " + KubernetesHelper.getKind(resource1OrCopy) + " " + KubernetesHelper.getName(resource1OrCopy));
        return resource1OrCopy;
    }

    private static void mergeMetadata(PodTemplateSpec item1, PodTemplateSpec item2) {
        if (item1 != null && item2 != null) {
            ObjectMeta metadata1 = item1.getMetadata();
            ObjectMeta metadata2 = item2.getMetadata();
            if (metadata1 == null) {
                item1.setMetadata(metadata2);
            } else if (metadata2 != null) {
                metadata1.setAnnotations(mergeMapsAndRemoveEmptyStrings(metadata2.getAnnotations(), metadata1.getAnnotations()));
                metadata1.setLabels(mergeMapsAndRemoveEmptyStrings(metadata2.getLabels(), metadata1.getLabels()));
            }
        }
    }

    public static void mergeMetadata(HasMetadata item1, HasMetadata item2) {
        if (item1 != null && item2 != null) {
            ObjectMeta metadata1 = item1.getMetadata();
            ObjectMeta metadata2 = item2.getMetadata();
            if (metadata1 == null) {
                item1.setMetadata(metadata2);
            } else if (metadata2 != null) {
                metadata1.setAnnotations(mergeMapsAndRemoveEmptyStrings(metadata2.getAnnotations(), metadata1.getAnnotations()));
                metadata1.setLabels(mergeMapsAndRemoveEmptyStrings(metadata2.getLabels(), metadata1.getLabels()));
            }
        }
    }

    private static List<File> getFilteredResourceFiles(File[] resourceFiles) {
        List<File> filteredResourceFiles = new ArrayList<>();
        for (File resourceFile : resourceFiles) {
            boolean hasNoExcludedFileNamePattern = EXCLUDED_RESOURCE_FILENAMES.stream()
                .noneMatch(s -> resourceFile.getName().endsWith(s));
            if (hasNoExcludedFileNamePattern) {
                filteredResourceFiles.add(resourceFile);
            }
        }
        return filteredResourceFiles;
    }

    /**
     * Returns a merge of the given maps and then removes any resulting empty string values (which is the way to remove, say, a label or annotation
     * when overriding
     */
    private static Map<String, String> mergeMapsAndRemoveEmptyStrings(Map<String, String> overrideMap, Map<String, String> originalMap) {
        Map<String, String> answer = MapUtil.mergeMaps(overrideMap, originalMap);
        if (overrideMap != null && originalMap != null) {
            Set<Map.Entry<String, String>> entries = overrideMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String value = entry.getValue();
                if (value == null || value.isEmpty()) {
                    String key = entry.getKey();
                    answer.remove(key);
                }
            }
        }
        return answer;
    }

    // lets use presence of an image name as a clue that we are just enriching things a little
    // rather than a full complete manifest
    // we could also use an annotation?
    private static boolean isLocalCustomisation(PodSpec podSpec) {
        List<Container> containers = podSpec.getContainers() != null ? podSpec.getContainers() : Collections.<Container>emptyList();
        for (Container container : containers) {
            if (StringUtils.isNotBlank(container.getImage())) {
                return false;
            }
        }
        return true;
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-]", "").replaceFirst("^-*(.*?)-*$","$1");
    }
}
