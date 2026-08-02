-- Approval Workflow Phase 2: allow Vendor Bills in the generic approval table.
-- The live MySQL schema is native ENUM-backed under Hibernate's current mapping.
ALTER TABLE approval_requests
    MODIFY COLUMN entity_type ENUM('MANUAL_JOURNAL', 'VENDOR_BILL') NOT NULL;
