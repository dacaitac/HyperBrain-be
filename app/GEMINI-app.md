# 🧠 Gemini Directives: App & Bootstrap

Este documento define el comportamiento específico del agente para el módulo `app`.

## 1. Misión del Módulo
Punto de entrada de Spring Boot y centro de orquestación (Bootstrap). Administra la configuración externalizada (12-Factor App) y la evolución del esquema mediante Flyway.

## 2. Invariantes del Módulo
* **12-Factor App:** Toda configuración sensible o variable debe inyectarse vía variables de entorno (definidas en `application.yml`).
* **Persistencia Simple:** Garantizar que el DDL esté simplificado para uso personal, sin requerimientos de aislamiento complejos.
* **Bootstrap Puro:** Cero lógica de negocio; solo composición de beans y adaptadores de infraestructura global.

## 3. Guía de Navegación Contextual
* Grafo de Geminis: [[../GEMINI.md|SOPFC Root Context]]
* Readme Local: [[README-app.md|App & Bootstrap Architecture]]
