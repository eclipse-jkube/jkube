FROM alpine
ENV JAVA_OPTIONS=-Xmx256m
LABEL MAINTAINER="JKube testing team"
EXPOSE 8080
COPY /deployments/static /deployments/static
COPY /deployments/app /deployments/app
ENTRYPOINT java -jar /deployments/xml-image-multilayer-0.1-SNAPSHOT.jar
