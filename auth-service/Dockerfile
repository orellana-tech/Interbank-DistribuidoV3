# -------------------------------------------------------
# ETAPA 1: BUILD
# Usamos una imagen completa de Java 21 para compilar
# -------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiamos el pom.xml y descargamos dependencias primero
# (aprovecha la caché de Docker si el pom no cambia)
COPY pom.xml .
COPY src ./src

# Instalamos Maven (Alpine no lo trae por defecto)
RUN apk add --no-cache maven

# Compilamos y empaquetamos el proyecto (sin correr tests)
RUN mvn clean package -DskipTests

# -------------------------------------------------------
# ETAPA 2: RUNTIME
# Imagen ligera solo para ejecutar el JAR
# -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos solo el JAR generado en la etapa anterior
COPY --from=build /app/target/auth-service-0.0.1-SNAPSHOT.jar app.jar

# Exponemos los puertos:
# 5001 → HTTP (Spring Boot / Tomcat)
# 9090 → gRPC (Netty)
EXPOSE 5001
EXPOSE 9090

# Comando para iniciar el servicio
ENTRYPOINT ["java", "-jar", "app.jar"]
