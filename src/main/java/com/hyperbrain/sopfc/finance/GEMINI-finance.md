# 🧠 Financial Service Directives

## 1. Integridad Transaccional
* Toda transacción financiera DEBE ser inmutable. Las correcciones se realizan mediante transacciones de ajuste (partida doble).
* El balance de una `FinAccount` debe ser recalculado periódicamente a partir del histórico de transacciones para asegurar consistencia.

## 2. Aislamiento
* Finance solo conoce el `executable_id` como una referencia débil (UUID). No realiza consultas directas a las tablas del Core.

## 3. Prompting Sugerido
*"Implementa el cálculo de Micro-ROI para un `Project` específico, sumando todas las transacciones vinculadas a sus ejecutables y dividiendo por la suma de `actual_duration_minutes` de los bloques de tiempo correspondientes."*

---
[[../../../../../../../README|Volver al Nodo Raíz]]
