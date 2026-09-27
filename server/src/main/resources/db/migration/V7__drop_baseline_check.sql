-- schema_baseline_check was only ever a smoke-test table from V1 to prove
-- Flyway wiring worked at project start. No entity or repository has ever
-- referenced it — dead weight.

drop table schema_baseline_check;
