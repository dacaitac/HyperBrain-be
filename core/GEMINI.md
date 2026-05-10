# 🧠 Contexto Core API

Este documento define el comportamiento específico del agente para el módulo `core`.

## 1. Misión del Módulo
El módulo `core` actúa como el orquestador de estado central y gestor de grafos (DAG). Controla el ciclo de vida de los `CoreExecutable` y `CoreCycle` y emite eventos de dominio cuando el estado cambia.

## 2. Invariantes del Módulo
* **Debounce Reactivo:** Aplicar patrones de debounce (ej: WebFlux) para consolidar mutaciones concurrentes y evitar ruido en el bus de eventos.
* **DAG Resolver:** Validar la integridad de dependencias entre tareas, impidiendo ciclos y optimizando el Habit Stacking.
* **Aislamiento:** NO PUEDE depender de ningún otro dominio (`sync`, `cognitive`, etc.), excepto de `common`.

## 3. Guía de Navegación Contextual
* Ancla superior: [[../GEMINI|Backend Root Context]]
* Readme Local: [[README-core.md|Architecture]]
