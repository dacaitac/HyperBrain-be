# 🧠 Contexto Cognitive Engine

Este documento define el comportamiento específico del agente para el módulo `cognitive`.

## 1. Misión del Módulo
El motor cognitivo actúa como el Gatekeeper de la salud mental y el gestor del aprendizaje acelerado (SRS). Orquesta LLMs para generar prompts diagnósticos y ajusta la carga de ingesta según biometría.

## 2. Invariantes del Módulo
* **Throttling Biométrico:** Bloquear la ingesta de nuevos temas si las tareas pendientes superan el umbral dinámico ajustado por el `Readiness Score`.
* **Pipeline FSM (LLM):** Implementar la máquina de estados que transiciona entre Active Recall (Prompt A), Priming (Prompt B), Estudio Quirúrgico (Prompt C) y Zettelkasten (Prompt D).
* **Regresión N-1:** Si el usuario falla en Active Recall reiteradamente, invocar diagnóstico de bases conceptuales antes de continuar.

## 3. Guía de Navegación Contextual
* Ancla superior: [[../GEMINI|Backend Root Context]]
* Readme Local: [[README-cognitive.md|Architecture]]
