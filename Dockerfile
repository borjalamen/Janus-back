FROM eclipse-temurin:21-jdk
USER root

RUN apt-get update \
    && apt-get install -y tzdata \
    && ln -fs /usr/share/zoneinfo/Europe/Madrid /etc/localtime \
    && echo "Europe/Madrid" > /etc/timezone \
    && dpkg-reconfigure -f noninteractive tzdata \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/assets/multimedia
WORKDIR /app

COPY target/janus-backend-*.jar app.jar

CMD ["java","-jar","app.jar"]
