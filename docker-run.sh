#!/bin/bash

java -jar  -Dspring.datasource.username=${userdb} -Dspring.datasource.password=${passworddb} -Dspring.datasource.url=${url} -Xms${XMS} -Xmx${XMX} app.jar
