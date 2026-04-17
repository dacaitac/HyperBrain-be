FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiamos primero solo los archivos de Gradle para aprovechar la caché de capas
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Asegurar permisos de ejecución para gradlew
RUN chmod +x gradlew

# Copiamos el código fuente
COPY src src

# Construir el jar
RUN ./gradlew build -x test

# Imagen final ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 1015
ENTRYPOINT ["java", "-jar", "app.jar"]
