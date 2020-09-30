# JKube Kit - Docker Image Build  from Dynamic multilayered Dockerfile Example

This [quickstart](../../../quickstarts) will generate a Docker image using JKube Kit API.

It first assembles files to be added to the image in a temporary directory.

The Dockerfile is dynamically generated and contains multiple entries for the `COPY` statement
which allows the use of multiple layers.

## How to run

Run the `mvn package` from your terminal.

Once the image is built a success informative message will print the generated image id.

You can now run the generated image by invoking `docker run -it ${imageId}`.

```shell script
$ mvn clean package
# ...
[INFO] --- exec-maven-plugin:1.2.1:java (default) @ dynamic-docker-image-file-multi-layer ---
Initiating default JKube configuration and required services...
 - Creating Docker Service Hub
 - Creating Docker Build Service Configuration
 - Creating configuration for JKube
 - Current working directory is: /home/user/00-MN/projects/forks/jkube/quickstarts/kit/dynamic-docker-image-file-multi-layer
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Assembling context for Dockerfile
Generating dynamic Dockerfile
Preparing build configuration for Dynamic Dockerfile mode
Build args set {}
Build args set {}
Build args set {}
Dockerfile /home/user/00-MN/projects/forks/jkube/quickstarts/kit/dynamic-docker-image-file-multi-layer/target/preassembled-docker/Dockerfile does not contain an ADD or COPY directive to include assembly created at ContextAssembly. Ignoring assembly.
[test-from-dynamic-dockerfile:latest]: Created docker-build.tar in 32 milliseconds
  latest Pulling from library/busybox
  df8698476c65 Pulling fs layer
  df8698476c65 Downloading
  df8698476c65 Downloading
  df8698476c65 Download complete
  Status: Downloaded newer image for busybox:latest
[test-from-dynamic-dockerfile:latest]: Built image sha256:7e822
Docker image built successfully (sha256:7e822)!
$ docker run -it --rm 7e822
/ #
```