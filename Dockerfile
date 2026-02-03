FROM 172.19.208.1:5000/gencat-sic-builders/mvn-builder:1.0-3.9-21-openjdk
USER root

RUN apt-get update \
    && apt-get install -y tzdata \
    && ln -fs /usr/share/zoneinfo/Europe/Madrid /etc/localtime \
    && echo "Europe/Madrid" > /etc/timezone \
    && dpkg-reconfigure -f noninteractive tzdata \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/assets/multimedia
RUN mkdir -p /app/assets/multimedia/avatars \
    /app/assets/multimedia/cv
WORKDIR /app

COPY target/janus-backend-*.jar app.jar

CMD ["java","-jar","app.jar"]
