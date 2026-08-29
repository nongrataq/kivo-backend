-- Repeatable demo data derived from the TSUM L1 floor plan dated 2026-07-16.
-- The topology, metric scale, place anchors, and accessibility remain demo-only
-- approximations pending an on-site survey and facility-owner confirmation.
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
        'tsum-demo',
        'ЦУМ — демонстрационный L1',
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
        'L1', 'Первый этаж', 1, 10, '/demo/maps/tsum-l1.png', 10000, 8358, 0.01, true);

insert into category (id, mall_id, code, display_order, active)
values ('00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000000001', 'exhibition', 10, true),
       ('00000000-0000-0000-0000-000000000102',
        '00000000-0000-0000-0000-000000000001', 'gastronomy', 20, true),
       ('00000000-0000-0000-0000-000000000103',
        '00000000-0000-0000-0000-000000000001', 'workshop', 30, true),
       ('00000000-0000-0000-0000-000000000104',
        '00000000-0000-0000-0000-000000000001', 'visitor-service', 40, true);

insert into category_translation (id, category_id, language, name)
values ('00000000-0000-0000-0000-000000000201',
        '00000000-0000-0000-0000-000000000101', 'ru', 'Выставки'),
       ('00000000-0000-0000-0000-000000000202',
        '00000000-0000-0000-0000-000000000101', 'en', 'Exhibitions'),
       ('00000000-0000-0000-0000-000000000203',
        '00000000-0000-0000-0000-000000000102', 'ru', 'Гастрономия'),
       ('00000000-0000-0000-0000-000000000204',
        '00000000-0000-0000-0000-000000000102', 'en', 'Food'),
       ('00000000-0000-0000-0000-000000000205',
        '00000000-0000-0000-0000-000000000103', 'ru', 'Мастерские'),
       ('00000000-0000-0000-0000-000000000206',
        '00000000-0000-0000-0000-000000000103', 'en', 'Workshops'),
       ('00000000-0000-0000-0000-000000000207',
        '00000000-0000-0000-0000-000000000104', 'ru', 'Сервисы для посетителей'),
       ('00000000-0000-0000-0000-000000000208',
        '00000000-0000-0000-0000-000000000104', 'en', 'Visitor services');

insert into kiosk (id, mall_id, floor_id, code, start_navigation_node_code, enabled)
values ('00000000-0000-0000-0000-000000000601',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        'tsum-l1-main-kiosk', 'n-kiosk-main', true);

insert into place
    (id, mall_id, floor_id, category_id, code, navigation_node_code, display_order, active)
