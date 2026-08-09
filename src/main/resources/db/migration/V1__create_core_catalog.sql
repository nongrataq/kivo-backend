create table mall (
    id uuid not null,
    code varchar(64) not null,
    name varchar(255) not null,
    default_language varchar(8) not null,
    timezone varchar(64) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    constraint pk_mall primary key (id),
    constraint uk_mall_code unique (code)
);

create table floor (
    id uuid not null,
    mall_id uuid not null,
    code varchar(32) not null,
    name varchar(128) not null,
    level_number integer not null,
    display_order integer not null,
    map_url varchar(2048),
    coordinate_width integer not null default 10000,
    coordinate_height integer not null default 10000,
    meters_per_unit numeric(12, 8),
    active boolean not null default true,
    constraint pk_floor primary key (id),
    constraint fk_floor_mall foreign key (mall_id) references mall (id),
    constraint uk_floor_mall_code unique (mall_id, code),
    constraint ck_floor_coordinate_width check (coordinate_width > 0),
    constraint ck_floor_coordinate_height check (coordinate_height > 0),
    constraint ck_floor_meters_per_unit check (meters_per_unit is null or meters_per_unit > 0)
);

create index idx_floor_mall_id on floor (mall_id);

create table category (
    id uuid not null,
    mall_id uuid not null,
    code varchar(64) not null,
    display_order integer not null,
    active boolean not null default true,
    constraint pk_category primary key (id),
    constraint fk_category_mall foreign key (mall_id) references mall (id),
    constraint uk_category_mall_code unique (mall_id, code)
);

create index idx_category_mall_active on category (mall_id, active, display_order);

create table category_translation (
    id uuid not null,
    category_id uuid not null,
    language varchar(8) not null,
    name varchar(255) not null,
    constraint pk_category_translation primary key (id),
    constraint fk_category_translation_category foreign key (category_id) references category (id) on delete cascade,
    constraint uk_category_translation_language unique (category_id, language)
);

create index idx_category_translation_category_id on category_translation (category_id);

create table place (
    id uuid not null,
    mall_id uuid not null,
    floor_id uuid not null,
    category_id uuid not null,
    code varchar(64) not null,
    navigation_node_code varchar(64),
    display_order integer not null,
    active boolean not null default true,
    constraint pk_place primary key (id),
    constraint fk_place_mall foreign key (mall_id) references mall (id),
    constraint fk_place_floor foreign key (floor_id) references floor (id),
    constraint fk_place_category foreign key (category_id) references category (id),
    constraint uk_place_mall_code unique (mall_id, code)
);

create index idx_place_mall_active on place (mall_id, active, display_order);
create index idx_place_floor_id on place (floor_id);
create index idx_place_category_id on place (category_id);

create table place_translation (
    id uuid not null,
    place_id uuid not null,
    language varchar(8) not null,
    name varchar(255) not null,
    description varchar(2000),
    constraint pk_place_translation primary key (id),
    constraint fk_place_translation_place foreign key (place_id) references place (id) on delete cascade,
    constraint uk_place_translation_language unique (place_id, language)
);

create index idx_place_translation_place_id on place_translation (place_id);

create table kiosk (
    id uuid not null,
    mall_id uuid not null,
    floor_id uuid not null,
    code varchar(64) not null,
    start_navigation_node_code varchar(64),
    enabled boolean not null default true,
    constraint pk_kiosk primary key (id),
    constraint fk_kiosk_mall foreign key (mall_id) references mall (id),
    constraint fk_kiosk_floor foreign key (floor_id) references floor (id),
    constraint uk_kiosk_mall_code unique (mall_id, code)
);

create index idx_kiosk_mall_enabled on kiosk (mall_id, enabled);
