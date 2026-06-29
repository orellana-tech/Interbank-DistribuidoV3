# Usamos la imagen oficial y ligera de Java 21
FROM eclipse-temurin:21-jre-alpine

# Carpeta de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el ejecutable compilado por Maven
COPY target/pagos-service-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto REST y el puerto gRPC
EXPOSE 5002 9091

# Comando para encender Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
