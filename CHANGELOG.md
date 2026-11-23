# Changelog

Todos los cambios relevantes del proyecto. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el versionado, [Semantic Versioning](https://semver.org/lang/es/).

## [0.3.1] - Sin publicar

### Agregado
- Deploy en Render (plan gratuito), descripto en `render.yaml`: https://challenge-conexa.onrender.com
- Memoria de la JVM configurable con `JAVA_TOOL_OPTIONS` (60 % de la RAM del contenedor por defecto).
- Access log: una línea por request con método, path, status, duración, IP y `correlationId` (sin body ni headers; omite el health check).
### Corregido
- Swagger documenta el body de error tal como lo devuelve la API (`title`, `status`, `detail`, `errors` y `correlationId` en la raíz), en lugar del schema genérico de `ProblemDetail`.
- Las búsquedas por id y los listados de SWAPI usan caches separadas: un 404 cacheado por una búsqueda ya no puede llegar como respuesta vacía a un listado con la misma URI.

## [0.3.0] - 2026-11-22

### Agregado
- Reintentos ante fallas transitorias de SWAPI (conexión y 5xx) con backoff exponencial y jitter.
- Circuit breaker sobre SWAPI: con el circuito abierto responde `503` con `Retry-After` sin llamarlo.
- Rate limit: 10 requests por minuto por IP en `/auth/**` y 120 cada 15 minutos por usuario en `/api/**`, con `429` y `Retry-After`.
- `correlationId` en cada request: header `X-Correlation-Id`, logs y body de los errores.
- Health check público en `/actuator/health`.
- `Dockerfile` multi-stage con usuario sin privilegios y health check.
- README con la guía de integración, diagramas del flujo y la configuración, y este CHANGELOG.

### Cambiado
- `POST /auth/register` responde `201 Created` sin body (antes devolvía `id` y `username`).

## [0.2.0] - 2026-11-22

### Agregado
- Endpoints `GET /api/v1/{people|films|starships|vehicles}` con paginación (`page`, `size`) y filtros por `id` y/o `name`.
- Integración con swapi.tech: modelo común de dominio, nombres normalizados en camelCase y particularidades de SWAPI resueltas en el adapter.
- Cache de las respuestas de SWAPI con Caffeine (10 minutos).
- Errores de SWAPI traducidos a `502`, `503` y `504`; `401` como `ProblemDetail`.
- Documentación OpenAPI / Swagger con autenticación Bearer.
- Virtual threads.

### Eliminado
- Archivos de scaffolding sin uso (`package-info.java` y el test `contextLoads`).

## [0.1.0] - 2026-11-21

### Agregado
- Estructura del proyecto con arquitectura hexagonal.
- Registro (`POST /auth/register`) y login (`POST /auth/login`) con contraseñas en BCrypt y usuarios en memoria.
- Autenticación JWT (HS256) en todas las rutas de negocio.
- Errores en formato `ProblemDetail` con detalle por campo.

[0.3.1]: https://github.com/lucasgimeneze/challenge-conexa/compare/77d268d...HEAD
[0.3.0]: https://github.com/lucasgimeneze/challenge-conexa/compare/843ffed...77d268d
[0.2.0]: https://github.com/lucasgimeneze/challenge-conexa/compare/d454616...843ffed
[0.1.0]: https://github.com/lucasgimeneze/challenge-conexa/commits/d454616
