DELETE FROM coordinator.oasys_associations WHERE entity_type IN ('ASSESSMENT', 'PLAN');

ALTER TYPE coordinator.entity_type RENAME TO entity_type_old;

CREATE TYPE coordinator.entity_type AS ENUM ('AAP_SAN', 'AAP_PLAN');

ALTER TABLE coordinator.oasys_associations ALTER COLUMN entity_type TYPE coordinator.entity_type USING entity_type::text::coordinator.entity_type;

DROP TYPE coordinator.entity_type_old;
