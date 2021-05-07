package org.eclipse.jkube.kit.resource.helm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HelmDependencyTest {

  @Test
  public void equalsAndHashCodeTest() {

    // Given
    HelmDependency helmDependency = HelmDependency
        .builder()
        .name("name")
        .repository("repository")
        .version("version")
        .build();

    // Then
    assertThat(helmDependency.equals(helmDependency)).isTrue();
    assertThat(helmDependency.getName()).isEqualTo("name");
    assertThat(helmDependency.getRepository()).isEqualTo("repository");
    assertThat(helmDependency.getVersion()).isEqualTo("version");
  }
}
