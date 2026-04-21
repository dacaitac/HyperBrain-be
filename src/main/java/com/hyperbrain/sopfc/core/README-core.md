# ⚙️ Core API: Gestor de Estado y Dependencias

## 1. Propósito
El Core API es el motor de ejecución central. Administra el ciclo de vida de los "Executables" (Tareas, Hábitos, Nodos de Aprendizaje) y resuelve el grafo de dependencias (DAG) que define el orden de ejecución.

## 2. Fundamentos Teóricos
* **DAG (Directed Acyclic Graph):** Las tareas no son aisladas; forman una red de dependencias. El Core resuelve qué tareas están "desbloqueadas" para su ejecución.
* **Estado Reactivo:** Implementa mecanismos de *Debounce* para consolidar múltiples cambios rápidos en un solo commit de estado.
* **4DX Implementation:** Mantiene la distinción entre Proyectos (Lag) y Executables (Lead).

## 3. Modelos Principales
* **CoreExecutable:** La unidad mínima de trabajo.
* **ExecutionProfile:** Define el costo (energía, carga mental, tiempo) de un ejecutable.
* **Project:** El contenedor de contexto y metas.

## 4. Transaccionalidad
Utiliza el patrón **Transactional Outbox**. Cada cambio de estado en un `Executable` inserta automáticamente un evento en la tabla `OUTBOX_EVENTS` dentro de la misma transacción de base de datos.

---
[[../../../../../../../README|Volver al Nodo Raíz]] | [[GEMINI-core|Directivas Técnicas de Core]]
