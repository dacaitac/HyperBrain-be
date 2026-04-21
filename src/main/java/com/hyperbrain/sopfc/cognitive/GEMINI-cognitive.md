# 🧠 Cognitive Engine Directives

## 1. Regulación
* El motor DEBE poder lanzar una `InsufficientCognitiveBandwidthException` si se intenta forzar una tarea de aprendizaje en un estado de agotamiento.

## 2. Interacción con LLMs
* Las respuestas de los LLMs deben ser parseadas y validadas contra el esquema `LearningState`. No confiar en texto libre para lógica de negocio.

## 3. Prompting Sugerido
*"Diseña la máquina de estados para que, tras tres fallos consecutivos en un concepto (Active Recall), el sistema lo marque como `StructuralDebt` y bloquee temas dependientes."*

---
[[../../../../../../../README|Volver al Nodo Raíz]]
