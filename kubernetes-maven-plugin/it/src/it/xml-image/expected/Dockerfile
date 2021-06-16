FROM alpine
ENV JAVA_OPTIONS=-Xmx256m
LABEL MAINTAINER="JKube testing team"
EXPOSE 8080
COPY /jkube-generated-layer-original/deployments /deployments/
COPY /jkube-generated-layer-final-artifact/deployments /deployments/
ENTRYPOINT java -jar /deployments/xml-image-0.1-SNAPSHOT.jar