values ('00000000-0000-0000-0000-000000000401',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000101', 'gallery', 'p-gallery', 10, true),
       ('00000000-0000-0000-0000-000000000402',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000104', 'cloakroom', 'p-cloakroom', 20, true),
       ('00000000-0000-0000-0000-000000000403',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000101', 'exhibition-one', 'p-exhibition-one', 30, true),
       ('00000000-0000-0000-0000-000000000404',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000101', 'exhibition-two', 'p-exhibition-two', 40, true),
       ('00000000-0000-0000-0000-000000000405',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000102', 'restaurant-west', 'p-restaurant-west', 50, true),
       ('00000000-0000-0000-0000-000000000406',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000102', 'restaurant-center', 'p-restaurant-center', 60, true),
       ('00000000-0000-0000-0000-000000000407',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000102', 'restaurant-east', 'p-restaurant-east', 70, true),
       ('00000000-0000-0000-0000-000000000408',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000104', 'excursion-center', 'p-excursion', 80, true),
       ('00000000-0000-0000-0000-000000000409',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'culinary-workshops', 'p-culinary', 90, true),
       ('00000000-0000-0000-0000-000000000410',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'aybat', 'p-aybat', 100, true),
       ('00000000-0000-0000-0000-000000000411',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'bikmatur', 'p-bikmatur', 110, true),
       ('00000000-0000-0000-0000-000000000412',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'tatardar', 'p-tatardar', 120, true),
       ('00000000-0000-0000-0000-000000000413',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'kazan-ceramics', 'p-kazan-ceramics', 130, true),
       ('00000000-0000-0000-0000-000000000414',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'artel-masters', 'p-artel', 140, true),
       ('00000000-0000-0000-0000-000000000415',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'tugannar', 'p-tugannar', 150, true),
       ('00000000-0000-0000-0000-000000000416',
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000103', 'kazan-silver', 'p-kazan-silver', 160, true);

insert into place_translation (id, place_id, language, name, description)
select md5('tsum-l1-place-translation:' || place_id::text || ':' || language)::uuid,
       place_id,
       language,
       name,
       description
from (values
    ('00000000-0000-0000-0000-000000000401'::uuid, 'ru', 'Галерея', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000401'::uuid, 'en', 'Gallery', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000402'::uuid, 'ru', 'Гардероб', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000402'::uuid, 'en', 'Cloakroom', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000403'::uuid, 'ru', 'Выставочное пространство №1', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000403'::uuid, 'en', 'Exhibition space 1', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000404'::uuid, 'ru', 'Выставочное пространство №2', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000404'::uuid, 'en', 'Exhibition space 2', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000405'::uuid, 'ru', 'Западная ресторанная зона', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000405'::uuid, 'en', 'West restaurant zone', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000406'::uuid, 'ru', 'Центральная ресторанная зона', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000406'::uuid, 'en', 'Central restaurant zone', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000407'::uuid, 'ru', 'Восточная ресторанная зона', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000407'::uuid, 'en', 'East restaurant zone', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000408'::uuid, 'ru', 'Экскурсионный центр Казани', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000408'::uuid, 'en', 'Kazan excursion center', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000409'::uuid, 'ru', 'Кулинарные мастер-классы', 'Демо-якорь по плану; положение требует проверки на месте.'),
    ('00000000-0000-0000-0000-000000000409'::uuid, 'en', 'Culinary workshops', 'Demo anchor derived from the plan; verify on site.'),
    ('00000000-0000-0000-0000-000000000410'::uuid, 'ru', 'Әйбәт', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000410'::uuid, 'en', 'Aybat', 'Demo workshop anchor inferred from a plan callout; verify on site.'),
    ('00000000-0000-0000-0000-000000000411'::uuid, 'ru', 'Бикматур', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000411'::uuid, 'en', 'Bikmatur', 'Demo workshop anchor inferred from a plan callout; verify on site.'),
    ('00000000-0000-0000-0000-000000000412'::uuid, 'ru', 'Татардар', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000412'::uuid, 'en', 'Tatardar', 'Demo workshop anchor inferred from a plan callout; verify on site.'),
    ('00000000-0000-0000-0000-000000000413'::uuid, 'ru', 'Казанская керамика', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000413'::uuid, 'en', 'Kazan ceramics', 'Demo workshop anchor inferred from a plan callout; verify on site.'),
    ('00000000-0000-0000-0000-000000000414'::uuid, 'ru', 'Артель мастеров', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000414'::uuid, 'en', 'Artel Masters', 'Demo workshop anchor inferred from a plan callout; verify on site.'),
    ('00000000-0000-0000-0000-000000000415'::uuid, 'ru', 'Туганнар', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000415'::uuid, 'en', 'Tugannar', 'Demo workshop anchor inferred from a plan callout; verify on site.'),
    ('00000000-0000-0000-0000-000000000416'::uuid, 'ru', 'Казанское серебро', 'Демо-якорь мастерской по выноске плана; требуется проверка на месте.'),
    ('00000000-0000-0000-0000-000000000416'::uuid, 'en', 'Kazan silver', 'Demo workshop anchor inferred from a plan callout; verify on site.')
) as translations(place_id, language, name, description);

insert into navigation_graph_version
    (id, mall_id, version_code, state, created_at, validated_at)
values ('30000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'tsum-l1-demo-2026-07-16-v1', 'VALIDATED', current_timestamp, current_timestamp);

insert into navigation_node
    (id, graph_version_id, floor_id, external_code, x, y, node_type)
select md5('tsum-l1-node:' || external_code)::uuid,
       '30000000-0000-0000-0000-000000000001'::uuid,
       '00000000-0000-0000-0000-000000000301'::uuid,
       external_code,
       x,
       y,
       node_type
from (values
    ('n-kiosk-main', 7500, 7600, 'KIOSK'),
    ('n-entry-main', 7500, 7200, 'WAYPOINT'),
    ('n-atrium-south', 7500, 7100, 'WAYPOINT'),
    ('n-atrium-west', 6600, 6900, 'WAYPOINT'),
    ('n-atrium-east', 8500, 6900, 'WAYPOINT'),
    ('n-atrium-northwest', 6600, 5500, 'WAYPOINT'),
    ('n-atrium-northeast', 8500, 5500, 'WAYPOINT'),
    ('n-cross-center', 7500, 5200, 'WAYPOINT'),
    ('n-cross-west', 6100, 5200, 'WAYPOINT'),
    ('n-cross-east', 8400, 5000, 'WAYPOINT'),
    ('n-gallery-east', 5300, 5200, 'WAYPOINT'),
    ('n-gallery-center', 4200, 5200, 'WAYPOINT'),
    ('n-gallery-north', 4200, 4200, 'WAYPOINT'),
    ('n-gallery-south', 4200, 6500, 'WAYPOINT'),
    ('n-food-west', 6200, 4200, 'WAYPOINT'),
    ('n-food-center', 7500, 4200, 'WAYPOINT'),
    ('n-food-east', 8700, 4200, 'WAYPOINT'),
    ('n-north-link-south', 6100, 3500, 'WAYPOINT'),
    ('n-north-link-middle', 6100, 2300, 'WAYPOINT'),
    ('n-north-link-top', 6100, 1500, 'WAYPOINT'),
    ('n-north-west', 3200, 1500, 'WAYPOINT'),
    ('n-north-center', 4700, 1500, 'WAYPOINT'),
    ('n-north-east', 7600, 1500, 'WAYPOINT'),
    ('n-east-north', 8500, 1500, 'WAYPOINT'),
    ('n-east-upper', 8500, 2300, 'WAYPOINT'),
    ('n-east-middle', 8500, 3300, 'WAYPOINT'),
    ('n-east-lower', 8500, 4200, 'WAYPOINT'),
    ('n-east-south', 8500, 6800, 'WAYPOINT'),
    ('p-gallery', 4200, 5000, 'PLACE_ACCESS'),
    ('p-cloakroom', 2500, 4200, 'PLACE_ACCESS'),
    ('p-exhibition-one', 5900, 3600, 'PLACE_ACCESS'),
    ('p-exhibition-two', 5900, 2700, 'PLACE_ACCESS'),
    ('p-restaurant-west', 6500, 3900, 'PLACE_ACCESS'),
    ('p-restaurant-center', 7500, 3900, 'PLACE_ACCESS'),
    ('p-restaurant-east', 8500, 3900, 'PLACE_ACCESS'),
    ('p-excursion', 7500, 4700, 'PLACE_ACCESS'),
    ('p-culinary', 7500, 1900, 'PLACE_ACCESS'),
    ('p-aybat', 3600, 1200, 'PLACE_ACCESS'),
    ('p-bikmatur', 5100, 1200, 'PLACE_ACCESS'),
    ('p-tatardar', 7000, 1200, 'PLACE_ACCESS'),
    ('p-kazan-ceramics', 9000, 1900, 'PLACE_ACCESS'),
    ('p-artel', 9000, 2800, 'PLACE_ACCESS'),
    ('p-tugannar', 9000, 3700, 'PLACE_ACCESS'),
    ('p-kazan-silver', 5000, 7000, 'PLACE_ACCESS')
) as nodes(external_code, x, y, node_type);

with undirected(code, from_code, to_code, wheelchair_accessible) as (values
    ('kiosk-entry', 'n-kiosk-main', 'n-entry-main', true),
    ('entry-atrium', 'n-entry-main', 'n-atrium-south', true),
    ('atrium-south-west', 'n-atrium-south', 'n-atrium-west', true),
    ('atrium-south-east', 'n-atrium-south', 'n-atrium-east', true),
    ('atrium-west-north', 'n-atrium-west', 'n-atrium-northwest', true),
    ('atrium-east-north', 'n-atrium-east', 'n-atrium-northeast', true),
    ('atrium-northwest-center', 'n-atrium-northwest', 'n-cross-center', true),
    ('atrium-northeast-center', 'n-atrium-northeast', 'n-cross-center', true),
    ('cross-center-west', 'n-cross-center', 'n-cross-west', true),
    ('cross-center-east', 'n-cross-center', 'n-cross-east', true),
    ('cross-east-east-lower', 'n-cross-east', 'n-east-lower', true),
    ('cross-west-gallery-east', 'n-cross-west', 'n-gallery-east', true),
    ('gallery-east-center', 'n-gallery-east', 'n-gallery-center', true),
    ('gallery-center-north', 'n-gallery-center', 'n-gallery-north', true),
    ('gallery-center-south', 'n-gallery-center', 'n-gallery-south', true),
    ('gallery-north-food-west', 'n-gallery-north', 'n-food-west', true),
    ('food-west-center', 'n-food-west', 'n-food-center', true),
    ('food-center-east', 'n-food-center', 'n-food-east', true),
    ('food-east-east-lower', 'n-food-east', 'n-east-lower', true),
    ('cross-west-north-link-south', 'n-cross-west', 'n-north-link-south', true),
    ('north-link-south-middle', 'n-north-link-south', 'n-north-link-middle', true),
    ('north-link-middle-top', 'n-north-link-middle', 'n-north-link-top', true),
    ('north-top-west', 'n-north-link-top', 'n-north-west', true),
    ('north-top-center', 'n-north-link-top', 'n-north-center', true),
    ('north-top-east', 'n-north-link-top', 'n-north-east', true),
    ('north-east-corridor', 'n-north-east', 'n-east-north', true),
    ('east-north-upper', 'n-east-north', 'n-east-upper', true),
    ('east-upper-middle', 'n-east-upper', 'n-east-middle', true),
    ('east-middle-lower', 'n-east-middle', 'n-east-lower', true),
    ('east-lower-south', 'n-east-lower', 'n-east-south', true),
    ('east-south-atrium', 'n-east-south', 'n-atrium-east', true),
    ('place-gallery', 'n-gallery-center', 'p-gallery', true),
    ('place-cloakroom', 'n-gallery-north', 'p-cloakroom', true),
    ('place-exhibition-one', 'n-north-link-south', 'p-exhibition-one', true),
    ('place-exhibition-two', 'n-north-link-middle', 'p-exhibition-two', true),
    ('place-restaurant-west', 'n-food-west', 'p-restaurant-west', true),
    ('place-restaurant-center', 'n-food-center', 'p-restaurant-center', true),
    ('place-restaurant-east', 'n-food-east', 'p-restaurant-east', true),
    ('place-excursion', 'n-cross-center', 'p-excursion', true),
    ('place-culinary', 'n-north-east', 'p-culinary', true),
    ('place-aybat', 'n-north-west', 'p-aybat', true),
    ('place-bikmatur', 'n-north-center', 'p-bikmatur', true),
    ('place-tatardar', 'n-north-east', 'p-tatardar', true),
    ('place-kazan-ceramics', 'n-east-upper', 'p-kazan-ceramics', true),
    ('place-artel', 'n-east-middle', 'p-artel', true),
    ('place-tugannar', 'n-east-lower', 'p-tugannar', true),
    ('place-kazan-silver', 'n-gallery-south', 'p-kazan-silver', true)
), directed(code, from_code, to_code, wheelchair_accessible) as (
    select code || '-forward', from_code, to_code, wheelchair_accessible from undirected
    union all
    select code || '-reverse', to_code, from_code, wheelchair_accessible from undirected
)
insert into navigation_edge
    (id, graph_version_id, external_code, from_node_id, to_node_id,
     length_meters, movement_type, closed, wheelchair_accessible)
select md5('tsum-l1-edge:' || directed.code)::uuid,
       '30000000-0000-0000-0000-000000000001'::uuid,
       directed.code,
       from_node.id,
       to_node.id,
       round((sqrt(power(from_node.x - to_node.x, 2)
                        + power(from_node.y - to_node.y, 2)) * 0.01)::numeric, 3),
       'CORRIDOR',
       false,
       directed.wheelchair_accessible
from directed
join navigation_node from_node
  on from_node.graph_version_id = '30000000-0000-0000-0000-000000000001'
 and from_node.external_code = directed.from_code
join navigation_node to_node
  on to_node.graph_version_id = '30000000-0000-0000-0000-000000000001'
 and to_node.external_code = directed.to_code;

insert into navigation_edge_point (id, edge_id, point_order, x, y)
select md5('tsum-l1-edge-point:' || edge.external_code || ':' || point.point_order)::uuid,
       edge.id,
       point.point_order,
       point.x,
       point.y
from navigation_edge edge
join navigation_node from_node on from_node.id = edge.from_node_id
join navigation_node to_node on to_node.id = edge.to_node_id
cross join lateral (values (0, from_node.x, from_node.y),
                           (1, to_node.x, to_node.y)) as point(point_order, x, y)
where edge.graph_version_id = '30000000-0000-0000-0000-000000000001';

insert into navigation_graph_publication
    (id, mall_id, active_graph_version_id, published_at)
values ('30000000-0000-0000-0000-000000000401',
        '00000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        current_timestamp);
