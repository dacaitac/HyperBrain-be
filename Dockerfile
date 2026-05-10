FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar archivos base de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copiar build files de cada módulo para caché de dependencias
COPY app/build.gradle app/
COPY common/build.gradle common/
COPY core/build.gradle core/
COPY sync/build.gradle sync/
COPY cognitive/build.gradle cognitive/
COPY finance/build.gradle finance/
COPY planner/build.gradle planner/
COPY prioritizer/build.gradle prioritizer/
COPY it/build.gradle it/

RUN chmod +x gradlew
# Descargar dependencias (caché)
RUN ./gradlew dependencies --no-daemon || true

# Copiar el resto del código
COPY . .

# Construir el jar del módulo app (el ejecutable)
RUN ./gradlew :app:bootJar -x test --no-daemon

# Imagen final ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
