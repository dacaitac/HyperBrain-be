# 🧠 Contexto Sync Engine

El Sync Engine es el corazón de la orquestación, encargado de mantener la consistencia entre los sistemas externos (Apple, Notion) y el núcleo del sistema.

## Invariantes del Módulo
1. **SSOT (Single Source of Truth):** El Backend actúa como mediador, pero Notion es la Fuente de Verdad para metadatos ricos y Apple para entrada rápida de usuario.
2. **Transactional Outbox:** Toda propagación a sistemas externos DEBE pasar por la tabla `outbox_events` para garantizar consistencia eventual ante fallos de red.
3. **Mapeo Unívoco:** Las tablas de `sync_mappings` garantizan que un ID externo se mapee exactamente a un ID local.
4. **Monousuario:** El sistema opera bajo un contexto único de usuario, sin necesidad de IDs de tenant.

## Flujos de Sincronización
* **Inbound (Apple/Notion -> Backend):** Manejado vía Webhooks en `SyncController`.
* **Outbound (Backend -> Apple/Notion):** Manejado asíncronamente por `OutboxScheduler` y `ExecutableStatusChangedListener`.

## Configuración de Notion
El mapeo de propiedades está optimizado para la base de datos de tareas:
* **Name:** Título de la página.
* **Status:** Estado mapeado (`Not started`, `In progress`, `Done`).
* **Type:** Tipo de tarea (`Task`, `Habit`, `Activity`).
* **Energy/Impact/Mental Load:** Selects para analítica de rendimiento.
* **Date:** Rango de fechas de planificación.

---
*Nota: Se ha implementado un framework de pruebas E2E que valida estos flujos contra las APIs reales.*
