### Build Scripts
FROM alpine:3.12.0 as scripts

# install dos2unix to remove \r's from scripts when compiling on Windows machines
RUN apk add --no-cache dos2unix
# copy updater script
COPY docker/updater.sh /updater.sh
# remove \r's and set as executable
RUN dos2unix /updater.sh && \
    chmod +x /updater.sh


### Build Application
FROM openjdk:15-ea-jdk-alpine3.12

# Install Wine
RUN apk add --no-cache \
    wine \
    # wine runtime dependencies for our Windows application
    libpng freetype

# copy update script
COPY --from=scripts /updater.sh /packages/updater
