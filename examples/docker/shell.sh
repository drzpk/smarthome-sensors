#!/bin/sh

docker run \
  -d \
  --name sensors \
  -p 8080:8080
  -v /path/to/external/configuration:/app/config \
  -v /path/to/external/log/directory:/app/logs \
  -e JAVA_TOOL_OPTIONS= \
  sensors:latest
