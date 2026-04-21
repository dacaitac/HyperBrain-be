# 🔄 Sync Engine: Capa de Integración y Anti-Corrupción

## 1. Propósito
El Sync Engine actúa como la membrana externa del sistema. Su responsabilidad es gestionar la comunicación bidireccional entre el SOPFC y los sistemas externos (Notion, iOS Calendar/Reminders), protegiendo al dominio interno de la complejidad y el ruido de las APIs externas.

## 2. Fundamentos Teóricos
* **Anti-Corruption Layer (ACL):** Traduce modelos externos volátiles a modelos de dominio estables.
* **Identity Map:** Mantiene la correspondencia entre IDs externos (ej. Notion Page ID) y UUIDs internos.
* **Semantic Hashing:** Calcula el hash del payload de negocio para determinar si una actualización externa contiene cambios reales, evitando ciclos de sincronización infinitos.

## 3. Arquitectura del Motor
* **Inbound Adapters:** Webhooks que reciben datos de Notion e iOS.
* **Outbound Adapters:** Clientes REST/GraphQL para persistir cambios en Notion o agendar en iOS.
* **Identity Map Store:** Tabla `SYNC_EXTERNAL_IDENTITY_MAP` en el esquema `sync_schema`.

## 4. Flujo de Sincronización (Semantic Hash)
1. Recibe Webhook -> 2. Busca `internal_id` en Identity Map -> 3. Compara `semantic_hash` -> 4. Si es diferente, emite evento de dominio -> 5. Actualiza `last_synced_at`.

---
[[../../../../../../../README|Volver al Nodo Raíz]] | [[GEMINI-sync|Directivas Técnicas de Sync]]
