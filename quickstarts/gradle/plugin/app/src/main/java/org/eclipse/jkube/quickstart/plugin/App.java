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
package org.eclipse.jkube.quickstart.plugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@RestController
public class App {


  @GetMapping("/")
  public ResponseEntity<Resource> home() {
    if (isProd()) {
      return ResponseEntity
        .ok()
        .contentType(MediaType.APPLICATION_PDF)
        .body(new FileSystemResource(Path.of("/deployments", "files", "spring-boot-doc.pdf")));
    } else {
      return ResponseEntity
        .ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body(new ByteArrayResource("This is the dev environment".getBytes()));
    }
  }

  @GetMapping("/version")
  public String version() throws IOException {
    if (isProd()) {
      return Files.readString(Path.of("/deployments", "files", "build-timestamp"));
    }
    return Instant.now().toString();
  }

  private static boolean isProd() {
    return new File("/deployments").exists();
  }

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

}
