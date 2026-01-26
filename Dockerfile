FROM eclipse-temurin:21-jdk
USER root

RUN mkdir -p /app/assets/multimedia
WORKDIR /app
COPY target/janus-backend-*.jar app.jar

COPY docker-run.sh /entrypoint.sh

# RUN mkdir -p /app/shared-data/
RUN chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]
