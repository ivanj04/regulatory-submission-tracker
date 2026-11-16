FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="Ivan Jiang <ivan.jiang@example.com>"
LABEL description="Regulatory Submission Document Tracker"

RUN addgroup -S regtracker && adduser -S regtracker -G regtracker

WORKDIR /app

COPY target/regulatory-submission-tracker-*.jar app.jar

RUN chown regtracker:regtracker app.jar

USER regtracker

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
