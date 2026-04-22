package com.hyperbrain.sopfc.sync.infrastructure.adapter.out.notion.dto;

public final class NotionConstants {
    private NotionConstants() {}

    // Property Names
    public static final String PROP_NAME = "Name";
    public static final String PROP_STATUS = "Status";
    public static final String PROP_TYPE = "Type";
    public static final String PROP_DESCRIPTION = "Description";
    public static final String PROP_COMPLETE = "Complete";
    public static final String PROP_DATE = "Date";
    public static final String PROP_ENERGY = "Energy";
    public static final String PROP_IMPACT = "Impact";
    public static final String PROP_MENTAL_LOAD = "Mental Load";
    public static final String PROP_URL = "URL";
    public static final String PROP_EST_MIN = "Est. Min";
    
    // Select/Status Names
    public static final String STATUS_NOT_STARTED = "Not started";
    public static final String STATUS_IN_PROGRESS = "In progress";
    public static final String STATUS_DONE = "Done";
    public static final String STATUS_FAILED = "Failed";

    public static final String TYPE_TASK = "Task";
    public static final String TYPE_HABIT = "Habit";
    public static final String TYPE_ACTIVITY = "Activity";

    public static final String IMPACT_CRITICAL = "Crítico";
    public static final String IMPACT_HIGH = "Alto";
    public static final String IMPACT_MODERATE = "Moderado";
    public static final String IMPACT_LOW = "Bajo";
    public static final String IMPACT_IRRELEVANT = "Irrelevante";

    public static final String ENERGY_INTENSE = "Intenso";
    public static final String ENERGY_DEMANDING = "Exigente";
    public static final String ENERGY_SUSTAINED = "Sostenido";
    public static final String ENERGY_EXECUTION = "Ejecución";
    public static final String ENERGY_AUTOMATIC = "Automático";

    public static final String MENTAL_ABSTRACT = "Abstracto";
    public static final String MENTAL_COMPLEX = "Complejo";
    public static final String MENTAL_ANALYSIS = "Análisis";
    public static final String MENTAL_FOCUS = "Foco";
    public static final String MENTAL_ROUTINE = "Rutinario";
}
