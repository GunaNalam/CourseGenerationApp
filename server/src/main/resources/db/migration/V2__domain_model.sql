create table app_user (
    id uuid primary key default gen_random_uuid(),
    auth0_sub varchar(255) unique,
    email varchar(255) unique,
    created_at timestamptz not null default now()
);

create table course (
    id uuid primary key default gen_random_uuid(),
    title varchar(255) not null,
    description text,
    owner_id uuid not null references app_user (id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_course_owner_id on course (owner_id);

create table course_tag (
    course_id uuid not null references course (id) on delete cascade,
    tag varchar(100) not null
);

create table module (
    id uuid primary key default gen_random_uuid(),
    title varchar(255) not null,
    order_index integer not null,
    course_id uuid not null references course (id) on delete cascade,
    created_at timestamptz not null default now()
);
create index idx_module_course_id on module (course_id);

create table lesson (
    id uuid primary key default gen_random_uuid(),
    title varchar(255) not null,
    order_index integer not null,
    content jsonb not null default '[]'::jsonb,
    is_enriched boolean not null default false,
    module_id uuid not null references module (id) on delete cascade,
    created_at timestamptz not null default now()
);
create index idx_lesson_module_id on lesson (module_id);

create table lesson_objective (
    lesson_id uuid not null references lesson (id) on delete cascade,
    objective text not null
);
