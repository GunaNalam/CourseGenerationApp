create table pipeline_run (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references app_user (id),
    course_id uuid references course (id),
    status varchar(20) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_pipeline_run_owner_id on pipeline_run (owner_id);

create table job_step (
    id uuid primary key default gen_random_uuid(),
    pipeline_run_id uuid not null references pipeline_run (id) on delete cascade,
    type varchar(30) not null,
    status varchar(20) not null,
    depends_on_step_id uuid references job_step (id),
    input jsonb,
    output jsonb,
    attempt integer not null default 0,
    error text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_job_step_pipeline_run_id on job_step (pipeline_run_id);
create index idx_job_step_status_created_at on job_step (status, created_at);
