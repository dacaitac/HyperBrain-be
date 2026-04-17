# 🧭 Documento de Arquitectura: Sistema de Orquestación Productiva Diaria Integrada (Notion + iOS)

---

### 🌐 Propósito

Diseñar un sistema de ejecución y planificación diaria altamente automatizado que sincronice y orqueste de forma inteligente tareas, rutinas y eventos. El objetivo central es maximizar el avance hacia los objetivos personales y profesionales (MCI, proyectos) según prioridades y disponibilidad horaria.

Todo el gobierno de datos se centraliza en Notion (como fuente de verdad), mientras que la ejecución se realiza a través de las interfaces nativas de iOS (Reminders, Calendar, Shortcuts) para garantizar una fricción cognitiva mínima.

---

### 🏛️ Fundamentos de Productividad Aplicados

El sistema se sostiene en cuatro pilares metodológicos:

1. **4DX (Las 4 Disciplinas de la Ejecución):**
    * *Focus on the Wildly Important Goal (MCI).*
    * Actuar sobre *Lead Measures* (las tareas con mayor apalancamiento).
    * Mantener un *Scoreboard* visible (progreso en Notion).
    * Crear una cadencia de rendición de cuentas (replanificación dinámica).
2. **Hábitos Atómicos (James Clear):**
    * Diseño del entorno: Notion como sistema de referencia, iOS como sistema de ejecución.
    * Integración de rutinas y acciones a nivel de identidad ("Yo soy el tipo de persona que...").
    * Hábitos anclados a franjas específicas del día.
3. **GTD + Deep Work + Matriz de Eisenhower:**
    * Ordenar tareas según importancia y urgencia de forma automatizada.
    * Crear y proteger bloques de atención profunda.
    * Descargar la mente y minimizar la fricción cognitiva.
4. **Time Blocking Inteligente:**
    * Distribución de la agenda y agrupación de tareas según el nivel de energía, carga mental y contexto.
    * Ejecución de bloques de alta calidad ajustados a tu cronotipo personal.

---

### 🏠 Arquitectura General y Base de Datos

El sistema divide su arquitectura entre el panel de control (Notion) y los dispositivos de ejecución (iOS).

#### 1. Panel de Control (Notion DBs)
* **Cycles DB:** Contenedor de nivel superior para Rutinas, Proyectos, Objetivos, Hábitos y Metas Crucialmente Importantes (MCIs).
* **Tasks DB:** Repositorio de acciones ejecutables y tareas individuales del día.
* **Activity DB:** Registro de eventos y compromisos con horario fijo.

**Propiedades Clave en Notion (Especialmente en Tasks DB):**
* **Manuales:** `Type` (Task, Activity, Habit, Agenda), `Status`, `Frecuency`, `Date`, `Impact`, `Energy`, `Mental Load`, `Estimate Duration`, `Important`.
* **Calculadas:** `Effort`, `Urgence/Urgence Value`, `Priority Score`.
* **Relacionales:** `Cycle`, `Subtasks`, `Objective`, `MCI`, `Parent Cycle`.
* **Control de Planificación:** `Planned` (Checkbox), `Time Block`, `Date.start`, `Date.end`.

#### 2. Dispositivos de Ejecución (iOS)
* **Reminders:** Notificaciones individuales sincronizadas 1:1 con las `Tasks` clave del día.
* **Calendar:** Agenda visual que mapea los eventos (`Activity`, `Agenda`, `Routine`, `Habit`).

---

### ⚖️ Motores de Ejecución y Módulos del Sistema

El núcleo del sistema opera a través de cuatro motores principales:

#### 1. Task Prioritizer Engine
Se encarga de evaluar qué se debe hacer hoy.
* **Filtro Inicial:** Selecciona tareas tipo `Task`, `Habit` o `Activity` donde `Status != Done`, la `Date` es igual a hoy, y el `Priority Score >= 6`.
* **Fórmula de Prioridad (Priority Score):** Calcula el peso dinámico usando los valores de las propiedades. El sistema contempla las siguientes variables para su ponderación final:
    * Modelo A: *Impacto (35%) + Urgencia (30%) + Inverso del Esfuerzo (20%) + Importancia (15%)*.
    * Modelo B: *Impacto (0.4) + Urgencia Normalizada (0.3) + Inverso del Esfuerzo [5 - Effort] (0.1) + Alineación de Energía (0.1) + Ajuste de Carga Mental (0.1)*.

