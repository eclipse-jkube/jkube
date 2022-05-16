---
name: "Kit :: Docker Image"
description: |
  Eclipse JKube Kit example showing how to generate a Docker image by using Eclipse JKube in standalone mode.
---
# JKube Kit - Docker Image Build Example

This [quickstart](../../../quickstarts) will generate a Docker image using JKube Kit API.

Image configuration can be altered programmatically in the following section:

```java
final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
    .name("jkube-example")
    .run(RunImageConfiguration.builder().user("1000").build())
    .build(BuildConfiguration.builder()
        .putEnv("MY_VAR", "value")
        .putEnv("MY_OTHER_VAR", "true")
        .label("maintainer", "JKube Devs")
        .port("80/tcp")
        .maintainer("JKube Devs")
        .from("busybox")
        .cmd(Arguments.builder().shell("/bin/sh").build())
        .build())
    .build();
```

## How to run

Run the `mvn package` from your terminal.

Once the image is built a success informative message will print the generated image id.

You can now run the generated image by invoking `docker run -it ${imageId}`.

```shell script
$ mvn clean package
# ...
[INFO] --- exec-maven-plugin:1.2.1:java (default) @ docker-image ---
Initiating default JKube configuration and required services...
 - Creating Docker Service Hub
 - Creating Docker Build Service Configuration
 - Creating configuration for JKube
Creating configuration for example Docker Image
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Build args set {}
Build args set {}
[jkube-example:latest]: Created docker-build.tar in 73 milliseconds
[jkube-example:latest]: Built image sha256:1e648
Deleted: "sha256:d51117140b834c19506d7ec8132861f23c5a8b19843f89f241522c3265e0c10f"
Deleted: "sha256:4a19816ec525a9975864f89e5a4349be9e18f5a75819758fc0ccfa741c111145"
Deleted: "sha256:b591658b980f4a906f173d86db212f32b2005a0bc06db346f5e5d66ddba88070"
Deleted: "sha256:14f2f99860068a0f58fd7ff861008e85a8ff6e9049577eb041868765e5553ec1"
Deleted: "sha256:bd42432d1bd00f27813542bf82226edfcb88718f4bf3b4b8bcaa9c6fd10f6d58"
[jkube-example:latest]: Removed old image sha256:d5111
Docker image built successfully (sha256:1e648)!
$ docker run -it 1e648
/ #
```