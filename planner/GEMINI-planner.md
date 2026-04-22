# 🧠 Gemini Directives: Agenda Planner

Este documento define el comportamiento específico del agente para el módulo `planner`.

## 1. Misión del Módulo
Acomodar automáticamente los ejecutables en la agenda usando técnicas de Bin Packing, gestionando la carga cognitiva y deduciendo el margen de caos.

## 2. Invariantes del Módulo
* **Stochastic Bin-packing:** Empaquetar tareas priorizadas en huecos de tiempo, ajustando según la carga cognitiva reportada.
* **Margen de Caos:** Reservar obligatoriamente un 20% de cada bloque de tiempo para imprevistos.
* **Aislamiento:** Reacciona a `PrioritiesCalculated` para emitir `TimeBlockCreated`.

## 3. Guía de Navegación Contextual
* Grafo de Geminis: [[../GEMINI.md|SOPFC Root Context]]
* Readme Local: [[README-planner.md|Planner Architecture]]
