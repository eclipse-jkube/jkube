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
package org.eclipse.jkube.kit.common.util.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates Kubernetes/OpenShift resource descriptors using JSON schema validation method.
 * For OpenShift it adds some some exceptions from JSON schema constraints and ignores some validation errors.
 */

public class ResourceValidator {

    public static final String SCHEMA_JSON = "/schema/kube-validation-schema.json";
    private KitLogger log;
    private File resources[];
    private ResourceClassifier target = ResourceClassifier.KUBERNETES;
    private List<ValidationRule> ignoreValidationRules = new ArrayList<>();

    /**
     * @param inputFile File/Directory path of resource descriptors
     */
    public ResourceValidator(File inputFile) {
        if(inputFile.isDirectory()) {
            resources = inputFile.listFiles();
        } else {
            resources = new File[]{inputFile};
        }
    }

    /**
     * @param inputFile File/Directory path of resource descriptors
     * @param target  Target platform e.g OpenShift, Kubernetes
     * @param log KitLogger for logging messages on standard output devices
     */
    public ResourceValidator(File inputFile, ResourceClassifier target, KitLogger log) {
        this(inputFile);
        this.target = target;
        this.log = log;
        setupIgnoreRules(this.target);
    }

    /*
     * Add exception rules to ignore validation constraint from JSON schema for OpenShift/Kubernetes resources. Some fields in JSON schema which are marked as required
     * but in reality it's not required to provide values for those fields while creating the resources.
     * e.g. In DeploymentConfig(https://docs.openshift.com/container-platform/3.6/rest_api/openshift_v1.html#v1-deploymentconfig) model 'status' field is marked as
     * required.
     */
    private void setupIgnoreRules(ResourceClassifier target) {
        ignoreValidationRules.add(new IgnorePortValidationRule(IgnorePortValidationRule.TYPE));
        ignoreValidationRules.add(new IgnoreResourceMemoryLimitRule(IgnoreResourceMemoryLimitRule.TYPE));
    }

    /**
     * Validates the resource descriptors as per JSON schema. If any resource is invalid it throws @{@link ConstraintViolationException} with
     * all violated constraints
     *
     * @return number of resources processed
     * @throws ConstraintViolationException  ConstraintViolationException
     * @throws IOException IOException
     */
    public int validate() throws ConstraintViolationException, IOException {
        for(File resource: resources) {
            if (resource.isFile() && resource.exists()) {
                try {
                    log.info("validating %s resource", resource.toString());
                    JsonNode inputSpecNode = geFileContent(resource);
                    String kind = inputSpecNode.get("kind").toString();
                    JsonSchema schema = getJsonSchema(prepareSchemaUrl(SCHEMA_JSON), kind);
                    Set<ValidationMessage> errors = schema.validate(inputSpecNode);
                    processErrors(errors, resource);
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
        }

        return resources.length;
    }

    private void processErrors(Set<ValidationMessage> errors, File resource) {
        Set<ConstraintViolationImpl> constraintViolations = new HashSet<>();
        for (ValidationMessage errorMsg: errors) {
            if(!ignoreError(errorMsg))
                constraintViolations.add(new ConstraintViolationImpl(errorMsg));
        }

        if(constraintViolations.size() > 0) {
            throw new ConstraintViolationException(getErrorMessage(resource, constraintViolations), constraintViolations);
        }
    }

    private boolean ignoreError(ValidationMessage errorMsg) {
        for (ValidationRule rule : ignoreValidationRules) {
            if(rule.ignore(errorMsg)) {
                return  true;
            }
        }

        return false;
    }

    private String getErrorMessage(File resource, Set<ConstraintViolationImpl> violations) {
        StringBuilder validationError = new StringBuilder();
        validationError.append("Invalid Resource : ");
        validationError.append(resource.toString());

        for (ConstraintViolationImpl violation: violations) {
            validationError.append("\n");
            validationError.append(violation.toString());
        }

        return  validationError.toString();
    }

    private JsonSchema getJsonSchema(URI schemaUrl, String kind) throws IOException {
        checkIfKindPropertyExists(kind);
        JsonSchemaFactory factory = new JsonSchemaFactory();
        JsonObject jsonSchema = getSchemaJson(schemaUrl);
        getResourceProperties(kind, jsonSchema);

        return factory.getSchema(jsonSchema.toString());
    }

    private void getResourceProperties(String kind, JsonObject jsonSchema) {
        jsonSchema.add("properties" , jsonSchema.get("resources").getAsJsonObject()
                .getAsJsonObject(kind.replaceAll("\"", "").toLowerCase())
                .getAsJsonObject("properties"));
    }

    private void checkIfKindPropertyExists(String kind) {
        if(kind == null) {
            throw new JsonIOException("Invalid kind of resource or 'kind' is missing from resource definition");
        }
    }

    private URI prepareSchemaUrl(String schemaFile) throws URISyntaxException {
        return getClass().getResource(schemaFile).toURI();
    }

    private JsonNode geFileContent(File file) throws IOException {
        try (InputStream resourceStream = new FileInputStream(file)) {
            ObjectMapper jsonMapper = new ObjectMapper(new YAMLFactory());
            return jsonMapper.readTree(resourceStream);
        }
    }

    public JsonObject getSchemaJson(URI schemaUrl) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String rootNode = objectMapper.readValue(schemaUrl.toURL(), JsonNode.class).toString();
        JsonObject jsonObject = new JsonParser().parse(rootNode).getAsJsonObject();
        jsonObject.remove("id");
        return jsonObject;
    }

    private class ConstraintViolationImpl implements ConstraintViolation<ValidationMessage> {

        private ValidationMessage errorMsg;

        public ConstraintViolationImpl(ValidationMessage errorMsg) {
            this.errorMsg = errorMsg;
        }

        @Override
        public String getMessage() {
            return errorMsg.getMessage();
        }

        @Override
        public String getMessageTemplate() {
            return null;
        }

        @Override
        public ValidationMessage getRootBean() {
            return null;
        }

        @Override
        public Class<ValidationMessage> getRootBeanClass() {
            return null;
        }

        @Override
        public Object getLeafBean() {
            return null;
        }

        @Override
        public Object[] getExecutableParameters() {
            return new Object[0];
        }

        @Override
        public Object getExecutableReturnValue() {
            return null;
        }

        @Override
        public Path getPropertyPath() {
            return null;
        }

        @Override
        public Object getInvalidValue() {
            return null;
        }

        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
        }

        @Override
        public <U> U unwrap(Class<U> aClass) {
            return null;
        }

        @Override
        public String toString() {
            return "[message=" + getMessage().replaceFirst("[$]", "") +", violation type="+errorMsg.getType()+"]";
        }
    }
}
