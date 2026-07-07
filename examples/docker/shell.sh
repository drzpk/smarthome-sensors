#!/bin/sh

docker run \
  -d \
  --name sensors \
  -p 8080:8080
  -v /path/to/config/file.conf:/config.conf:ro \
  -v /path/to/external/logback.xml:/logback.xml \
  -e CONFIG_FILE=/config.conf \
  -e LOGBACK_CONFIG_FILE=/logback.xml \
  -e JAVA_TOOL_OPTIONS= \
  sensors:latest
