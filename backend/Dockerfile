# ============================================
# Build Stage
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar archivos de configuración Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Descargar dependencias (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Build de la aplicación
RUN mvn clean package -DskipTests -B

# ============================================
# Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="GetNet-Facturante"
LABEL version="1.0.0"
LABEL description="GetNet to Facturante Connector - Production"

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Instalar dependencias del sistema
RUN apk add --no-cache \
    curl \
    wget \
    tzdata \
    && rm -rf /var/cache/apk/*

# Crear directorios necesarios
RUN mkdir -p /app /var/log/app /backups && \
    chown -R appuser:appgroup /app /var/log/app /backups

WORKDIR /app

# Copiar JAR desde build stage
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Cambiar a usuario no-root
USER appuser

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC" \
    TZ=America/Argentina/Buenos_Aires

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Ejecutar aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

