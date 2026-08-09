create table navigation_graph_version (
    id uuid not null,
    mall_id uuid not null,
    version_code varchar(64) not null,
    state varchar(16) not null,
    created_at timestamp with time zone not null,
    validated_at timestamp with time zone,
    constraint pk_navigation_graph_version primary key (id),
    constraint fk_navigation_graph_version_mall foreign key (mall_id) references mall (id),
    constraint uk_navigation_graph_version_mall_code unique (mall_id, version_code),
    constraint uk_navigation_graph_version_mall_id unique (mall_id, id),
    constraint ck_navigation_graph_version_state check (state in ('DRAFT', 'VALIDATED')),
    constraint ck_navigation_graph_version_validated_at check (
        (state = 'DRAFT' and validated_at is null)
        or (state = 'VALIDATED' and validated_at is not null)
    )
);

create index idx_navigation_graph_version_mall_state
    on navigation_graph_version (mall_id, state, created_at);

create table navigation_node (
    id uuid not null,
    graph_version_id uuid not null,
    floor_id uuid not null,
    external_code varchar(64) not null,
    x integer not null,
    y integer not null,
    node_type varchar(32) not null,
    constraint pk_navigation_node primary key (id),
    constraint fk_navigation_node_graph_version
        foreign key (graph_version_id) references navigation_graph_version (id) on delete cascade,
    constraint fk_navigation_node_floor foreign key (floor_id) references floor (id),
    constraint uk_navigation_node_version_code unique (graph_version_id, external_code),
    constraint uk_navigation_node_version_id unique (graph_version_id, id),
    constraint ck_navigation_node_x check (x between 0 and 10000),
    constraint ck_navigation_node_y check (y between 0 and 10000),
    constraint ck_navigation_node_type check (
        node_type in ('WAYPOINT', 'PLACE_ACCESS', 'KIOSK', 'TRANSITION')
    )
);

create index idx_navigation_node_graph_version
    on navigation_node (graph_version_id, external_code);
create index idx_navigation_node_floor on navigation_node (floor_id);

create table navigation_edge (
    id uuid not null,
    graph_version_id uuid not null,
    external_code varchar(64) not null,
    from_node_id uuid not null,
    to_node_id uuid not null,
    length_meters numeric(12, 3) not null,
    movement_type varchar(32) not null,
    closed boolean not null default false,
    wheelchair_accessible boolean not null default true,
    constraint pk_navigation_edge primary key (id),
    constraint fk_navigation_edge_graph_version
        foreign key (graph_version_id) references navigation_graph_version (id) on delete cascade,
    constraint fk_navigation_edge_from_node
        foreign key (graph_version_id, from_node_id)
        references navigation_node (graph_version_id, id),
    constraint fk_navigation_edge_to_node
        foreign key (graph_version_id, to_node_id)
        references navigation_node (graph_version_id, id),
    constraint uk_navigation_edge_version_code unique (graph_version_id, external_code),
    constraint ck_navigation_edge_length check (length_meters > 0),
    constraint ck_navigation_edge_distinct_nodes check (from_node_id <> to_node_id),
    constraint ck_navigation_edge_movement_type check (
        movement_type in ('CORRIDOR', 'STAIRS', 'ESCALATOR', 'ELEVATOR')
    )
);

create index idx_navigation_edge_graph_version
    on navigation_edge (graph_version_id, external_code);
create index idx_navigation_edge_from_node on navigation_edge (from_node_id);
create index idx_navigation_edge_to_node on navigation_edge (to_node_id);

create table navigation_edge_point (
    id uuid not null,
    edge_id uuid not null,
    point_order integer not null,
    x integer not null,
    y integer not null,
    constraint pk_navigation_edge_point primary key (id),
    constraint fk_navigation_edge_point_edge
        foreign key (edge_id) references navigation_edge (id) on delete cascade,
    constraint uk_navigation_edge_point_order unique (edge_id, point_order),
    constraint ck_navigation_edge_point_order check (point_order >= 0),
    constraint ck_navigation_edge_point_x check (x between 0 and 10000),
    constraint ck_navigation_edge_point_y check (y between 0 and 10000)
);

create index idx_navigation_edge_point_edge
    on navigation_edge_point (edge_id, point_order);

create table navigation_graph_publication (
    id uuid not null,
    mall_id uuid not null,
    active_graph_version_id uuid not null,
    published_at timestamp with time zone not null,
    constraint pk_navigation_graph_publication primary key (id),
    constraint fk_navigation_graph_publication_mall foreign key (mall_id) references mall (id),
    constraint fk_navigation_graph_publication_version
        foreign key (mall_id, active_graph_version_id)
        references navigation_graph_version (mall_id, id),
    constraint uk_navigation_graph_publication_mall unique (mall_id),
    constraint uk_navigation_graph_publication_version unique (active_graph_version_id)
);

create index idx_navigation_graph_publication_version
    on navigation_graph_publication (active_graph_version_id);
