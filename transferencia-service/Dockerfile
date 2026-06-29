# ETAPA 1: Construcción (Build) con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiamos el archivo pom y descargamos las dependencias primero (mejora el caché de Docker)
COPY pom.xml .
# Copiamos el código fuente y los archivos proto
COPY src ./src

# Compilamos el proyecto y generamos el .jar (ignoramos los tests para que sea más rápido)
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución (Run) con una imagen ligera de Java 21
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copiamos SOLO el .jar generado en la etapa anterior
COPY --from=builder /app/target/transferencias-service-1.0.0.jar app.jar

# Exponemos los puertos
EXPOSE 5003
EXPOSE 9092

# Ejecutamos el servicio
ENTRYPOINT ["java", "-jar", "app.jar"]