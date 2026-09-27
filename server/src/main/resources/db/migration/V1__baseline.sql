create table if not exists schema_baseline_check (
    id serial primary key,
    created_at timestamptz not null default now()
);
