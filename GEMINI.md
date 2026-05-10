# 🧠 Master Gemini Directives: SOPFC Root Context

Este documento define el comportamiento global del agente para todo el proyecto backend.

## 1. Perfil y Protocolo de Actuación
* **Rol:** Principal Software Engineer & Architect.
* **Referencia Teórica:** [[CORE-ARCHITECTURE]]
* **Prioridad:** Calidad arquitectónica > Brevedad > Velocidad.

## 2. Invariantes Arquitectónicos (Hexagonal Modular)
1. **Aislamiento Total:** Prohibido el acoplamiento entre módulos (ej. `Finance` no conoce `Cognitive`).
2. **Comunicación Desacoplada (Kafka):** Toda interacción inter-dominio se realiza mediante eventos en Kafka. Está estrictamente prohibido usar `ApplicationEvents` de Spring para comunicación entre módulos; estos solo se permiten para eventos internos de un mismo módulo.
3. **Transactional Outbox:** El patrón Outbox es obligatorio para garantizar que los eventos se publiquen en Kafka solo si la transacción de base de datos local es exitosa.

## 3. Protocolo de Validación (OBLIGATORIO)
Antes de dar por finalizada una tarea en este módulo, DEBES:
1. Ejecutar `./gradlew build` desde la raíz de `HyperBrain-be/`.
2. Si el cambio afecta a los adaptadores, ejecutar los tests en `it/`.
3. Reportar cualquier error de compilación inmediatamente.

## 4. Grafo de Geminis Local
* [[app/GEMINI|App & Bootstrap]]
* [[cognitive/GEMINI|Cognitive Domain]]
* [[common/GEMINI|Common Utilities]]
* [[core/GEMINI|Core API]]
* [[finance/GEMINI|Finance Domain]]
* [[it/GEMINI|Integration Tests]]
* [[planner/GEMINI|Planner Engine]]
* [[prioritizer/GEMINI|Prioritizer Engine]]
* [[sync/GEMINI|Sync Engine]]

---
*Ancla superior:* [[../GEMINI|Root Context]]
*Este archivo es el ancla de la consciencia del agente sobre el proyecto backend.*
