# Task Prioritizer

## 1. Misión
Recalcular dinámicamente la importancia relativa de cada ejecutable. Es el cerebro que decide qué debe hacerse ahora basándose en la realidad biológica y temporal del usuario.

## 2. Fundamentos Teóricos
- **Priorización Reactiva:** El score decae o aumenta automáticamente con el tiempo ($t$) y la cercanía al deadline ($t_{deadline}$) siguiendo una curva de urgencia sigmoide.
- **Modulación Biométrica:** El sistema penaliza o incentiva tareas según el `readiness_index` (HRV, Sueño). Si la energía es baja, las tareas de alto `mental_load` pierden prioridad para evitar el burnout.
- **Montecarlo & Valor Esperado:** Algoritmos que evalúan el retorno por unidad de tiempo/energía para maximizar el Micro-ROI de la jornada.

## 3. Grafo de Navegación
* [[../README.md|Arquitectura Global (Root)]]
