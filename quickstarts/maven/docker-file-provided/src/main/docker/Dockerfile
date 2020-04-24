FROM fabric8/s2i-java:latest-java11
EXPOSE 8080/tcp
COPY maven/*.jar /opt/app.jar
ENTRYPOINT [ "java", "-jar", "/opt/app.jar" ]
