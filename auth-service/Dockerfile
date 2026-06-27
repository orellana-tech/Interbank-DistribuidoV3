# ETAPA 1: BUILD (imagen Debian con glibc, compatible con protoc)
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Instalar Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY src ./src

# Compilar (ahora protoc puede ejecutarse correctamente)
RUN mvn clean package -DskipTests

# ETAPA 2: RUNTIME (imagen ligera)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/auth-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 5001
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
