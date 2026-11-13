# Multi-stage Dockerfile para ChefBot Culinário

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copia arquivos do Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Dá permissão de execução ao wrapper
RUN chmod +x gradlew

# Baixa dependências para cache
RUN ./gradlew dependencies --no-daemon || true

# Copia o código fonte
COPY src src

# Executa o build gerando o jar sem rodar testes demorados no container
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime enxuto
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria usuário não-root por segurança
RUN addgroup -S chefbot && adduser -S chefbot -G chefbot
USER chefbot

# Copia o artefato compilado do Stage 1
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
