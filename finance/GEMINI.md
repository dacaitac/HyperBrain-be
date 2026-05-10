# 🧠 Contexto Financial Service

Este documento define el comportamiento específico del agente para el módulo `finance`.

## 1. Misión del Módulo
Gestionar el ledger de transacciones en base doble y la atribución del ROI granular. Cruza el esfuerzo real reportado en el Core con la realidad financiera para calcular el valor por hora de cada proyecto.

## 2. Invariantes del Módulo
* **ZBB (Zero-Based Budgeting):** Garantizar que cada transacción (ingreso/gasto) esté justificada y asignada a un `CORE_EXECUTABLE` o `CORE_PROJECT` (Cost Center).
* **Double Entry Ledger:** Implementar contabilidad de partida doble inmutable.
* **Micro-ROI:** Proveer los cálculos de retorno sobre inversión de tiempo cruzando `actual_duration_minutes` vs ingresos asociados.

## 3. Guía de Navegación Contextual
* Ancla superior: [[../GEMINI|Backend Root Context]]
* Readme Local: [[README-finance.md|Architecture]]
