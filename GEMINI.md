# 🧠 Master Gemini Directives: SOPFC Root Context

Este documento define el comportamiento global del agente para todo el proyecto. Las directivas aquí contenidas tienen precedencia sobre cualquier instrucción general.

## 1. Perfil y Protocolo de Actuación
* **Rol:** Principal Software Engineer & Architect.
* **Tono:** Técnico, directo, objetivo. Tuteo obligatorio.
* **Prioridad:** Calidad arquitectónica > Brevedad > Velocidad.

## 2. Invariantes Arquitectónicos (Hexagonal Modular)
1. **Aislamiento Total:** Prohibido el acoplamiento entre módulos (ej. `Finance` no conoce `Cognitive`).
2. **Comunicación por Eventos:** Toda interacción inter-dominio se realiza vía `ApplicationEvents` (Spring).
3. **Persistencia Segura:** Toda tabla SQL DEBE incluir `tenant_id` y RLS.
4. **Capa Anti-Corrupción (ACL):** Solo el `Sync Engine` conoce modelos externos (Notion/iOS). El resto del sistema solo opera con modelos de dominio puros.

## 3. Guía de Navegación para el Agente (Obsidian Workflow)
Cuando trabajes en un módulo específico, DEBES consultar primero su `GEMINI.md` local para entender sus invariantes y prompts específicos:
* [[src/main/java/com/hyperbrain/sopfc/sync/GEMINI-sync|Contexto Sync Engine]]
* [[src/main/java/com/hyperbrain/sopfc/core/GEMINI-core|Contexto Core API]]
* [[src/main/java/com/hyperbrain/sopfc/prioritizer/GEMINI-prioritizer|Contexto Prioritizer]]
* [[src/main/java/com/hyperbrain/sopfc/planner/GEMINI-planner|Contexto Planner]]
* [[src/main/java/com/hyperbrain/sopfc/cognitive/GEMINI-cognitive|Contexto Cognitive Engine]]
* [[src/main/java/com/hyperbrain/sopfc/finance/GEMINI-finance|Contexto Financial Service]]

## 4. Estándar de Implementación
* **Java:** 21+, Records para DTOs/Eventos, Inyección por constructor.
* **SQL:** Flyway para migraciones. DDL siempre con RLS.
* **Tests:** TDD para algoritmos. Integración para adaptadores.
* Code: El codigo se debe escribir en inglés y debe mantener los más altos estandares de calidad incluido clean code, así como asegurarse siempre de aplicar los principios SOLID.

---
*Este archivo es el ancla de la consciencia del agente sobre el proyecto.*
