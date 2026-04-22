# 🧠 Contexto Integration Tests (it)

Este módulo es el guardián de la integridad sistémica. Valida que el backend monousuario interactúe correctamente con el ecosistema Apple y Notion mediante pruebas reales de extremo a extremo (E2E).

## Invariantes de Calidad
1. **Validación Multi-Sistema Obligatoria:** No basta con verificar la base de datos local. Los tests E2E deben realizar consultas directas (GET) a las APIs de Apple y Notion para confirmar que los datos se crearon o actualizaron físicamente.
2. **Uso Personal:** Todas las pruebas asumen un único usuario. No se permite el uso de contextos de tenant.
3. **Resiliencia Asíncrona:** Dado que la sincronización utiliza el patrón Transactional Outbox, se deben usar estrategias de reintento (`waitFor`) con tiempos de espera realistas (ej. 20s para Notion).
4. **Protección de Entorno:** El uso de `ServiceAssumptions` es obligatorio para evitar que el build falle en entornos donde los servicios de Apple (Vapor Bridge) no estén activos.

## Framework E2E (`FullExternalSystemsSyncE2ETest`)
Este test representa el estándar de oro del proyecto y valida:
* **Creación:** Apple -> Webhook -> Local DB -> Notion (Verificación por ID).
* **Actualización:** Apple -> Webhook -> Local DB -> Notion (Verificación por contenido).
* **Limpieza:** Borrado manual de recursos externos para mantener el entorno de pruebas saneado.

## Requisitos de Ejecución
* **NOTION_TOKEN:** Variable de entorno con acceso a la base de datos.
* **ReminderCLI:** Corriendo en puerto 1995.
* **Sentinel:** Daemon activo.

---
*Este módulo garantiza que cada cambio en el código mantenga la promesa de sincronización fluida entre sistemas.*
