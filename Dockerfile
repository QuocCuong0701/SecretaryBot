# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (nhẹ hơn, tiết kiệm RAM)
# Sử dụng base image nhẹ, hỗ trợ tốt Alpine + Zulu (tối ưu memory)
FROM azul/zulu-openjdk:17-jre-headless

# Tạo thư mục app
WORKDIR /app

# Copy JAR (thay tên nếu khác)
COPY --from=builder /build/target/*.jar app.jar

# Tune JVM để fit free tier (256MB → giới hạn heap ~180-200MB)
ENV JAVA_OPTS="-XX:InitialRAMPercentage=70.0 -XX:MaxRAMPercentage=80.0 -XX:+UseG1GC"

# Expose port
EXPOSE 8080

# Chạy app
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar