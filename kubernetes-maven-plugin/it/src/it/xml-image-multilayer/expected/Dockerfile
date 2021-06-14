FROM alpine
ENV JAVA_OPTIONS=-Xmx256m
LABEL MAINTAINER="JKube testing team"
EXPOSE 8080
COPY /static-files/deployments /deployments/
COPY /artifact/deployments /deployments/
ENTRYPOINT java -jar /deployments/xml-image-multilayer-0.1-SNAPSHOT.jar
