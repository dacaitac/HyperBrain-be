# 📅 Agenda Planner: Optimizador de Espacio-Tiempo

## 1. Propósito
Transformar la lista priorizada de tareas en un plan ejecutable en el calendario, maximizando el uso de franjas de alta energía y protegiendo el tiempo de descanso.

## 2. Fundamentos Teóricos
* **Stochastic Bin-packing:** Trata las franjas de tiempo del calendario como "contenedores" y las tareas como "ítems" con volumen (duración) y peso (prioridad).
* **Margen de Caos (20%):** El algoritmo nunca llena el 100% del tiempo; reserva un margen para imprevistos.
* **Cronobiología:** Alinea tareas de alta carga mental con las ventanas de mayor energía del usuario (ej. mañanas).

## 3. Funcionalidades
* **Auto-Scheduling:** Re-agenda automáticamente tareas fallidas.
* **Focus Mode Trigger:** Emite eventos para activar modos de concentración en dispositivos iOS cuando inicia un bloque de `Deep Work`.

---
[[../../../../../../../README|Volver al Nodo Raíz]] | [[GEMINI-planner|Directivas de Planning]]
