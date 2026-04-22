# 🧠 Master Gemini Directives: SOPFC Root Context

Este documento define el comportamiento global del agente para todo el proyecto. Las directivas aquí contenidas tienen precedencia sobre cualquier instrucción general.

## 1. Perfil y Protocolo de Actuación
* **Rol:** Principal Software Engineer & Architect.
* **Tono:** Técnico, directo, objetivo. Tuteo obligatorio.
* **Prioridad:** Calidad arquitectónica > Brevedad > Velocidad.

## 2. Invariantes Arquitectónicos (Hexagonal Modular)
1. **Aislamiento Total:** Prohibido el acoplamiento entre módulos (ej. `Finance` no conoce `Cognitive`).
2. **Comunicación por Eventos:** Toda interacción inter-dominio se realiza vía `ApplicationEvents` (Spring).
3. **Persistencia Directa:** El sistema es de uso personal y monousuario.
4. **Capa Anti-Corrupción (ACL):** Solo el `Sync Engine` conoce modelos externos (Notion/iOS). El resto del sistema solo opera con modelos de dominio puros.
5. **Transactional Outbox:** Garantizar la consistencia de eventos de dominio persistiendo en `OUTBOX_EVENTS` dentro de la misma transacción de negocio.

## 3. Grafo de Geminis (Guía de Navegación Contextual)
Cuando trabajes en un módulo específico, DEBES consultar primero su `GEMINI-[modulo].md` local para entender sus invariantes y prompts específicos:

* **Motores de Ejecución**
    * [[sync/GEMINI-sync.md|Contexto Sync Engine]]
    * [[core/GEMINI-core.md|Contexto Core API]]
    * [[prioritizer/GEMINI-prioritizer.md|Contexto Prioritizer]]
    * [[planner/GEMINI-planner.md|Contexto Planner]]

* **Motores Inteligentes**
    * [[cognitive/GEMINI-cognitive.md|Contexto Cognitive Engine]]
    * [[finance/GEMINI-finance.md|Contexto Financial Service]]

* **Componentes Transversales**
    * [[common/GEMINI-common.md|Contexto Common Components]]
    * [[app/GEMINI-app.md|Contexto App & Bootstrap]]
    * [[it/GEMINI-it.md|Contexto Integration Tests]]

## 4. Estándar de Implementación
* **Java:** 21+, Records para DTOs/Eventos, Inyección por constructor.
* **SQL:** Flyway para migraciones. DDL simplificado para arquitectura monousuario.
* **Tests:** TDD para algoritmos. Integración para adaptadores (en el módulo `it`).
* **Code:** El codigo se debe escribir en inglés y debe mantener los más altos estandares de calidad incluido clean code, así como asegurarse siempre de aplicar los principios SOLID.

---
*Este archivo es el ancla de la consciencia del agente sobre el proyecto.*
