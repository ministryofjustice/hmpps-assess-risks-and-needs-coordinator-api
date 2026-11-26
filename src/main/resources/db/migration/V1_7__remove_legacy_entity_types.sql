DELETE FROM coordinator.oasys_associations WHERE entity_type IN ('ASSESSMENT', 'PLAN');

-- Postgres can't drop enum values, so the type is recreated. This goes via VARCHAR rather than
-- ALTER TYPE ... RENAME so that the script also runs on H2 (used by the integration tests).
ALTER TABLE coordinator.oasys_associations ALTER COLUMN entity_type TYPE VARCHAR(16);

DROP TYPE coordinator.entity_type;

CREATE TYPE coordinator.entity_type AS ENUM ('AAP_SAN', 'AAP_PLAN');

ALTER TABLE coordinator.oasys_associations ALTER COLUMN entity_type TYPE coordinator.entity_type USING entity_type::coordinator.entity_type;
