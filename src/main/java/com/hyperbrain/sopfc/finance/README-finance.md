# 💰 Financial Service: ZBB & Micro-ROI

## 1. Propósito
Gestionar la salud financiera del ecosistema mediante el método de Presupuesto Base Cero (ZBB) y calcular la rentabilidad real de cada unidad de tiempo invertida por el usuario.

## 2. Fundamentos Teóricos
* **Zero-Based Budgeting (ZBB):** Todo ingreso debe ser asignado a una categoría (Gasto, Ahorro, Inversión) hasta que el balance sea cero.
* **Micro-ROI:** Cruza la duración real de un `TimeBlock` con los ingresos generados y los costos asociados (ej. suscripciones de software) para obtener el valor/hora real de una tarea.
* **Sinking Funds:** Previsión de gastos futuros mediante ahorros mensuales automatizados vinculados a proyectos de largo plazo.

## 3. Entidades Principales
* **FinTransaction:** Registro de partida doble.
* **FinAccount:** Cuentas reales o virtuales.
* **CostCenter:** El vínculo entre un `Project` del Core y su realidad financiera.

## 4. Flujo de Atribución
1. El Core marca tarea como `DONE` -> 2. Finance registra el tiempo consumido -> 3. Si es facturable, genera ingreso proyectado -> 4. Cruza con gastos operativos del periodo.

---
[[../../../../../../../README|Volver al Nodo Raíz]] | [[GEMINI-finance|Directivas Financieras]]
