# 🧠 Cognitive Engine: El Gatekeeper de la Mente

## 1. Propósito
Gestionar el aprendizaje acelerado y prevenir el agotamiento. Actúa como un regulador que decide qué información entra al sistema y cuándo debe ser repasada.

## 2. Fundamentos Teóricos
* **SRS (Spaced Repetition System):** Implementa algoritmos tipo Anki/SuperMemo para asegurar la retención a largo plazo.
* **Active Recall FSM:** Una máquina de estados que orquesta sesiones de evaluación mediante LLMs.
* **Throttling Biométrico:** Si la carga cognitiva pendiente supera el umbral (basado en biometría), el sistema bloquea la creación de nuevos nodos de aprendizaje.

## 3. Pipeline de Aprendizaje (LLM Orquestación)
1. **Prompt A (Diagnóstico):** Evalúa el estado actual.
2. **Prompt B (Priming):** Introduce conceptos si hay ignorancia total.
3. **Prompt C (Deep Dive):** Estudio quirúrgico para puntos ciegos.
4. **Prompt D (Consolidación):** Síntesis Zettelkasten.

---
[[../../../../../../../README|Volver al Nodo Raíz]] | [[GEMINI-cognitive|Directivas Cognitivas]]
