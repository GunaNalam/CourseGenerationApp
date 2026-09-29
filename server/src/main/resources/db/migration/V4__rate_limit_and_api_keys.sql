create table generation_request_log (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_user (id),
    created_at timestamptz not null default now()
);
create index idx_generation_request_log_user_created on generation_request_log (user_id, created_at);

create table user_api_key (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references app_user (id),
    provider varchar(50) not null,
    encrypted_key text not null,
    created_at timestamptz not null default now()
);
