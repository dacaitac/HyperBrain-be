# 🧠 Gemini Directives: Common Components

Este documento define el comportamiento específico del agente para el módulo `common`.

## 1. Misión del Módulo
Proveer los bloques de construcción transversales (eventos de dominio, excepciones, outbox port, utilidades) utilizados por todos los módulos para implementar una arquitectura EDA resiliente.

## 2. Invariantes del Módulo
* **Transactional Outbox:** Alojar la lógica del puerto y el scheduler que garantiza la consistencia eventual entre mutaciones de DB y publicación de eventos.
* **Cero Dependencias Circulares:** `common` NO PUEDE depender de ningún otro módulo interno.
* **Resiliencia:** Proveer patrones de reintento (Retry) y cortocircuitos (Circuit Breaker) para el manejo de fallos en APIs externas.

## 3. Guía de Navegación Contextual
* Grafo de Geminis: [[../GEMINI.md|SOPFC Root Context]]
* Readme Local: [[README-common.md|Common Components Architecture]]
