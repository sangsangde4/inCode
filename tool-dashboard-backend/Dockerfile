# 多阶段构建 - 构建阶段
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 和 settings.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
COPY settings.xml .
RUN mvn dependency:go-offline -B -s settings.xml

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B -s settings.xml

# 运行阶段
FROM eclipse-temurin:17-jre

WORKDIR /app

# 创建非 root 用户
RUN groupadd -r spring && useradd -r -g spring spring

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志和上传文件目录
RUN mkdir -p /app/logs /app/uploads && \
    chown -R spring:spring /app

USER spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
