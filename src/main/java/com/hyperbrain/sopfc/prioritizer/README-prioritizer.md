# ⚖️ Task Prioritizer: El Cerebro Matemático

## 1. Propósito
Calcular dinámicamente qué es lo más importante que el usuario debe hacer en un momento dado, considerando factores biétricos, temporales y de impacto.

## 2. Fundamentos Teóricos (La Ecuación)
La prioridad $S$ se calcula como:
$$S(n, t) = [W(n) \cdot \beta(B)] + [U(n)] - [Penalización(E, M)]$$
* **$W(n)$:** Valor intrínseco (Impacto/Esfuerzo).
* **$\beta(B)$:** Modulador Biométrico. Si el HRV es bajo, las tareas de alta carga mental bajan de prioridad.
* **$U(n)$:** Urgencia Sigmoidal. La urgencia crece exponencialmente conforme se acerca el deadline.

## 3. Algoritmos
* **Decaimiento Temporal:** Las tareas estancadas pierden prioridad o requieren re-evaluación.
* **Montecarlo Simulations:** Para proyectar probabilidades de cumplimiento de metas basadas en la velocidad actual.

---
[[../../../../../../../README|Volver al Nodo Raíz]] | [[GEMINI-prioritizer|Directivas de Priorización]]
