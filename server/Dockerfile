#### Packages Server
# Note: The build context for this Dockerfile is at the project root, as maven requires the api-server's parent

### Build Scripts
FROM alpine:3.12.0 as scripts

# install dos2unix to remove \r's from scripts when compiling on Windows machines
RUN apk add --no-cache dos2unix
# copy updater script
COPY ./server/docker/*.sh /packages/server/scripts/
# remove \r's and set as executable
RUN dos2unix /packages/server/scripts/*.sh && \
    chmod +x /packages/server/scripts/*.sh


### Compile Maven Project
FROM maven:3.6.3-openjdk-15 as compiler

# mark environment as docker; will disable other modules via profiles
ENV DOCKER=true

# set current work directory
WORKDIR /packages/

# copy project module manifests
COPY ./pom.xml /packages/
COPY ./server/pom.xml /packages/server/
COPY ./io-api/pom.xml /packages/io-api/

# fetch and cache dependencies
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies -Dstyle.color=always -Djansi.force=true

# copy io-api module
COPY ./io-api/ /packages/io-api/
# copy api-server module
COPY ./server/ /packages/server/

# compile project artifacts for api server
RUN mvn clean package -Dstyle.color=always -Djansi.force=true


### Runtime Application
FROM openjdk:15-ea-jdk-alpine3.12

## Image Configuration
# dynamic server data (e.g. configuration, logs, etc)
VOLUME /packages/server/data/

# expose default HTTP port
EXPOSE 80

# set container "entrypoint"
CMD /packages/server/entrypoint.sh

## Collect dependencies
# Install Wine
RUN apk add --no-cache \
    wine \
    # wine runtime dependencies for our Windows application
    libpng freetype

## Project Artifacts
# copy scripts
COPY --from=scripts /packages/server/scripts/*.sh /packages/server/

# copy built project artifact
COPY --from=compiler /packages/server/target/packages-server.jar /packages/server/
