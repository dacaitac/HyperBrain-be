# Core API (State & DAG Manager)

## 1. Misión
El módulo `core` es el corazón del sistema SOPFC. Su propósito es orquestar los estados, dependencias y propiedades básicas de los componentes ejecutables (Tareas, Hábitos, Nodos de Aprendizaje).

## 2. Fundamentos Teóricos
- **Directed Acyclic Graph (DAG):** Manejo de dependencias y secuencias que evita bucles y optimiza el *Habit Stacking*. El sistema resuelve el orden de ejecución basado en la topología del grafo.
- **Gestión de Estados Reactiva:** Modelado como Máquina de Estados Finitos (FSM) donde las transiciones de estado (`PENDING`, `IN_PROGRESS`, `DONE`, `BLOCKED`) emiten eventos asíncronos.
- **Debounce Reactivo:** Utiliza programación reactiva para agrupar múltiples actualizaciones rápidas de un mismo recurso, reduciendo la carga en la base de datos y en el bus de eventos.

## 3. Grafo de Navegación
* [[../README.md|Arquitectura Global (Root)]]
