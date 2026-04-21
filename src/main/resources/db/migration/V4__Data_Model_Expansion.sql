-- Expansión del Modelo de Datos para Sincronización y Finanzas
CREATE SCHEMA IF NOT EXISTS finance;

-- 1. Añadir Jerarquía al CoreExecutable
ALTER TABLE core.core_executable 
ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES core.core_executable(id),
ADD COLUMN IF NOT EXISTS cycle_id UUID, -- Referencia futura a core_cycle
ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'TASK';

-- 2. Crear Tabla de Ciclos (Metas 4DX)
CREATE TABLE core.core_cycle (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
);

-- 3. Crear Tabla de Transacciones Financieras (Micro-Gasto)
CREATE TABLE finance.fin_transaction (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    executable_id UUID REFERENCES core.core_executable(id),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    description VARCHAR(500),
    type VARCHAR(50) NOT NULL, -- INCOME, EXPENSE
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Habilitar RLS en las nuevas estructuras
ALTER TABLE core.core_cycle ENABLE ROW LEVEL SECURITY;
ALTER TABLE finance.fin_transaction ENABLE ROW LEVEL SECURITY;

-- 5. Crear Políticas de Aislamiento por Tenant
CREATE POLICY core_cycle_isolation ON core.core_cycle
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY fin_transaction_isolation ON finance.fin_transaction
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- 6. Actualizar las relaciones de FK para los Ciclos (ahora que existe la tabla)
ALTER TABLE core.core_executable 
ADD CONSTRAINT fk_core_executable_cycle FOREIGN KEY (cycle_id) REFERENCES core.core_cycle(id);
