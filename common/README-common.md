# Common Components

## 1. Misión
Actúa como el cimiento técnico del sistema distribuido modularmente. Contiene los contratos globales y las utilidades que permiten la interoperabilidad entre motores sin crear acoplamiento directo.

## 2. Fundamentos Teóricos
- **Event-Driven Architecture (EDA):** Comunicación desacoplada mediante eventos. El sistema no llama a otros módulos directamente; publica que algo ha sucedido y los interesados reaccionan.
- **Transactional Outbox Pattern:** Solución al problema de las transacciones distribuidas. Los eventos se guardan en la misma base de datos que el estado de negocio y un proceso separado los envía al bus, garantizando "at-least-once delivery".
- **Clean Architecture & Hexagonal:** Define los `Port` outbox y excepciones de negocio puras que son compartidas por los dominios.

## 3. Grafo de Navegación
* [[../README.md|Arquitectura Global (Root)]]
