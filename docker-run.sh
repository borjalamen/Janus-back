#!/bin/bash
set -e

java \
  -Dspring.data.mongodb.uri="${urlMongoDB}" \
  -Dspring.data.mongodb.database="${databaseMongoDB}" \
  -Dspring.data.mongodb.auto-index-creation="${indexMongoDB}" \
  -Dserver.port="${portMongoDB}" \
  -Dgroq.api.key="${GROQ_API_KEY}" \
  -Xms"${XMS}" \
  -Xmx"${XMX}" \
  -jar app.jar