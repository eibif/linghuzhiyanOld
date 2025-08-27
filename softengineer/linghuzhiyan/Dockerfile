# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only maven descriptor first for better cache
COPY pom.xml ./
# Optional: custom Maven settings for faster mirrors
COPY maven-settings.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2 mvn \
	-U -DskipTests \
	-Dorg.slf4j.simpleLogger.showDateTime=true \
	-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=info \
	dependency:go-offline

# Copy sources and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn \
	-DskipTests \
	-Dorg.slf4j.simpleLogger.showDateTime=true \
	-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=info \
	clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /opt/app

# Timezone and locale (optional)
ENV TZ=Asia/Shanghai

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# Use prod profile via env override if needed
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
