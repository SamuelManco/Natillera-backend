# Natillera API

CRUD REST del modelo de natillera, construido con Spring Boot + Maven + MySQL.

## Antes de correrlo

1. Crea la base de datos en MySQL (vacía, sin tablas — Hibernate las crea solas):
   ```sql
   CREATE DATABASE natillera_db;
   ```

2. Abre `src/main/resources/application.properties` y ajusta:
   ```
   spring.datasource.username=root
   spring.datasource.password=TU_CLAVE_AQUI
   ```

3. Corre el proyecto:
   ```bash
   mvn spring-boot:run
   ```
   Al iniciar, Hibernate crea automáticamente las 12 tablas a partir de las entidades (`spring.jpa.hibernate.ddl-auto=update`).

## Estructura

```
model/        -> las 12 entidades (una clase = una tabla)
repository/   -> interfaces JpaRepository (dan el CRUD básico gratis)
controller/   -> endpoints REST para cada entidad
```

## Endpoints disponibles (mismo patrón para las 12 entidades)

Ejemplo con `Rifa` (ruta: `/api/rifa`):

| Método | Ruta | Acción |
|---|---|---|
| POST | `/api/rifa` | Crear una rifa |
| GET | `/api/rifa` | Listar todas las rifas |
| GET | `/api/rifa/{id}` | Ver una rifa específica |
| PUT | `/api/rifa/{id}` | Actualizar una rifa |
| DELETE | `/api/rifa/{id}` | Eliminar una rifa |

**Todas las rutas disponibles:**
- `/api/rol`
- `/api/tipo-documento`
- `/api/categoria`
- `/api/estado`
- `/api/tipo-movimiento`
- `/api/natillera`
- `/api/persona`
- `/api/movimiento`
- `/api/rifa`
- `/api/boleta`
- `/api/liquidacion`
- `/api/liquidacionxpersona`

## Ejemplo de prueba en Postman

**Crear un rol** — `POST http://localhost:8080/api/rol`
```json
{
  "nombre": "admin",
  "descripcion": "Administrador de la natillera"
}
```

**Crear una persona** (usando el id del rol creado arriba) — `POST http://localhost:8080/api/persona`
```json
{
  "nombre": "Samuel",
  "apellido": "Perez",
  "numeroDocumento": "1000123456",
  "idTipoDocumento": 1,
  "idRol": 1,
  "correo": "samuel@correo.com",
  "fechaRegistro": "2026-09-13",
  "cuotaMinima": 50000
}
```

## Nota importante sobre este CRUD

Este CRUD es genérico y directo (expone las tablas casi tal cual) — es el punto de partida correcto para probar que el modelo funciona end-to-end contra MySQL. La lógica de negocio real (calcular liquidaciones, verificar ganador de rifa, calcular interés de préstamos, actualizar `Monto_Actual`) **todavía no está aquí** — son métodos de servicio adicionales que se construyen sobre estos repositorios, no reemplazan este CRUD base.
