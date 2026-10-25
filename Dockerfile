# Drools HR Tagging 后端镜像：多阶段构建（根 POM 聚合后端单模块）
# 运行时约束 JDK 8 ~ 17（Drools 7.x 的 MVEL 依赖 JDK 21 起被移除的 java.lang.Compiler），构建与运行均使用 JDK 17。

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY drools-hr-tagging-backend/pom.xml drools-hr-tagging-backend/
# 先单独解析依赖，提高源码变更时的层缓存命中率
RUN mvn -B -q dependency:go-offline -f pom.xml -DskipTests || true
COPY drools-hr-tagging-backend/src drools-hr-tagging-backend/src
RUN mvn -B -q clean package -DskipTests -f pom.xml

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/drools-hr-tagging-backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
