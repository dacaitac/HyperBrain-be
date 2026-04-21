# 🧠 Core API Directives

## 1. Gestión de Estado
* Prohibido cambiar el estado de un `Executable` sin pasar por el caso de uso correspondiente que valide las pre-condiciones del DAG.
* Todo cambio de estado DEBE emitir un `ExecutableStatusChangedEvent`.

## 2. Rendimiento
* Usar `WebFlux` o mecanismos no bloqueantes para operaciones de alta concurrencia si el volumen de webhooks es elevado.
* Implementar cacheo agresivo para la resolución del grafo de dependencias.

## 3. Prompting Sugerido
*"Crea un nuevo tipo de ejecutable que requiera validación de pre-condiciones personalizadas antes de pasar a estado PENDING, asegurando que el evento de cambio de estado se registre en el Outbox."*

---
[[../../../../../../../README|Volver al Nodo Raíz]]
