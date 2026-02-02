FROM maven:3.9.9-eclipse-temurin-21-alpine
USER root

RUN apk add tzdata \
    && cp /usr/share/zoneinfo/Europe/Madrid /etc/localtime \
    && echo "Europe/Madrid" > /etc/timezone \
    && apk del tzdata

RUN mkdir -p /app/assets/multimedia
WORKDIR /app

COPY target/janus-backend-*.jar app.jar
COPY docker-run.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]