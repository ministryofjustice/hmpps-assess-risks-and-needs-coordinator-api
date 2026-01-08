CREATE TYPE coordinator.entity_type AS ENUM ('ASSESSMENT', 'PLAN', 'AAP_PLAN');

CREATE TABLE coordinator.oasys_associations (
    id SERIAL PRIMARY KEY,
    uuid uuid NOT NULL,
    created_at timestamp NOT NULL,
    entity_uuid uuid NOT NULL,
    entity_type coordinator.entity_type NOT NULL,
    oasys_assessment_pk varchar(64) NOT NULL,
    region_prison_code varchar(64),
    deleted boolean DEFAULT false
);
