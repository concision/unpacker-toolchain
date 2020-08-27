#### Packages Server
# Note: The build context for this Dockerfile is at the project root, as maven requires the api-server's parent


### Build Scripts
# Windows build compatbility - sanitize Windows line endings and mark scripts as executable
FROM alpine:3.12.0 as scripts

# install dos2unix to remove \r's from scripts when compiling on Windows machines
RUN apk add --no-cache dos2unix
# copy updater script
COPY ./server/docker/*.sh /packages/server/scripts/
# remove \r's and set as executable
RUN dos2unix /packages/server/scripts/*.sh && \
    chmod +x /packages/server/scripts/*.sh



### Maven Precompiled Project Artifacts
# Compile the io-api module and api-server's module-info for building a custom JRE
FROM maven:3.6.3-openjdk-15 as precompiled

## Runtime configuration
# mark environment as Docker; will disable cli-tool module via Maven profiles
ENV DOCKER=true
# set current work directory
WORKDIR /packages/

## Copy Projects
# copy project module manifests
COPY ./pom.xml /packages/
# copy io-api module
COPY ./io-api/ /packages/io-api/
# copy api-server module configuration
COPY ./server/pom.xml /packages/server/
# copy api-server's module-info.java
COPY ./server/src/main/java/module-info.java /packages/server/src/main/java/module-info.java

## Compile Projects
# compile io-api and api-server's module-info.java
# note: this layer will cache project dependencies in the same way that dependency:go-offline does
RUN mvn package -Dstyle.color=always -Djansi.force=true


### Build Server Artifacts
# Compile the api-server module
FROM precompiled as compiled

## Copy Sources
# copy entire api-server module
COPY ./server/src/ /packages/server/src/

## Compile Project
# build project artifacts for api-server
RUN mvn package -Dstyle.color=always -Djansi.force=true



### Build Minimal JRE
# Build a minimal JRE based on the compiled module-info.class from the precompiled build stage
# Note: This stage needs an alpine-based JDK to build a JRE for the final alpine image
FROM openjdk:15-ea-jdk-alpine3.12 as jre

# copy module information
COPY --from=precompiled /packages/server/target/classes/module-info.class /packages/server/

# create custom JRE
RUN jlink \
    --module-path "/packages/server/" \
    --add-modules me.concision.unnamed.packages.server \
    --output /packages/jre


### Build Runtime Application
# Build Docker image with all project artifacts
FROM alpine:3.12.0

## Image Configuration
# add JRE to path
ENV PATH="/opt/jre/bin:${PATH}"
# set current working directory
WORKDIR /packages/
# dynamic server data (e.g. configuration, logs, etc)
VOLUME /packages/server/data/
# set container "entrypoint"
CMD /packages/server/entrypoint.sh
# expose default HTTP port
EXPOSE 80

## Collect Linux Dependencies
# install Wine
RUN apk add --no-cache \
    wine \
    # wine runtime dependencies for our Windows application
    libpng freetype
# copy custom minimal JRE
COPY --from=jre /packages/jre/ /opt/jre/

## Project Artifacts
# copy Linux scripts
COPY --from=scripts /packages/server/scripts/*.sh /packages/server/
# copy built project artifact
COPY --from=compiled /packages/server/target/packages-server.jar /packages/server/
