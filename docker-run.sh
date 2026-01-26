#!/bin/bash
set -e

java \
  -Dspring.data.mongodb.uri="${urlMongoDB}" \
  -Dspring.data.mongodb.database="${databaseMongoDB}" \
  -Dspring.data.mongodb.auto-index-creation="${indexMongoDB}" \
  -Dserver.port="${portMongoDB}" \
  -Xms"${XMS}" \
  -Xmx"${XMX}" \
  -jar app.jar