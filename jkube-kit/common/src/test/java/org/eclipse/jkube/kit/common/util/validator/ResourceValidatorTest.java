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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.nio.file.Paths;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;

import mockit.Mocked;
import org.assertj.core.api.Condition;
import org.junit.Test;

public class ResourceValidatorTest {

  @Mocked
  private KitLogger logger;

  @Test
  public void validateWithValidResource() throws Exception {
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
  public void validateWithInvalidResource() throws Exception {
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
        .has(new HasErrMessage("$.spec.replicas: string found, integer expected"));
  }

  private static final class HasErrMessage extends Condition<Throwable> {

    private final String message;

    private HasErrMessage(String message) {
      this.message = message;
    }

    @Override
    public boolean matches(Throwable value) {
      if (value instanceof ConstraintViolationException) {
        final ConstraintViolationException ex = (ConstraintViolationException)value;
        return ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage).anyMatch(m -> m.equals(message));
      }
      return false;
    }
  }
}
