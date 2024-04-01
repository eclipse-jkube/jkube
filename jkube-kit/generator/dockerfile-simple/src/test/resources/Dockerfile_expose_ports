ARG VERSION=latest
FROM openjdk:$VERSION
EXPOSE 80
COPY maven/target/docker-file-simple.jar /deployments/docker-file-simple.jar
# Copying a file inside project root directory
COPY maven/static-dir-in-project-root/my-file.txt /deployments/my-file.txt
EXPOSE 8080
EXPOSE 80/tcp
EXPOSE 8080/udp
EXPOSE 99/udp
CMD ["java", "-jar", "/deployments/docker-file-simple.jar"]
EXPOSE 8080