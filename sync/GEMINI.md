# 🧠 Contexto Sync Engine

El Sync Engine es el corazón de la orquestación, encargado de mantener la consistencia entre los sistemas externos (Apple, Notion) y el núcleo del sistema.

## 1. Invariantes del Módulo
1. **SSOT (Single Source of Truth):** La Base de Datos Local es la Única Fuente de Verdad. Los sistemas externos (Apple, Notion) son proyecciones de este estado.
2. **Ciclo de Sincronización Protegido:** 
   - Entrada: Webhook -> Update DB -> Emit Kafka Event (con `source_system`).
   - Salida: Consume Kafka Event -> Update ALL externals (excepto `source_system`).
3. **Validación con Testcontainers:** Es OBLIGATORIO que todos los tests de integración utilicen Testcontainers para PostgreSQL y Kafka. No se permiten mocks para la infraestructura de persistencia o mensajería en las pruebas de `it`.

## 2. Flujos de Sincronización
* **Inbound:** Los adaptadores de entrada actualizan la DB local. Esta actualización DEBE generar una entrada en el Outbox para Kafka.
* **Outbound:** El `SyncKafkaConsumer` escucha cambios en la DB local y orquestadores, propagándolos a Notion y Apple Reminders (vía `[[Apple/AppleReminderAPI/GEMINI|AppleReminderAPI]]`) de forma atómica.

## 3. Configuración de Notion
El mapeo de propiedades está optimizado para la base de datos de tareas:
* **Name:** Título de la página.
* **Status:** Estado mapeado (`Not started`, `In progress`, `Done`).
* **Type:** Tipo de tarea (`Task`, `Habit`, `Activity`).
* **Energy/Impact/Mental Load:** Selects para analítica de rendimiento.
* **Date:** Rango de fechas de planificación. Sincronizado obligatoriamente en Zona Horaria de **Bogotá (GMT-5)**.
* **Important:** Checkbox que se activa automáticamente si la prioridad en Apple Reminders es Alta (1-4).

## 4. Subagentes Especializados
* **notion_expert:** Subagente experto en la API REST de Notion y su integración en HyperBrain. Se debe invocar para cualquier cambio en los adaptadores de Notion, mapeo de propiedades o debugging de la sincronización.

## 5. Guía de Navegación Contextual
* Ancla superior: [[../GEMINI|Backend Root Context]]
* Readme Local: [[README-sync.md|Architecture]]

---
*Nota: Se ha implementado un framework de pruebas E2E que valida estos flujos contra las APIs reales.*