#### 2. Agenda Planner Engine
Asigna el "cuándo" de forma inteligente, operando en 4 fases:
* **Fase 1: Inputs.** Lee las `Tasks`, `Activity` y `Routine` del día. Construye un mapa de disponibilidad dividido en franjas (Mañana, Tarde, Noche). Resta el tiempo ya ocupado por reuniones de iOS Calendar (excluyendo *holidays*), actividades personales y rutinas planificadas.
* **Fase 2: Clasificación y Asignación.** Empaca las tareas disponibles en las franjas libres según su esfuerzo, nivel de energía requerido, carga mental y bloque preferido. Usa un cursor de minutos para calcular los `startTime` y `endTime` reales. Escribe los resultados en Notion (`Time Block`, `Date.start`, `Date.end`) y marca `Planned = true`.
* **Fase 3: Sincronización con Reminders.** Filtra las tareas planificadas (`Planned = true` y `Date.start != null`). Crea o actualiza en Reminders utilizando: `title` = Name, `dueDate` = Date.start, `notes` = https://www.notion.com/use-case/to-do-list, y lee el estado para `isCompleted`.
* **Fase 4: Replanificación Dinámica.** Si ocurren cambios (eventos nuevos, tareas completadas antes/después de tiempo), el motor replanifica los bloques teniendo en cuenta la hora actual real.

#### 3. Habit Generator
Inyecta acciones repetitivas diarias de forma automática.
* **Proceso:** Busca en Notion (`Cycles.Type = Routine`). Clona las tareas internas (`Type = Habit`) y las genera como un `Task` ejecutable para el día de hoy, conservando sus propiedades clave originales (Impact, Energy, etc.). Estas nuevas tareas pasan inmediatamente al *Task Prioritizer*.

#### 4. Sync Engine
Capa de comunicación bidireccional continua.
* **Notion ↔ Reminders:** Mantiene sincronizado el estado (`Done` / Eliminado). Si se completa en iOS, se marca en Notion.
* **Notion ↔ Calendar:** Crea un evento en el calendario de iOS si en Notion hay una `Activity` o rutina. A la inversa, si hay un nuevo evento en iOS Calendar, crea el registro `Agenda` correspondiente en Notion para bloquear ese tiempo.

---

### ⏳ Automatización y Triggers

La orquestación ocurre mediante eventos automatizados en momentos clave:

| Trigger / Disparador | Hora / Momento | Acción Ejecutada |
| :--- | :--- | :--- |
| **Shortcut Diario (iOS)** | 05:30 AM | Dispara en cascada: Habit Generator ➔ Task Prioritizer ➔ Agenda Planner ➔ Sync Engine. |
| **Gatillo Manual** | Al ejecutar una Rutina | Dispara: Habit Generator ➔ Replanificación Dinámica de la agenda. |
| **Completar Recordatorio** | Automático (Inmediato) | Marca la propiedad `Task` como `Done` en la base de datos de Notion. |
| **Nuevo Evento iOS Calendar** | Dinámico (Automático/Manual) | Crea el registro tipo `Agenda` en Notion si no existe, activando la Fase 4 del Planner. |

---

### 🚀 Resultado Esperado

* Agenda diaria en iOS Calendar perfectamente distribuida y balanceada según energía, tiempo y objetivos.
* Tareas clave (Lead Measures) ejecutables y altamente visibles en Reminders.
* Recordatorios ejecutivos con enlace directo al contexto en Notion.
* Sincronización bidireccional estable sin conflictos.
* Gobierno total de la vida personal y profesional desde una fuente centralizada.
* Escalabilidad inmediata para nuevos tipos de ciclos (ej. planificación de viajes, trabajo en bloques rígidos, etc.).

---

### 📊 Expansión Futura (Roadmap)

* **Scoreboard Avanzado:** Evaluación diaria de enfoque calculando el % de tareas clave completadas.
* **Inteligencia de Backlog:** Sugerencias automáticas de mejora y replanteamiento según las tareas rezagadas (no ejecutadas).
* **Analítica de Vida:** Visualización semanal de la distribución del tiempo por áreas de responsabilidad (Ocio, Salud, Trabajo, etc.).
* **Contextualización IA:** Clasificación automática de nuevas tareas según su contexto o proyecto inferido.