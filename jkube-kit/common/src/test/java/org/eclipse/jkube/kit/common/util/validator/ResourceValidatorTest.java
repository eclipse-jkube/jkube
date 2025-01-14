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


import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceValidatorTest {
  private KitLogger logger;

  @BeforeEach
  public void setUp() {
    logger = new KitLogger.SilentLogger();
  }

  @Test
  void validateWithValidService() throws Exception {
    // Given
    final ResourceValidator validator = new ResourceValidator(
        Paths.get(ResourceValidatorTest.class.getResource("/util/validator/valid-service.yml").toURI()).toFile(),
        ResourceClassifier.KUBERNETES,
        logger
    );
    // When
    final int result = validator.validate();
    // Then
    assertThat(result).isEqualTo(1);
  }

  @Test
  void validateWithValidDeployment() throws Exception {
    // Given
    final ResourceValidator validator = new ResourceValidator(
        Paths.get(ResourceValidatorTest.class.getResource("/util/validator/valid-deployment.yml").toURI()).toFile(),
        ResourceClassifier.KUBERNETES,
        logger
    );
    // When
    final int result = validator.validate();
    // Then
    assertThat(result).isEqualTo(1);
  }

  @Test
  void validateWithInvalidResource() throws Exception {
    // Given
    final ResourceValidator validator = new ResourceValidator(
        Paths.get(ResourceValidatorTest.class.getResource("/util/validator/invalid-deployment.yml").toURI()).toFile(),
        ResourceClassifier.KUBERNETES,
        logger
    );
    // When
    final ConstraintViolationException result = assertThrows(ConstraintViolationException.class, validator::validate);
    // Then
    assertThat(result).isNotNull()
        .hasMessageStartingWith("Invalid Resource :")
        .hasMessageContaining("invalid-deployment.yml")
        .extracting(ConstraintViolationException::getConstraintViolations)
        .asInstanceOf(InstanceOfAssertFactories.set(ConstraintViolation.class))
        .extracting("message")
        .contains("[Path '/spec/replicas'] Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])");
  }
}
