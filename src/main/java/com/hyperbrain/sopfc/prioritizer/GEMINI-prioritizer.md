# 🧠 Task Prioritizer Directives

## 1. Invariantes Matemáticos
* El `PriorityScore` debe ser siempre un valor normalizado o comparable.
* No se permiten cálculos de prioridad que no consideren la carga mental (`mental_load`).

## 2. Integración
* Este motor es puramente reactivo. Se activa al recibir un `PrioritiesRecalculationRequested` o cuando cambia la biometría.

## 3. Prompting Sugerido
*"Ajusta el algoritmo de priorización para que las tareas marcadas como `HighDeepWork` sean penalizadas un 50% extra si el `ReadinessScore` biométrico es inferior a 60."*

---
[[../../../../../../../README|Volver al Nodo Raíz]]
