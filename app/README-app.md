# App & Bootstrap

## 1. Misión
Ensamblar y arrancar todos los módulos funcionales en una unidad ejecutable cohesionada. Provee el entorno de ejecución robusto necesario para el gemelo digital.

## 2. Fundamentos Teóricos
- **12-Factor App Methodology:** Configuración externalizada, procesos sin estado y paridad entre entornos mediante variables de sistema.
- **Inversión de Control (IoC):** Centraliza la instanciación de los casos de uso (`DomainConfig`) y su cableado con los adaptadores de persistencia y web.
- **Persistencia Directa:** El sistema está diseñado para uso personal (monousuario), simplificando el modelo de datos y las consultas al no requerir aislamiento multi-tenant.
- **PostgreSQL como SSOT:** Única fuente de verdad transaccional que garantiza ACID, algo que las herramientas externas como Notion no pueden proveer.

## 3. Grafo de Navegación
* [[../README.md|Arquitectura Global (Root)]]
