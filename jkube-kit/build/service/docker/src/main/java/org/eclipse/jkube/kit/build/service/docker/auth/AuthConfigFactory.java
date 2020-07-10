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
package org.eclipse.jkube.kit.build.service.docker.auth;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.ecr.EcrExtendedAuth;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory for creating docker specific authentication configuration
 *
 * @author roland
 * @since 29.07.14
 */
public class AuthConfigFactory {

    // Properties for specifying username, password (can be encrypted), email and authtoken (not used yet)
    // + whether to check for OpenShift authentication
    public static final String AUTH_USERNAME = "username";
    public static final String AUTH_PASSWORD = "password";
    public static final String AUTH_EMAIL = "email";
    public static final String AUTH_AUTHTOKEN = "authToken";
    protected static final String AUTH_USE_OPENSHIFT_AUTH = "useOpenShiftAuth";

    static final String DOCKER_LOGIN_DEFAULT_REGISTRY = "https://index.docker.io/v1/";

    private final KitLogger log;
    private static final String[] DEFAULT_REGISTRIES = new String[]{
            "docker.io", "index.docker.io", "registry.hub.docker.com"
    };

    public AuthConfigFactory(KitLogger log) {
        this.log = log;
    }

    /**
     * Create an authentication config object which can be used for communication with a Docker registry
     *
     * The authentication information is looked up at various places (in this order):
     *
     * <ul>
     *    <li>From system properties</li>
     *    <li>From the provided map which can contain key-value pairs</li>
     *    <li>From the openshift settings in ~/.config/kube</li>
     *    <li>From the Maven settings stored typically in ~/.m2/settings.xml</li>
     *    <li>From the Docker settings stored in ~/.docker/config.json</li>
     * </ul>
     *
     * The following properties (prefix with 'docker.') and config key are evaluated:
     *
     * <ul>
     *     <li>username: User to authenticate</li>
     *     <li>password: Password to authenticate. Can be encrypted</li>
     *     <li>email: Optional EMail address which is send to the registry, too</li>
     * </ul>
     *
     *  If the repository is in an aws ecr registry and skipExtendedAuth is not true, if found
     *  credentials are not from docker settings, they will be interpreted as iam credentials
     *  and exchanged for ecr credentials.
     *
     * @param isPush if true this AuthConfig is created for a push, if false it's for a pull
     * @param skipExtendedAuth if false, do not execute extended authentication methods
     * @param authConfig String-String Map holding configuration info from the plugin's configuration. Can be <code>null</code> in
     *                   which case the settings are consulted.
     * @param settings the global Maven settings object
     * @param user user to check for
     * @param registry registry to use, might be null in which case a default registry is checked,
     * @param passwordDecryptionMethod a functional interface to customize how password should be decoded
     * @return the authentication configuration or <code>null</code> if none could be found
     *
     * @throws IOException mojo failure exception
     */
    public AuthConfig createAuthConfig(boolean isPush, boolean skipExtendedAuth, Map authConfig, List<RegistryServerConfiguration> settings, String user, String registry, UnaryOperator<String> passwordDecryptionMethod)
            throws IOException {

        AuthConfig ret = createStandardAuthConfig(isPush, authConfig, settings, user, registry, passwordDecryptionMethod, log);
        if (ret != null) {
            if (registry == null || skipExtendedAuth) {
                return ret;
            }
            try {
                return extendedAuthentication(ret, registry);
            } catch (IOException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        // Finally check ~/.docker/config.json
        ret = getAuthConfigFromDockerConfig(registry, log);
        if (ret != null) {
            log.debug("AuthConfig: credentials from ~/.docker/config.json");
            return ret;
        }

        log.debug("AuthConfig: no credentials found");
        return null;
    }


    /**
     * Try various extended authentication method.  Currently only supports amazon ECR
     *
     * @param standardAuthConfig The locally stored credentials.
     * @param registry The registry to authenticated against.
     * @return The given credentials, if registry does not need extended authentication;
     * else, the credentials after authentication.
     * @throws IOException
     */
    private AuthConfig extendedAuthentication(AuthConfig standardAuthConfig, String registry) throws IOException {
        EcrExtendedAuth ecr = new EcrExtendedAuth(log, registry);
        if (ecr.isAwsRegistry()) {
            return ecr.extendedAuth(standardAuthConfig);
        }
        return standardAuthConfig;
    }

    /**
     * Create an authentication config object which can be used for communication with a Docker registry
     *
     * The authentication information is looked up at various places (in this order):
     *
     * <ul>
     *    <li>From system properties</li>
     *    <li>From the provided map which can contain key-value pairs</li>
     *    <li>From the openshift settings in ~/.config/kube</li>
     *    <li>From the Maven settings stored typically in ~/.m2/settings.xml</li>
     * </ul>
     *
     * The following properties (prefix with 'docker.') and config key are evaluated:
     *
     * <ul>
     *     <li>username: User to authenticate</li>
     *     <li>password: Password to authenticate. Can be encrypted</li>
     *     <li>email: Optional EMail address which is send to the registry, too</li>
     * </ul>
     *
     *
     * @param isPush if true this AuthConfig is created for a push, if false it's for a pull
     * @param authConfigMap String-String Map holding configuration info from the plugin's configuration. Can be <code>null</code> in
     *                   which case the settings are consulted.
     * @param settings the global Maven settings object
     * @param user user to check for
     * @param registry registry to use, might be null in which case a default registry is checked,
     * @param passwordDecryptionMethod a function to customize how password should be decoded
     * @param log Kit logger
     * @return the authentication configuration or <code>null</code> if none could be found
     *
     * @throws IOException any exception in case of fetching authConfig
     */
    private static AuthConfig createStandardAuthConfig(boolean isPush, Map authConfigMap, List<RegistryServerConfiguration> settings, String user, String registry, UnaryOperator<String> passwordDecryptionMethod, KitLogger log)
            throws IOException {
        AuthConfig ret;

        // Check first for specific configuration based on direction (pull or push), then for a default value
        for (LookupMode lookupMode : new LookupMode[] { getLookupMode(isPush), LookupMode.DEFAULT }) {
            // System properties docker.username and docker.password always take precedence
            ret = getAuthConfigFromSystemProperties(lookupMode, passwordDecryptionMethod);
            if (ret != null) {
                log.debug("AuthConfig: credentials from system properties");
                return ret;
            }

            // Check for openshift authentication either from the plugin config or from system props
            ret = getAuthConfigFromOpenShiftConfig(lookupMode, authConfigMap);
            if (ret != null) {
                log.debug("AuthConfig: OpenShift credentials");
                return ret;
            }

            // Get configuration from global plugin config
            ret = getAuthConfigFromPluginConfiguration(lookupMode, authConfigMap, passwordDecryptionMethod);
            if (ret != null) {
                log.debug("AuthConfig: credentials from plugin config");
                return ret;
            }
        }

        // ===================================================================
        // These are lookups based on registry only, so the direction (push or pull) doesn't matter:

        // Now lets lookup the registry & user from ~/.m2/setting.xml
        ret = getAuthConfigFromSettings(settings, user, registry, passwordDecryptionMethod);
        if (ret != null) {
            log.debug("AuthConfig: credentials from ~/.m2/setting.xml");
            return ret;
        }

        // check EC2 instance role if registry is ECR
        if (EcrExtendedAuth.isAwsRegistry(registry)) {
            try {
                ret = getAuthConfigFromEC2InstanceRole(log);
            } catch (ConnectTimeoutException ex) {
                log.debug("Connection timeout while retrieving instance meta-data, likely not an EC2 instance (%s)",
                        ex.getMessage());
            } catch (IOException ex) {
                // don't make that an error since it may fail if not run on an EC2 instance
                log.warn("Error while retrieving EC2 instance credentials: %s", ex.getMessage());
            }
            if (ret != null) {
                log.debug("AuthConfig: credentials from EC2 instance role");
                return ret;
            }
        }

        // No authentication found
        return null;
    }

    // ===================================================================================================


    // if the local credentials don't contain user and password, use EC2 instance
    // role credentials
    private static AuthConfig getAuthConfigFromEC2InstanceRole(KitLogger log) throws IOException {
        log.debug("No user and password set for ECR, checking EC2 instance role");
        try (CloseableHttpClient client = HttpClients.custom().useSystemProperties().build()) {
            // we can set very low timeouts because the request returns almost instantly on
            // an EC2 instance
            // on a non-EC2 instance we can fail early
            RequestConfig conf = RequestConfig.custom().setConnectionRequestTimeout(1000).setConnectTimeout(1000)
                    .setSocketTimeout(1000).build();

            // get instance role - if available
            HttpGet request = new HttpGet("http://169.254.169.254/latest/meta-data/iam/security-credentials");
            request.setConfig(conf);
            String instanceRole;
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    // no instance role found
                    log.debug("No instance role found, return code was %d", response.getStatusLine().getStatusCode());
                    return null;
                }

                // read instance role
                try (InputStream is = response.getEntity().getContent()) {
                    instanceRole = IOUtils.toString(is, StandardCharsets.UTF_8);
                }
            }
            log.debug("Found instance role %s, getting temporary security credentials", instanceRole);

            // get temporary credentials
            request = new HttpGet("http://169.254.169.254/latest/meta-data/iam/security-credentials/"
                    + UrlEscapers.urlPathSegmentEscaper().escape(instanceRole));
            request.setConfig(conf);
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    log.debug("No security credential found, return code was %d",
                            response.getStatusLine().getStatusCode());
                    // no instance role found
                    return null;
                }

                // read instance role
                try (Reader r = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
                    JsonObject securityCredentials = new Gson().fromJson(r, JsonObject.class);

                    String user = securityCredentials.getAsJsonPrimitive("AccessKeyId").getAsString();
                    String password = securityCredentials.getAsJsonPrimitive("SecretAccessKey").getAsString();
                    String token = securityCredentials.getAsJsonPrimitive("Token").getAsString();

                    log.debug("Received temporary access key %s...", user.substring(0, 8));
                    return new AuthConfig(user, password, "none", token);
                }
            }
        }
    }

    protected static AuthConfig getAuthConfigFromSystemProperties(LookupMode lookupMode, UnaryOperator<String> passwordDecryptionMethod) throws IOException {
        Properties props = System.getProperties();
        String userKey = lookupMode.asSysProperty(AUTH_USERNAME);
        String passwordKey = lookupMode.asSysProperty(AUTH_PASSWORD);
        if (props.containsKey(userKey)) {
            if (!props.containsKey(passwordKey)) {
                throw new IOException("No " + passwordKey + " provided for username " + props.getProperty(userKey));
            }
            return new AuthConfig(props.getProperty(userKey),
                                  passwordDecryptionMethod.apply(props.getProperty(passwordKey)),
                                  props.getProperty(lookupMode.asSysProperty(AUTH_EMAIL)),
                                  props.getProperty(lookupMode.asSysProperty(AUTH_AUTHTOKEN)));
        } else {
            return null;
        }
    }

    protected static AuthConfig getAuthConfigFromOpenShiftConfig(LookupMode lookupMode, Map authConfigMap) {
        Properties props = System.getProperties();
        String useOpenAuthModeProp = lookupMode.asSysProperty(AUTH_USE_OPENSHIFT_AUTH);
        // Check for system property
        if (props.containsKey(useOpenAuthModeProp)) {
            boolean useOpenShift = Boolean.parseBoolean(props.getProperty(useOpenAuthModeProp));
            if (useOpenShift) {
                return validateMandatoryOpenShiftLogin(parseOpenShiftConfig(), useOpenAuthModeProp);
            } else {
                return null;
            }
        }

        // Check plugin config
        Map mapToCheck = getAuthConfigMapToCheck(lookupMode,authConfigMap);
        if (mapToCheck != null && mapToCheck.containsKey(AUTH_USE_OPENSHIFT_AUTH) &&
            Boolean.parseBoolean((String) mapToCheck.get(AUTH_USE_OPENSHIFT_AUTH))) {
                return validateMandatoryOpenShiftLogin(parseOpenShiftConfig(), useOpenAuthModeProp);
        } else {
            return null;
        }
    }

    protected static AuthConfig getAuthConfigFromPluginConfiguration(LookupMode lookupMode, Map<String, ?> authConfig, UnaryOperator<String> passwordDecryptionMethod) {
        Map<String, String> mapToCheck = getAuthConfigMapToCheck(lookupMode,authConfig);

        if (mapToCheck != null && mapToCheck.containsKey(AUTH_USERNAME)) {
            if (!mapToCheck.containsKey(AUTH_PASSWORD)) {
                throw new IllegalStateException("No 'password' given while using <authConfig> in configuration for mode " + lookupMode);
            }
            Map<String, String> cloneConfig = new HashMap<>(mapToCheck);
            cloneConfig.put(AUTH_PASSWORD, passwordDecryptionMethod.apply(cloneConfig.get(AUTH_PASSWORD)));
            return AuthConfig.fromMap(cloneConfig);
        } else {
            return null;
        }
    }

    protected static AuthConfig getAuthConfigFromSettings(
        List<RegistryServerConfiguration> settings, String user, String registry, UnaryOperator<String> passwordDecryptionMethod) {

        RegistryServerConfiguration defaultServer = null;
        RegistryServerConfiguration found;
        for (RegistryServerConfiguration server : settings) {
            String id = server.getId();

            // Remember a default server without user as fallback for later
            if (defaultServer == null) {
                defaultServer = checkForServer(server, id, registry, null);
            }
            // Check for specific server with user part
            found = checkForServer(server, id, registry, user);
            if (found != null) {
                return createAuthConfigFromServer(found, passwordDecryptionMethod);
            }
        }
        return defaultServer != null ? createAuthConfigFromServer(defaultServer, passwordDecryptionMethod) : null;
    }

    protected static AuthConfig getAuthConfigFromDockerConfig(String registry, KitLogger log) throws IOException {
        JsonObject dockerConfig = DockerFileUtil.readDockerConfig();
        if (dockerConfig == null) {
            return null;
        }
        String registryToLookup = registry != null ? registry : DOCKER_LOGIN_DEFAULT_REGISTRY;

        if (dockerConfig.has("credHelpers") || dockerConfig.has("credsStore")) {
            if (dockerConfig.has("credHelpers")) {
                final JsonObject credHelpers = dockerConfig.getAsJsonObject("credHelpers");
                if (credHelpers.has(registryToLookup)) {
                    return extractAuthConfigFromCredentialsHelper(registryToLookup, credHelpers.get(registryToLookup).getAsString(), log);
                }
            }
            if (dockerConfig.has("credsStore")) {
                return extractAuthConfigFromCredentialsHelper(registryToLookup, dockerConfig.get("credsStore").getAsString(), log);
            }
        }

        if (dockerConfig.has("auths")) {
            return extractAuthConfigFromAuths(registryToLookup, dockerConfig.getAsJsonObject("auths"));
        }

        return null;
    }

    private static AuthConfig extractAuthConfigFromAuths(String registryToLookup, JsonObject auths) {
        JsonObject credentials = getCredentialsNode(auths,registryToLookup);
        if (credentials == null || !credentials.has("auth")) {
            return null;
        }
        String auth = credentials.get("auth").getAsString();
        String email = credentials.has(AUTH_EMAIL) ? credentials.get(AUTH_EMAIL).getAsString() : null;
        return AuthConfig.fromCredentialsEncoded(auth,email);
    }

    private static AuthConfig extractAuthConfigFromCredentialsHelper(String registryToLookup, String credConfig, KitLogger log) throws IOException {
        CredentialHelperClient credentialHelper = new CredentialHelperClient(log, credConfig);
        String version = credentialHelper.getVersion();
        log.debug("AuthConfig: credentials from credential helper/store %s%s",
                  credentialHelper.getName(),
                  version != null ? " version " + version : "");
        return credentialHelper.getAuthConfig(registryToLookup);
    }

    private static JsonObject getCredentialsNode(JsonObject auths,String registryToLookup) {
        if (auths.has(registryToLookup)) {
            return auths.getAsJsonObject(registryToLookup);
        }
        String registryWithScheme = EnvUtil.ensureRegistryHttpUrl(registryToLookup);
        if (auths.has(registryWithScheme)) {
            return auths.getAsJsonObject(registryWithScheme);
        }
        return null;
    }

    // =======================================================================================================

    private static Map<String, String> getAuthConfigMapToCheck(LookupMode lookupMode, Map<?, ?> authConfigMap) {
        String configMapKey = lookupMode.getConfigMapKey();
        if (configMapKey == null) {
            return (Map<String, String>)authConfigMap;
        }
        if (authConfigMap != null) {
            return (Map<String, String>)authConfigMap.get(configMapKey);
        }
        return null;
    }

    // Parse OpenShift config to get credentials, but return null if not found
    private static AuthConfig parseOpenShiftConfig() {
        Map kubeConfig = DockerFileUtil.readKubeConfig();
        if (kubeConfig == null) {
            return null;
        }

        String currentContextName = (String) kubeConfig.get("current-context");
        if (currentContextName == null) {
            return null;
        }

        for (Map contextMap : (List<Map>) kubeConfig.get("contexts")) {
            if (currentContextName.equals(contextMap.get("name"))) {
                return parseContext(kubeConfig, (Map) contextMap.get("context"));
            }
        }

        return null;
    }

    private static AuthConfig parseContext(Map kubeConfig, Map context) {
        if (context == null) {
            return null;
        }
        String userName = (String) context.get("user");
        if (userName == null) {
            return null;
        }

        List<Map> users = (List<Map>) kubeConfig.get("users");
        if (users == null) {
            return null;
        }

        for (Map userMap : users) {
            if (userName.equals(userMap.get("name"))) {
                return parseUser(userName, (Map) userMap.get("user"));
            }
        }
        return null;
    }

    private static AuthConfig parseUser(String userName, Map user) {
        if (user == null) {
            return null;
        }
        String token = (String) user.get("token");
        if (token == null) {
            return null;
        }

        // Strip off stuff after username
        Matcher matcher = Pattern.compile("^([^/]+).*$").matcher(userName);
        return new AuthConfig(matcher.matches() ? matcher.group(1) : userName,
                              token, null, null);
    }

    private static AuthConfig validateMandatoryOpenShiftLogin(AuthConfig openShiftAuthConfig, String useOpenAuthModeProp) {
        if (openShiftAuthConfig != null) {
            return openShiftAuthConfig;
        }
        // No login found
        String kubeConfigEnv = System.getenv("KUBECONFIG");
        throw new IllegalStateException(
            String.format("System property %s set, but not active user and/or token found in %s. " +
                          "Please use 'oc login' for connecting to OpenShift.",
                          useOpenAuthModeProp, kubeConfigEnv != null ? kubeConfigEnv : "~/.kube/config"));

    }


    private static RegistryServerConfiguration checkForServer(RegistryServerConfiguration server, String id, String registry, String user) {

        String[] registries = registry != null ? new String[] { registry } : DEFAULT_REGISTRIES;
        for (String reg : registries) {
            if (id.equals(user == null ? reg : reg + "/" + user)) {
                return server;
            }
        }
        return null;
    }

    private static AuthConfig createAuthConfigFromServer(RegistryServerConfiguration server, UnaryOperator<String> passwordDecryptionMethod) {
        return new AuthConfig(
                server.getUsername(),
                passwordDecryptionMethod.apply(server.getPassword()),
                extractFromServerConfiguration(server.getConfiguration(), AUTH_EMAIL),
                extractFromServerConfiguration(server.getConfiguration(), "auth")
        );
    }

    private static String extractFromServerConfiguration(Map<String, Object> configuration, String prop) {
        if (configuration != null && configuration.containsKey(prop)) {
            return configuration.get(prop).toString();
        }
        return null;
    }

    // ========================================================================================
    // Mode which direction to lookup (pull, push or default value for both, pull and push)

    private static LookupMode getLookupMode(boolean isPush) {
        return isPush ? LookupMode.PUSH : LookupMode.PULL;
    }

    protected enum LookupMode {
        PUSH("docker.push.","push"),
        PULL("docker.pull.","pull"),
        DEFAULT("docker.",null);

        private final String sysPropPrefix;
        private String configMapKey;

        LookupMode(String sysPropPrefix,String configMapKey) {
            this.sysPropPrefix = sysPropPrefix;
            this.configMapKey = configMapKey;
        }

        public String asSysProperty(String prop) {
            return sysPropPrefix + prop;
        }

        public String getConfigMapKey() {
            return configMapKey;
        }
    }

}
