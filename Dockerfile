# Sử dụng base image nhẹ, hỗ trợ tốt Alpine + Zulu (tối ưu memory)
FROM azul/zulu-openjdk:17-jre-headless

# Tạo thư mục app
WORKDIR /app

# Copy JAR (thay tên nếu khác)
COPY target/*.jar secretary-bot.jar

# Tune JVM để fit free tier (256MB → giới hạn heap ~180-200MB)
ENV JAVA_OPTS="-XX:InitialRAMPercentage=75.0 \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseSerialGC \
               -XX:MaxMetaspaceSize=128m \
               -Djava.security.egd=file:/dev/./urandom"

# Expose port
EXPOSE 8080

# Chạy app
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar