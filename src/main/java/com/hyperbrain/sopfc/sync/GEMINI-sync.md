# 🧠 Sync Engine Directives

## 1. Responsabilidad Estricta
Este motor es el ÚNICO que puede conocer clases como `NotionPage`, `AppleReminder` o cualquier DTO de APIs externas.
Preferiblemente no usar H2.

## 2. Invariantes de Código
* **Idempotencia:** Todos los adaptadores de entrada deben validar el `semantic_hash` antes de procesar.
* **Mapeo de Identidad:** Antes de crear una entidad en el Core, verificar si ya existe una relación en `SYNC_EXTERNAL_IDENTITY_MAP`.
* **Seguridad:** Los tokens de API deben gestionarse vía `Secrets` y nunca loguearse.

## 3. Prompting Sugerido para Cambios
*"Implementa un nuevo adaptador de entrada para [Sistema] asegurando que la traducción al modelo de dominio `CoreExecutable` pase por el Identity Map y valide el hash semántico para evitar duplicados."*

---
[[../../../../../../../README|Volver al Nodo Raíz]]
