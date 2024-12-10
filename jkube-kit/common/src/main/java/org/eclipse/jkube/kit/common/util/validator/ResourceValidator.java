/*
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

import io.fabric8.kubernetes.schema.validator.ValidationMessage;
import io.fabric8.kubernetes.schema.validator.ValidationReport;
import io.fabric8.kubernetes.schema.validator.Validator;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates Kubernetes/OpenShift resource descriptors using JSON schema validation method.
 * For OpenShift, it adds some exceptions from JSON schema constraints and ignores some validation errors.
 */

public class ResourceValidator {

    private KitLogger log;
    private final File[] resources;
    private final List<ValidationRule> ignoreValidationRules = new ArrayList<>();

    /**
     * @param inputFile File/Directory path of resource descriptors
     */
    public ResourceValidator(File inputFile) {
        if(inputFile.isDirectory()) {
            resources = inputFile.listFiles();
        } else {
            resources = new File[]{inputFile};
        }
        setupIgnoreRules();
    }

    /**
     * @param inputFile File/Directory path of resource descriptors
     * @param target  Target platform e.g OpenShift, Kubernetes
     * @param log KitLogger for logging messages on standard output devices
     */
    public ResourceValidator(File inputFile, ResourceClassifier target, KitLogger log) {
        this(inputFile);
        this.log = log;
    }

    /*
     * Add exception rules to ignore validation constraint from JSON schema for OpenShift/Kubernetes resources. Some fields in JSON schema which are marked as required
     * but in reality it's not required to provide values for those fields while creating the resources.
     * e.g. In DeploymentConfig(https://docs.openshift.com/container-platform/3.6/rest_api/openshift_v1.html#v1-deploymentconfig) model 'status' field is marked as
     * required.
     */
    private void setupIgnoreRules() {
        ignoreValidationRules.add(new IgnorePortValidationRule());
    }



    /**
     * Validates the resource descriptors as per JSON schema. If any resource is invalid it throws @{@link ConstraintViolationException} with
     * all violated constraints
     *
     * @return number of resources processed
     * @throws ConstraintViolationException  ConstraintViolationException
     * @throws IOException IOException
     */
    public int validate() throws IOException {
        final Validator validator = Validator.newInstance();
        for(File resource: resources) {
            if (resource.isFile() && resource.exists()) {
                log.info("validating %s resource", resource.toString());
                JsonNode inputSpecNode = geFileContent(resource);
                final ValidationReport report = validator.validate(inputSpecNode);
                if (report.hasErrors()) {
                    processErrors(report.getMessages(), resource);
                }
            }
        }
        return resources.length;
    }

    private void processErrors(Collection<ValidationMessage> errors, File resource) {
        final Set<ConstraintViolationImpl> constraintViolations = new HashSet<>();
        for (ValidationMessage errorMsg: errors) {
            if (!ignoreError(errorMsg)) {
                constraintViolations.add(new ConstraintViolationImpl(errorMsg));
            }
        }
        if(!constraintViolations.isEmpty()) {
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

    private JsonNode geFileContent(File file) throws IOException {
        try (InputStream resourceStream = Files.newInputStream(file.toPath())) {
            ObjectMapper jsonMapper = new ObjectMapper(new YAMLFactory());
            return jsonMapper.readTree(resourceStream);
        }
    }

    private static class ConstraintViolationImpl implements ConstraintViolation<ValidationMessage> {

        private final ValidationMessage errorMsg;

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
            return "[message=" + getMessage().replaceFirst("[$]", "") +", violation type="+errorMsg.getLevel().name()+"]";
        }
    }

}
