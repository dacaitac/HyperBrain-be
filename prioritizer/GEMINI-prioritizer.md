# 🧠 Gemini Directives: Task Prioritizer

Este documento define el comportamiento específico del agente para el módulo `prioritizer`.

## 1. Misión del Módulo
El módulo `prioritizer` es el motor matemático reactivo. Su responsabilidad es re-evaluar la prioridad de los `CoreExecutable` integrando telemetría biométrica y decaimiento temporal.

## 2. Invariantes del Módulo
* **Ecuación de Priorización:** Implementar la fórmula $S(n, t)$ que integra peso de esfuerzo, urgencia (curva sigmoide) y modulador biométrico.
* **Simulación Montecarlo:** Utilizar simulaciones para predecir el score esperado en escenarios de incertidumbre de esfuerzo.
* **Reactividad:** No tiene BD propia; reacciona a eventos para actualizar `priority_score` en el Core.

## 3. Guía de Navegación Contextual
* Grafo de Geminis: [[../GEMINI.md|SOPFC Root Context]]
* Readme Local: [[README-prioritizer.md|Prioritizer Architecture]]
