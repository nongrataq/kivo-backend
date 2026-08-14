-- Repeatable demo data may evolve independently from the production schema.
-- Remove only the deterministic demo mall before recreating its complete dataset.
delete from shared_route
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from navigation_graph_publication
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from navigation_graph_version
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from kiosk
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from place
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from category
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from floor
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from mall_language
where mall_id = '00000000-0000-0000-0000-000000000001';

delete from mall
where id = '00000000-0000-0000-0000-000000000001';

insert into mall (id, code, name, default_language, timezone, active, created_at)
values ('00000000-0000-0000-0000-000000000001',
        'demo-mall',
        'Демо ТЦ',
        'ru',
        'Europe/Moscow',
        true,
        current_timestamp);

insert into mall_language (id, mall_id, language, display_label, display_order, active)
values ('00000000-0000-0000-0000-000000000011',
        '00000000-0000-0000-0000-000000000001', 'ru', 'RU', 10, true),
       ('00000000-0000-0000-0000-000000000012',
        '00000000-0000-0000-0000-000000000001', 'en', 'EN', 20, true);

insert into floor
    (id, mall_id, code, name, level_number, display_order, map_url,
     coordinate_width, coordinate_height, meters_per_unit, active)
values ('00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000001',
        'L1', 'Первый этаж', 1, 10, '/demo/maps/L1.svg', 10000, 10000, 0.1, true);

insert into category (id, mall_id, code, display_order, active)
values ('00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000000001', 'food', 10, true),
       ('00000000-0000-0000-0000-000000000102',
        '00000000-0000-0000-0000-000000000001', 'fashion', 20, true);

insert into category_translation (id, category_id, language, name)
values ('00000000-0000-0000-0000-000000000201',
        '00000000-0000-0000-0000-000000000101', 'ru', 'Гастрономия'),
       ('00000000-0000-0000-0000-000000000202',
        '00000000-0000-0000-0000-000000000101', 'en', 'Food'),
       ('00000000-0000-0000-0000-000000000203',
        '00000000-0000-0000-0000-000000000102', 'ru', 'Мода'),
       ('00000000-0000-0000-0000-000000000204',
        '00000000-0000-0000-0000-000000000102', 'en', 'Fashion');

insert into kiosk (id, mall_id, floor_id, code, start_navigation_node_code, enabled)
values ('00000000-0000-0000-0000-000000000601',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        'demo-kiosk', 'node-kiosk', true);

insert into place
    (id, mall_id, floor_id, category_id, code, navigation_node_code, display_order, active)
values ('00000000-0000-0000-0000-000000000401',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000101',
        'grocery', 'node-grocery', 10, true),
       ('00000000-0000-0000-0000-000000000402',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000102',
        'boutique', 'node-boutique', 20, true);

insert into place_translation (id, place_id, language, name, description)
values ('00000000-0000-0000-0000-000000000501',
        '00000000-0000-0000-0000-000000000401',
        'ru', 'Продуктовый магазин', 'Свежие продукты'),
       ('00000000-0000-0000-0000-000000000502',
        '00000000-0000-0000-0000-000000000401',
        'en', 'Grocery store', 'Fresh groceries'),
       ('00000000-0000-0000-0000-000000000503',
        '00000000-0000-0000-0000-000000000402',
        'ru', 'Бутик', 'Одежда и аксессуары'),
       ('00000000-0000-0000-0000-000000000504',
        '00000000-0000-0000-0000-000000000402',
        'en', 'Boutique', 'Clothing and accessories');

insert into navigation_graph_version
    (id, mall_id, version_code, state, created_at, validated_at)
values ('30000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'demo-v1', 'VALIDATED', current_timestamp, current_timestamp);

insert into navigation_node
    (id, graph_version_id, floor_id, external_code, x, y, node_type)
values ('30000000-0000-0000-0000-000000000101',
        '30000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        'node-kiosk', 1200, 5000, 'KIOSK'),
       ('30000000-0000-0000-0000-000000000102',
        '30000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        'node-junction', 5000, 5000, 'WAYPOINT'),
       ('30000000-0000-0000-0000-000000000103',
        '30000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        'node-grocery', 5000, 2500, 'PLACE_ACCESS'),
       ('30000000-0000-0000-0000-000000000104',
        '30000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        'node-boutique', 8200, 5000, 'PLACE_ACCESS');

insert into navigation_edge
    (id, graph_version_id, external_code, from_node_id, to_node_id,
     length_meters, movement_type, closed, wheelchair_accessible)
values ('30000000-0000-0000-0000-000000000201',
        '30000000-0000-0000-0000-000000000001',
        'edge-kiosk-junction',
        '30000000-0000-0000-0000-000000000101',
        '30000000-0000-0000-0000-000000000102',
        38.000, 'CORRIDOR', false, true),
       ('30000000-0000-0000-0000-000000000202',
        '30000000-0000-0000-0000-000000000001',
        'edge-junction-grocery',
        '30000000-0000-0000-0000-000000000102',
        '30000000-0000-0000-0000-000000000103',
        25.000, 'CORRIDOR', false, true),
       ('30000000-0000-0000-0000-000000000203',
        '30000000-0000-0000-0000-000000000001',
        'edge-junction-boutique',
        '30000000-0000-0000-0000-000000000102',
        '30000000-0000-0000-0000-000000000104',
        32.000, 'CORRIDOR', false, true);

insert into navigation_edge_point (id, edge_id, point_order, x, y)
values ('30000000-0000-0000-0000-000000000301',
        '30000000-0000-0000-0000-000000000201', 0, 1200, 5000),
       ('30000000-0000-0000-0000-000000000302',
        '30000000-0000-0000-0000-000000000201', 1, 5000, 5000),
       ('30000000-0000-0000-0000-000000000303',
        '30000000-0000-0000-0000-000000000202', 0, 5000, 5000),
       ('30000000-0000-0000-0000-000000000304',
        '30000000-0000-0000-0000-000000000202', 1, 5000, 2500),
       ('30000000-0000-0000-0000-000000000305',
        '30000000-0000-0000-0000-000000000203', 0, 5000, 5000),
       ('30000000-0000-0000-0000-000000000306',
        '30000000-0000-0000-0000-000000000203', 1, 8200, 5000);

insert into navigation_graph_publication
    (id, mall_id, active_graph_version_id, published_at)
values ('30000000-0000-0000-0000-000000000401',
        '00000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        current_timestamp);
