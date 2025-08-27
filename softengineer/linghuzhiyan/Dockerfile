# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only maven descriptor first for better cache
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -e -q -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -e -q -DskipTests clean package

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
