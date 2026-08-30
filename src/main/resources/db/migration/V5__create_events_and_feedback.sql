alter table place
    add constraint uk_place_mall_id_id unique (mall_id, id);

create table event (
    id uuid not null,
    mall_id uuid not null,
    place_id uuid not null,
    code varchar(64) not null,
    starts_at timestamp with time zone not null,
    ends_at timestamp with time zone not null,
    publication_state varchar(16) not null,
    publication_starts_at timestamp with time zone not null,
    publication_ends_at timestamp with time zone,
    display_order integer not null default 0,
    constraint pk_event primary key (id),
    constraint fk_event_mall foreign key (mall_id) references mall (id),
    constraint fk_event_mall_place
        foreign key (mall_id, place_id) references place (mall_id, id),
    constraint uk_event_mall_code unique (mall_id, code),
    constraint ck_event_code check (code ~ '^[a-z0-9][a-z0-9-]{0,63}$'),
    constraint ck_event_time_window check (ends_at > starts_at),
    constraint ck_event_publication_state check (publication_state in ('DRAFT', 'PUBLISHED')),
    constraint ck_event_publication_window check (
        publication_ends_at is null or publication_ends_at > publication_starts_at
    )
);

create index idx_event_today
    on event (mall_id, publication_state, starts_at, ends_at, display_order);
create index idx_event_publication_window
    on event (publication_starts_at, publication_ends_at);
create index idx_event_place_id on event (place_id);

create table event_translation (
    id uuid not null,
    event_id uuid not null,
    language varchar(8) not null,
    title varchar(255) not null,
    description varchar(2000),
    constraint pk_event_translation primary key (id),
    constraint fk_event_translation_event
        foreign key (event_id) references event (id) on delete cascade,
    constraint uk_event_translation_event_language unique (event_id, language),
    constraint ck_event_translation_language check (language ~ '^[a-z]{2,3}$')
);

create index idx_event_translation_event_id on event_translation (event_id);

create table feedback (
    id uuid not null,
    mall_id uuid not null,
    kiosk_id uuid not null,
    place_id uuid not null,
    route_id uuid,
    share_id uuid,
    rating integer not null,
    created_at timestamp with time zone not null,
    constraint pk_feedback primary key (id),
    constraint fk_feedback_mall foreign key (mall_id) references mall (id),
    constraint fk_feedback_kiosk foreign key (kiosk_id) references kiosk (id),
    constraint fk_feedback_place foreign key (place_id) references place (id),
    constraint fk_feedback_share foreign key (share_id) references shared_route (id),
    constraint ck_feedback_context check ((route_id is null) <> (share_id is null)),
    constraint ck_feedback_rating check (rating between 1 and 5)
);

create index idx_feedback_mall_created_at on feedback (mall_id, created_at);
create index idx_feedback_kiosk_id on feedback (kiosk_id);
create index idx_feedback_place_id on feedback (place_id);
create index idx_feedback_route_id on feedback (route_id) where route_id is not null;
create index idx_feedback_share_id on feedback (share_id) where share_id is not null;
