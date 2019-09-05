package io.jshift.kit.build.service.docker.auth;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonObject;
import io.jshift.kit.build.api.auth.AuthConfig;
import io.jshift.kit.common.ExternalCommand;
import io.jshift.kit.common.JsonFactory;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.common.util.EnvUtil;
import org.apache.maven.plugin.MojoExecutionException;

public class CredentialHelperClient {

    static final String SECRET_KEY = "Secret";
    static final String USERNAME_KEY = "Username";
    private final String credentialHelperName;
    private final KitLogger log;

    public CredentialHelperClient(KitLogger log, String credentialsStore) {
        this.log = log;
        credentialHelperName = "docker-credential-" + credentialsStore;
    }

    public String getName() {
        return credentialHelperName;
    }

    public String getVersion() {
        try {
            return new VersionCommand().getVersion();
        } catch (IOException e) {
            throw new RuntimeException("Error getting the version of the configured credential helper", e);
        }
    }


    public AuthConfig getAuthConfig(String registryToLookup) throws IOException {
        try {
            JsonObject creds = new GetCommand().getCredentialNode(registryToLookup);
            if (creds == null) {
                creds = new GetCommand().getCredentialNode(EnvUtil.ensureRegistryHttpUrl(registryToLookup));
            }
            return toAuthConfig(creds);
        } catch (IOException e) {
            throw new IOException("Error getting the credentials for " + registryToLookup + " from the configured credential helper",e);
        }
    }

    private AuthConfig toAuthConfig(JsonObject credential){
        if (credential == null) {
            return null;
        }
        String password = credential.get(CredentialHelperClient.SECRET_KEY).getAsString();
        String userKey = credential.get(CredentialHelperClient.USERNAME_KEY).getAsString();
        return new AuthConfig(userKey,password, null,null);
    }

    // docker-credential-XXX version
    private class VersionCommand extends ExternalCommand {

        private String version;

        VersionCommand() {
            super(CredentialHelperClient.this.log);
        }

        @Override
        protected String[] getArgs() {
            return new String[]{CredentialHelperClient.this.credentialHelperName, "version"};
        }

        @Override
        protected void processLine(String line) {
            log.verbose("Credentials helper reply for \"%s\" is %s",CredentialHelperClient.this.credentialHelperName,line);
            version = line;
        }

        public String getVersion() throws IOException {
            execute();
            if (version == null) {
                log.verbose("The credentials helper \"%s\" didn't return a version string",CredentialHelperClient.this.credentialHelperName);
            }
            return version;
        }
    }

    // echo <registryToLookup> | docker-credential-XXX get
    private class GetCommand extends ExternalCommand {

        private List<String> reply = new LinkedList<>();

        GetCommand() {
            super(CredentialHelperClient.this.log);
        }

        @Override
        protected String[] getArgs() {
            return new String[]{CredentialHelperClient.this.credentialHelperName, "get"};
        }

        @Override
        protected void processLine(String line) {
            reply.add(line);
        }

        public JsonObject getCredentialNode(String registryToLookup) throws IOException {
            try {
                execute(registryToLookup);
            } catch (IOException ex) {
                if (getStatusCode() == 1) {
                    return null;
                } else {
                    throw ex;
                }
            }
            JsonObject credentials =
                JsonFactory.newJsonObject(String.join("\n", reply));
            if (!credentials.has(SECRET_KEY) || !credentials.has(USERNAME_KEY)) {
                return null;
            }
            return credentials;
        }
    }
}
