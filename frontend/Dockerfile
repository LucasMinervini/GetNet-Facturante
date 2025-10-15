# ============================================
# Build Stage
# ============================================
FROM node:18-alpine AS build

WORKDIR /app

# Copiar package files
COPY package.json package-lock.json ./

# Instalar dependencias
RUN npm ci --only=production

# Copiar código fuente
COPY . .

# Build de la aplicación
RUN npm run build

# ============================================
# Runtime Stage con NGINX
# ============================================
FROM nginx:alpine

# Metadata
LABEL maintainer="GetNet-Facturante"
LABEL version="1.0.0"
LABEL description="GetNet-Facturante Frontend - Production"

# Copiar build desde build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración de NGINX
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exponer puerto
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1

# Ejecutar NGINX
CMD ["nginx", "-g", "daemon off;"]

