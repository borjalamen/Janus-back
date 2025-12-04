#!/bin/bash

java -jar -Dspring.data.mongodb.uri=${urlMongoDB} -Dspring.data.mongodb.database=${indexMongoDB} -Dspring.data.mongodb.auto-index-creation=${databaseMongoDB} -Dserver.port=${portMongoDB} -Xms${XMS} -Xmx${XMX} app.jar
