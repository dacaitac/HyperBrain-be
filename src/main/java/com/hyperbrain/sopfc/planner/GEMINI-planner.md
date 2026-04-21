# 🧠 Agenda Planner Directives

## 1. Reglas de Negocio
* Nunca solapar bloques de tiempo (`TimeBlocks`).
* Las tareas de `Habit` tienen prioridad de ubicación sobre tareas esporádicas.

## 2. Outputs
* El resultado de este motor debe ser una serie de `TimeBlockCreatedEvent` que el `Sync Engine` usará para escribir en el calendario.

## 3. Prompting Sugerido
*"Optimiza el empaquetado de tareas para que deje un espacio de 15 minutos entre bloques de más de 90 minutos de duración (Buffer de recuperación cognitiva)."*

---
[[../../../../../../../README|Volver al Nodo Raíz]]
