# Sync Engine (ACL & Identity Map)

## 1. Misión
El módulo `sync` aísla el núcleo de SOPFC de la volatilidad de integraciones de terceros (Notion, Apple Reminders, Calendar), permitiendo la comunicación bidireccional y la orquestación de webhooks.

## 2. Fundamentos Teóricos
- **Capa Anticorrupción (ACL):** Protege el dominio interno de cambios en esquemas externos. Todo dato entrante se mapea a través de adaptadores especializados.
- **Hashes Semánticos:** Prevención de "Sync Loops". Si el estado externo no ha cambiado semánticamente (según su hash), el sistema ignora la actualización.
- **Identity Mapping:** Tabla de traducción de identidades que permite al sistema saber que una página de Notion es el mismo objeto que un recordatorio de Apple y un ejecutable en SQL.

## 3. Grafo de Navegación
* [[../README.md|Arquitectura Global (Root)]]
