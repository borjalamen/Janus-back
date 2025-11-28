FROM registreimatges.sic.intranet.gencat.cat/gencatcloud/java:21.0.7-alpine3.20

RUN apk add tzdata \
    && cp /usr/share/zoneinfo/Europe/Madrid /etc/localtime \
    && echo "Europe/Madrid" > /etc/timezone \
    && apk del tzdata

COPY target/janus-backend-*.jar app.jar

COPY docker-run.sh /entrypoint.sh

# RUN mkdir -p /app/shared-data/
RUN chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]
