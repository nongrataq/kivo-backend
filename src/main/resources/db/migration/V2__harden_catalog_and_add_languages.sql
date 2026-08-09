create table mall_language (
    id uuid not null,
    mall_id uuid not null,
    language varchar(8) not null,
    display_label varchar(64) not null,
    display_order integer not null,
    active boolean not null default true,
    constraint pk_mall_language primary key (id),
    constraint fk_mall_language_mall foreign key (mall_id) references mall (id) on delete cascade,
    constraint uk_mall_language_mall_language unique (mall_id, language),
    constraint ck_mall_language_language check (language ~ '^[a-z]{2,3}$')
);

create index idx_mall_language_mall_active
    on mall_language (mall_id, active, display_order);

alter table floor
    add constraint uk_floor_mall_id unique (mall_id, id);

alter table category
    add constraint uk_category_mall_id unique (mall_id, id);

alter table place
    drop constraint fk_place_floor,
    drop constraint fk_place_category,
    add constraint fk_place_mall_floor
        foreign key (mall_id, floor_id) references floor (mall_id, id),
    add constraint fk_place_mall_category
        foreign key (mall_id, category_id) references category (mall_id, id);

alter table kiosk
    drop constraint fk_kiosk_floor,
    drop constraint uk_kiosk_mall_code,
    add constraint fk_kiosk_mall_floor
        foreign key (mall_id, floor_id) references floor (mall_id, id),
    add constraint uk_kiosk_code unique (code);
