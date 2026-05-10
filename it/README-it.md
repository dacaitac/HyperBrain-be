# 🧪 Integration Tests (it) - E2E Framework

Este módulo contiene las pruebas de integración de sistema y las validaciones de extremo a extremo (E2E) entre el backend y los sistemas externos (Apple & Notion).

## 1. Infraestructura E2E
Se ha implementado un framework de pruebas reales en `FullExternalSystemsSyncE2ETest.java` que valida:
1. **Apple -> Backend:** Creación de recordatorios vía `ReminderCLI` y recepción de webhooks.
2. **Backend -> Notion:** Propagación automática de cambios mediante el patrón **Transactional Outbox**.
3. **Validación Cruzada:** El sistema no solo verifica la base de datos local, sino que consulta las APIs reales de Notion y Apple para confirmar la integridad del dato.

## 2. Requisitos para Pruebas Reales
Para ejecutar los tests de integración externos exitosamente, se deben cumplir estas precondiciones:
* **ReminderCLI:** Corriendo en `localhost:1995`.
* **Sentinel:** Daemon activo para monitoreo de eventos de macOS.
* **Credenciales:** Variables de entorno `NOTION_TOKEN` y `NOTION_TASKS_DB_ID` configuradas.

## 3. Arquitectura de Verificación
* **Assumptions:** Los tests utilizan `ServiceAssumptions` para saltarse (no fallar) si los servicios externos no están disponibles.
* **Wait Strategy:** Dado que la sincronización es asíncrona (Outbox Scheduler), se utiliza una estrategia de reintentos (`waitFor`) para dar tiempo a la propagación entre nubes.

---
*Nota: El sistema ahora es monousuario, eliminando la necesidad de gestionar Tenant IDs en los flujos de integración.*
