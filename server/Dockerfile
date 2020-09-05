#### Packages Server
# Note: The build context for this Dockerfile is at the project root, as maven requires the api-server's parent

### Build Scripts
# Windows build compatbility - sanitize Windows line endings and mark scripts as executable
FROM alpine:3.12.0 as scripts

# install dos2unix to remove \r's from scripts when compiling on Windows machines
RUN apk add --no-cache dos2unix
# copy updater script
COPY ./server/docker/*.sh /unpacker/server/scripts/
# remove \r's and set as executable
RUN dos2unix /unpacker/server/scripts/*.sh && \
    chmod +x /unpacker/server/scripts/*.sh


### Maven Precompiled Project Artifacts
# Compiles the cli-tool
FROM maven:3.6.3-openjdk-15 as unpacker

## Runtime configuration
# set current work directory
WORKDIR /unpacker/

## Copy Project Configuration
# copy root module manifest
COPY ./pom.xml ./lombok.config /unpacker/
# copy I/O api module manifest
COPY ./api/pom.xml /unpacker/api/pom.xml
# copy cli-tool module manifest
COPY ./cli/pom.xml /unpacker/cli/pom.xml
# cache dependencies
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies -Dstyle.color=always -Djansi.force=true

## Copy Sources
# copy I/O api module
COPY ./api/ /unpacker/api/
# copy cli-tool module
COPY ./cli/ /unpacker/cli/

## Compile Projects
# compile project
# note: this layer will cache project dependencies in the same way that dependency:go-offline does
RUN DOCKER=true mvn package -Dstyle.color=always -Djansi.force=true


### Build Minimal JRE
# Build a minimal JRE based on the compiled module-info.class from the precompiled build stage
# Note: This stage needs an alpine-based JDK to build a JRE for the final alpine image
FROM openjdk:15-ea-jdk-alpine3.12 as jre

# copy module information
COPY --from=unpacker /unpacker/cli/target/unpacker.jar /unpacker/cli/unpacker.jar

# create custom minimal JRE
RUN jlink \
    --module-path "/unpacker/cli/" \
    --add-modules me.concision.unnamed.unpacker.cli \
    --output /unpacker/jre/


### Collect Python Dependencies ###
FROM python:3.9.0rc1-alpine3.12 as python-deps
## Runtime Configuration
# set current working directory
WORKDIR /unpacker/server/

## Pipenv
# pipenv dependencies
COPY ./server/pipenv-requirements.txt /unpacker/server/
# install pipenv
RUN pip install --no-cache-dir -r pipenv-requirements.txt

## Python Virtual Environment
# initialize virtual environment
RUN python -m venv /opt/venv
# set virtual environment path
ENV PATH="/opt/venv/bin:$PATH" VIRTUAL_ENV="/opt/venv"

## Linux Dependencies
# install build tools to compile the postgres python dependency
RUN apk add --no-cache \
    gcc \
    musl-dev \
    # postgres
    postgresql-dev

## Python Dependencies
# copy dependency manifests
COPY ./server/Pipfile ./server/Pipfile.lock /unpacker/server/
# install locked depenedencies with pipenv without creating a virtual environment
RUN pipenv install --deploy


### Build Runtime Application
# Build Docker image with all project artifacts
FROM python:3.9.0rc1-alpine3.12

## Image Configuration
# add JRE to path
ENV PATH="/opt/jre/bin:${PATH}"
# set current working directory
WORKDIR /unpacker/server/
# dynamic server data (e.g. configuration, logs, etc)
VOLUME /unpacker/server/data/
# set container "entrypoint"
CMD ["/unpacker/server/entrypoint.sh"]
# expose default HTTP port
EXPOSE 80

## Collect Linux Dependencies
# copy custom minimal JRE
COPY --from=jre /unpacker/jre/ /opt/jre/
# copy unpacker cli tool
COPY --from=unpacker /unpacker/cli/target/unpacker.jar /unpacker/cli/unpacker.jar

# install Wine and minimize based on runtime analysis
RUN \
    # install strace for runtime analysis
    apk add --no-cache strace && \
    # take snapshot of filesystem before wine installation
    find / -type f | sort > original-snapshot.txt && \
    # install wine and dependencies
    apk add --no-cache \
        wine \
        # wine runtime dependencies for our Windows application
        libpng freetype && \
    # take snapshot of filesystem after wine installation
    find / -type f | sort > installed-snapshot.txt && \
    # compute difference to discover all new wine files
    comm -2 -3 installed-snapshot.txt original-snapshot.txt > wine-files.txt && \
    # remove filesystem snapshots
    rm -f installed-snapshot.txt original-snapshot.txt && \
    # run unpacker using game updater
    java -jar /unpacker/cli/unpacker.jar \
        # log information
        --verbose \
        # use game updater to fetch latest
        --source UPDATER \
        # output a raw binary Packages.bin
        --output BINARY \
        # set the default wine command
        --wine-cmd "strace --follow-forks --quiet=all --trace=open --silence=all --no-abbrev --output=strace.txt /bin/sh -c \"/usr/bin/wine64 %UNPACKER_COMMAND%\"" \
        # pipe the Packages.bin output to /dev/null
        >/dev/null && \
    # sanitize strace log
    grep -o "\".*\"" strace.txt | tr -d "\"" | xargs realpath 2>/dev/null | grep -v "%" | uniq | sort > used-files.txt && \
    # delete strace log
    rm -f strace.txt && \
    # compute difference to discover all unused wine files and delete them
    comm -2 -3 wine-files.txt used-files.txt | xargs rm -f && \
    # remove filesystem snapshots
    rm -f wine-files.txt used-files.txt && \
    # uninstall strace
    apk --no-network del strace

## Project Artifacts
# copy Linux scripts
COPY --from=scripts /unpacker/server/scripts/*.sh /unpacker/server/

## Runtime Configuration
# disable Python .pyc
ENV PYTHONDONTWRITEBYTECODE 1

## Python Dependencies
# copy the virtual environment
COPY --from=python-deps /opt/venv /opt/venv
# add venv to path
ENV PATH="/opt/venv/bin:$PATH" VIRTUAL_ENV="/opt/venv"

## Project Artifacts
# copy sources
COPY ./server/src/ /unpacker/server/src/
