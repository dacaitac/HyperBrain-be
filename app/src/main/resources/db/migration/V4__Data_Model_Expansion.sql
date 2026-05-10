-- Expansión del Modelo de Datos para Sincronización y Finanzas

-- 1. Añadir Jerarquía al CoreExecutable
ALTER TABLE core.core_executable 
ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES core.core_executable(id),
ADD COLUMN IF NOT EXISTS cycle_id UUID, -- Referencia futura a core_cycle
ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'TASK';

-- 2. Crear Tabla de Ciclos (Metas 4DX)
CREATE TABLE IF NOT EXISTS core.core_cycle (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
);

-- 3. Crear Tabla de Transacciones Financieras (Micro-Gasto)
CREATE TABLE IF NOT EXISTS finance.fin_transaction (
    id UUID PRIMARY KEY,
    executable_id UUID REFERENCES core.core_executable(id),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    description VARCHAR(500),
    type VARCHAR(50) NOT NULL, -- INCOME, EXPENSE
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Actualizar las relaciones de FK para los Ciclos (ahora que existe la tabla)
ALTER TABLE core.core_executable 
ADD CONSTRAINT fk_core_executable_cycle FOREIGN KEY (cycle_id) REFERENCES core.core_cycle(id);
