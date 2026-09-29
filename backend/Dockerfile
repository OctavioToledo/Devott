# Imagen de producción de la API. Se construye desde backend/: docker build -t devott-api .
# Los tests no corren acá (necesitan Docker para Testcontainers): los corre el CI antes de construir.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependencias primero, para que la caché de Docker las reuse mientras no cambie el pom.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests \
    && cp target/devott-api-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extraido

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 --no-create-home devott
USER devott

# Capas de la más estable a la que más cambia: un cambio de código solo reemplaza la última.
COPY --from=build /build/extraido/dependencies/ ./
COPY --from=build /build/extraido/spring-boot-loader/ ./
COPY --from=build /build/extraido/snapshot-dependencies/ ./
COPY --from=build /build/extraido/application/ ./

# Pensado para una instancia de 1 GB: heap proporcional al límite del contenedor, GC serial (menos memoria
# que G1 con un solo núcleo efectivo) y pilas de hilo más chicas. Se puede pisar con JAVA_TOOL_OPTIONS.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
ENV TZ=America/Argentina/Buenos_Aires

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
