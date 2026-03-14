FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="OpenDebt Team <opendebt@ufst.dk>"
LABEL org.opencontainers.image.source="https://github.com/ufst/opendebt"
LABEL org.opencontainers.image.description="OpenDebt Citizen Portal"

RUN addgroup -g 1000 opendebt && \
    adduser -u 1000 -G opendebt -s /bin/sh -D opendebt

WORKDIR /app

COPY --chown=opendebt:opendebt target/*.jar app.jar

USER opendebt

EXPOSE 8086

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
