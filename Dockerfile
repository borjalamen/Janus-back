FROM 172.19.208.1:5000/gencat-sic-builders/mvn-builder:1.0-3.9-21-openjdk
USER root

RUN apk add tzdata \
    && cp /usr/share/zoneinfo/Europe/Madrid /etc/localtime \
    && echo "Europe/Madrid" > /etc/timezone \
    && apk del tzdata    

RUN mkdir -p /app/assets/multimedia
RUN mkdir -p /app/assets/multimedia/avatars
RUN mkdir -p /app/assets/multimedia/cv

WORKDIR /app

COPY target/janus-backend-*.jar app.jar
COPY docker-run.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh
CMD ["/entrypoint.sh"]