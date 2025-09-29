# Getnet → Facturante (AFIP) Connector

Sistema completo para conectar transacciones de Getnet con el sistema de facturación Facturante (AFIP).

## Características Implementadas

### ✅ Configuración de Facturación
- **Pantalla de Configuración**: CUIT, punto de venta, tipo de comprobante, IVA por defecto
- **Reglas de Facturación**: Configurar facturar solo transacciones `PAID`
- **Consumidor Final vs CUIT**: Configuración automática según documento del cliente
- **Persistencia**: Configuración guardada en base de datos PostgreSQL

### ✅ Funcionalidades Principales
- **Backend**: Spring Boot 3 (Java 17), REST API completa
- **Frontend**: React + Vite con interfaz moderna y responsive
- **Base de Datos**: PostgreSQL con Docker Compose
- **Webhooks**: Procesamiento automático de transacciones Getnet
- **Facturación**: Integración con Facturante para emisión de comprobantes AFIP

## Arquitectura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   PostgreSQL    │
│   (React)       │◄──►│   (Spring Boot) │◄──►│   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   Facturante    │
                       │   (AFIP API)    │
                       └─────────────────┘
```

## Requisitos
- Java 17+
- Maven 3.9+
- Node 18+
- Docker + Docker Compose

## Instalación y Configuración

### 1. Clonar y configurar
```bash
git clone <repository>
cd getnet-facturante
cp .env.example .env
```


```

### 3. Levantar servicios
```bash
# Base de datos
docker compose up -d

# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd ../frontend
npm install
npm run dev
```

### 4. Configurar facturación
1. Abrir http://localhost:5173
2. Hacer clic en "Configuración" en el header
3. Completar datos de la empresa:
   - CUIT Empresa
   - Razón Social
   - Punto de Venta
   - Tipo de Comprobante
   - IVA por defecto
4. Configurar reglas de facturación
5. Guardar configuración

## Uso

### Configuración de Facturación
- **CUIT Empresa**: CUIT de la empresa emisora
- **Punto de Venta**: Número de punto de venta AFIP
- **Tipo de Comprobante**: FA (A), FB (B), FC (C)
- **IVA por Defecto**: Porcentaje de IVA a aplicar
- **Facturar solo PAID**: Solo facturar transacciones pagadas
- **Consumidor Final**: Configuración automática para clientes sin CUIT

### Procesamiento de Transacciones
1. Las transacciones llegan vía webhook de Getnet
2. Se procesan automáticamente según la configuración
3. Se generan facturas en Facturante para transacciones PAID
4. Se pueden descargar los PDFs desde la interfaz

### API Endpoints
- `GET /api/billing-settings/active` - Obtener configuración activa
- `PUT /api/billing-settings/{id}` - Actualizar configuración
- `GET /api/transactions` - Listar transacciones
- `GET /api/invoices/pdf/{id}` - Descargar factura PDF

## Estructura del Proyecto

```
├── backend/
│   ├── src/main/java/com/gf/connector/
│   │   ├── domain/           # Entidades JPA
│   │   ├── dto/             # DTOs para API
│   │   ├── repo/            # Repositorios
│   │   ├── service/         # Lógica de negocio
│   │   ├── web/             # Controladores REST
│   │   └── facturante/      # Integración Facturante
│   └── src/main/resources/
│       └── application.yml  # Configuración
├── frontend/
│   ├── src/pages/           # Componentes React
│   ├── src/styles.css       # Estilos
│   └── package.json
└── docker-compose.yml       # Infraestructura
```

## Roadmap

### ✅ Completado
- [x] Configuración persistente de facturación
- [x] Reglas de facturación configurables
- [x] Pantalla de configuración en frontend
- [x] Integración con Facturante
- [x] Procesamiento automático de webhooks

### 🚧 En Desarrollo
- [ ] Seguridad JWT
- [ ] Múltiples configuraciones de facturación
- [ ] Jobs de reconciliación
- [ ] Reportes y estadísticas

### 📋 Próximamente
- [ ] Notificaciones por email
- [ ] API para integraciones externas
- [ ] Dashboard con métricas
- [ ] Backup automático de configuración
