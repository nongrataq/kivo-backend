create table shared_route (
    id uuid not null,
    route_id uuid not null,
    mall_id uuid not null,
    graph_version_id uuid not null,
    token_hash varchar(64) not null,
    snapshot_json text not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    constraint pk_shared_route primary key (id),
    constraint uk_shared_route_token_hash unique (token_hash),
    constraint ck_shared_route_token_hash check (token_hash ~ '^[0-9a-f]{64}$'),
    constraint ck_shared_route_expiry check (expires_at > created_at),
    constraint ck_shared_route_revocation check (revoked_at is null or revoked_at >= created_at)
);

create index idx_shared_route_route_id on shared_route (route_id);
create index idx_shared_route_expires_at on shared_route (expires_at);
