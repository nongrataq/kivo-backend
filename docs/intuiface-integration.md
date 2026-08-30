# Handoff: интеграция Kivo с Intuiface для Егора

Этот документ описывает kiosk-интеграцию со стабильным Kivo JSON API. Канонический контракт —
[`docs/api/openapi.yaml`](api/openapi.yaml), ограничения демонстрационной карты —
[`docs/demo/tsum-l1-data.md`](demo/tsum-l1-data.md).

## Что подтвердить до начала

1. Версию Intuiface Composer, тип Player, целевую ОС и лицензию.
2. Публичный HTTPS origin backend, например `https://kivo.example`.
3. Точный origin Player/experience для `KIVO_CORS_ALLOWED_ORIGINS`, включая scheme и port.
4. Код установленного киоска. Для demo: `tsum-l1-main-kiosk`.
5. Доступность backend из сети Player без VPN/browser-only исключений.

Kiosk API публичный и не требует login/session. Admin Basic credentials нельзя хранить в experience;
admin publication endpoint в интерфейс киоска не добавляется.

## Особенности Intuiface, которые влияют на контракт

- REST Interface Assets удобнее создать через API Explorer: он принимает URL или cURL, поддерживает
  GET/POST и JSON body, а параметры запроса превращает в свойства/actions.
- Текущая официальная документация описывает `Request completed` для ответа `200`. Kivo возвращает
  `201` для создания/share и `204` для feedback. Это обязательный пункт spike на фактических
  Composer/Player; backend-контракт до результата spike не менять.
- API Explorer не поддерживает array внутри array. Kivo отдаёт только верхнеуровневые коллекции с
  плоскими элементами: `languages`, `floors`, `instructions`, `overlays`, categories, places, events.
- После `Request failed` Intuiface может сохранять последний успешный ответ. На error trigger нужно
  скрывать старую коллекцию/карточку и показывать ошибку.
- URL прозрачного PNG передаётся в Image Asset через action `Change image`. Если URL относительный,
  сначала добавить `baseOrigin` через Concatenate converter.
- Для iOS Player нужен HTTPS; HTTP endpoint использовать только в локальном Composer spike.

Официальные справки: [API Explorer](https://support.intuiface.com/hc/en-us/articles/360007431091-API-Explorer-Create-a-REST-based-Web-Service-Interface-Asset),
[Image Asset](https://support.intuiface.com/hc/en-us/articles/360007429891-Image-Asset),
[bindings](https://support.intuiface.com/hc/en-us/articles/360007430691-Binding-General-Concepts) и
[value converters](https://support.intuiface.com/hc/en-us/articles/360007430631-Value-Converters).

## Переменные experience

| Переменная | Начальное значение | Назначение |
| --- | --- | --- |
| `baseOrigin` | staging HTTPS origin | Без `/api/v1` и завершающего `/` |
| `apiBase` | `{baseOrigin}/api/v1` | Основа JSON-запросов |
| `kioskCode` | `tsum-l1-main-kiosk` для demo | Идентификатор физического киоска |
| `mallCode` | из bootstrap | Для categories, places и events |
| `lang` | из bootstrap/default или выбор пользователя | Всегда передавать как query parameter |
| `categoryId` | пусто | Текущий фильтр |
| `placeId` | пусто | Выбранное место/событие |
| `profile` | `STANDARD` | `STANDARD` или `ACCESSIBLE` |
| `routeId` | пусто | Результат создания маршрута |
| `shareId` | пусто | Контекст feedback после share |
| `overlayUrl` | пусто | URL PNG текущего этажа |
| `routeUrl` | пусто | Мобильная HTTPS-ссылка |
| `qrImageUrl` | пусто | URL QR PNG |
| `correlationId` | новый ID на kiosk flow | 8–128 символов `[A-Za-z0-9._-]` |

Передавать `X-Correlation-ID: {correlationId}` во все JSON-запросы. Для POST также передавать
`Content-Type: application/json`; для JSON-ответов — `Accept: application/json`.

## Interface Assets

Создать отдельный REST Interface Asset для каждой операции. Это проще проверять и не связывает
параметры разных экранов.

| IA | Метод и path относительно `baseOrigin` | Основной результат |
| --- | --- | --- |
| Bootstrap | `GET /api/v1/kiosks/{kioskCode}/bootstrap?lang={lang}` | mall, язык, `languages[]`, `floors[]` |
| Categories | `GET /api/v1/malls/{mallCode}/categories?lang={lang}` | плоская коллекция категорий |
| Places | `GET /api/v1/malls/{mallCode}/places?q={q}&categoryId={categoryId}&lang={lang}` | до 50 мест |
| Place | `GET /api/v1/places/{placeId}?lang={lang}` | карточка места |
| Events | `GET /api/v1/malls/{mallCode}/events/today?lang={lang}` | события локальных суток ТЦ |
| Create route | `POST /api/v1/routes` | `routeId`, ETA, `instructions[]`, `overlays[]` |
| Get route | `GET /api/v1/routes/{routeId}` | повторное чтение временного маршрута |
| Share route | `POST /api/v1/routes/{routeId}/share` | `shareId`, `routeUrl`, `qrImageUrl`, expiry |
| Feedback | `POST /api/v1/feedback` | success `204`, без response body |

Binary endpoints overlay/QR не нужно моделировать как JSON IA. Их URL передаётся в Image Asset
action `Change image`.

## POST bodies

Маршрут:

```json
{
  "kioskCode": "tsum-l1-main-kiosk",
  "placeId": "00000000-0000-0000-0000-000000000401",
  "profile": "STANDARD"
}
```

Feedback до share:

```json
{
  "routeId": "{routeId}",
  "shareId": null,
  "rating": 5
}
```

Feedback после share использует ровно один контекст:

```json
{
  "routeId": null,
  "shareId": "{shareId}",
  "rating": 5
}
```

## Экранный flow

### 1. Startup

1. Сгенерировать `correlationId`, показать loading и вызвать Bootstrap.
2. На success сохранить `mallCode`, `defaultLanguage`, `selectedLanguage`, `currentFloorCode`.
3. Построить переключатель языка из `languages[]` и список этажей из `floors[]`.
4. Разрешить `mapImageUrl`: абсолютный URL оставить как есть, относительный соединить с
   `baseOrigin`.
5. Затем вызвать Categories, Places без фильтров и Events.

### 2. Каталог и поиск

- По выбору языка обновить `lang` и refresh Bootstrap/Categories/Places/Events/Place.
- По выбору категории установить `categoryId`; для «Все» передать пустое значение.
- По вводу поиска установить `q`; backend сам ограничивает результат 50 элементами.
- Элемент списка использует `id`, `name`, `description`, `floorCode`, `categoryCode`.
- Выбор события ведёт к тому же Place flow через `event.placeId`.

### 3. Карточка места

1. Сохранить `placeId` выбранного item.
2. Вызвать Place и показать локализованные поля.
3. Кнопка «Маршрут» вызывает Create route с выбранным movement profile.

### 4. Маршрут и overlay

1. На `201` сохранить `routeId`, distance, ETA, expiry и `instructions[]`.
2. Отсортировать `overlays[]` по `displayOrder`.
3. Для текущего overlay разрешить `imageUrl` относительно `baseOrigin`.
4. Показать базовую карту этажа ниже, прозрачный PNG overlay выше с одинаковыми bounds.
5. Вызвать Image Asset `Change image(overlayUrl)`, а не bind к статическому Image property.
6. Для multi-floor route переключать пару map/overlay по `floorCode`; demo содержит только L1.

Route overlay URL уникален благодаря `routeId`, поэтому session media cache не должен показать слой
предыдущего маршрута. Ответ backend также содержит `Cache-Control: no-store`.

### 5. QR и мобильный маршрут

1. Вызвать Share route без body.
2. Сохранить `shareId`, `routeUrl`, `qrImageUrl`, `expiresAt`.
3. Передать абсолютный `qrImageUrl` в отдельный Image Asset через `Change image`.
4. QR открывает `/r/{shareToken}`; токен вручную в experience не хранить и не логировать.

### 6. Feedback

- Разрешить rating только `1..5`.
- Передать ровно один из `routeId`/`shareId`.
- Success — `204` без тела: показать локальное подтверждение и заблокировать повторный tap.
- Проверить, что целевой Player вызывает success trigger на `204`.

## Loading и ошибки

Для каждого IA связать `Request started` с loading state, success trigger — с показом новых данных,
`Request failed` — с очисткой/сокрытием старых данных. Сохранять status code и raw response для
диагностики, но не показывать технический текст посетителю.

Problem Details содержит `status`, стабильный `code`, `detail`, необязательные `errors` и
`correlationId`. Основные UI-ветки:

- `400` — подсветить неверный ввод или сбросить испорченный локальный state;
- `404` — ресурс/route/share истёк или не найден; вернуть пользователя в каталог;
- `422` — киоск и место несовместимы; показать недоступность маршрута;
- `429` — временно заблокировать повтор и учитывать `Retry-After`;
- `503` — граф ещё не готов; предложить повторить позже;
- network/CORS/TLS — отдельный экран «Нет связи», без показа cached response.

В support-лог передавать endpoint, status, `code` и `correlationId`; route/share token не логировать.

## Idle/reset

Backend session отсутствует, поэтому server-side logout не нужен. По idle timeout:

1. остановить локальные timers/animations;
2. скрыть overlay, QR, ошибки, карточку и результаты предыдущего поиска;
3. очистить `categoryId`, `placeId`, `routeId`, `shareId`, URLs и rating;
4. вернуть `profile=STANDARD` и стартовую сцену;
5. сгенерировать новый `correlationId`;
6. повторить Bootstrap, чтобы проверить активность киоска и актуальную конфигурацию.

Временные route/share записи истекут на backend автоматически.

## Demo smoke для Егора

- mall: `tsum-demo`;
- kiosk: `tsum-l1-main-kiosk`;
- floor: `L1`;
- первая place: `00000000-0000-0000-0000-000000000401` (`gallery`);
- map: `/demo/maps/tsum-l1.png`;
- языки: `ru`, `en`.

Запуск demo backend описан в [`README.md`](../README.md). Карта, расстояния, места и доступность
демонстрационные и требуют натурной проверки.

## Acceptance checklist

- [ ] Composer и Player version/license зафиксированы;
- [ ] Bootstrap, Categories, Places, Place и Events работают через HTTPS;
- [ ] query/header/body parameters меняются actions/bindings;
- [ ] `201` create/share вызывает ожидаемый success trigger и отдаёт response properties;
- [ ] `204` feedback вызывает ожидаемый success trigger без body;
- [ ] overlay и QR загружаются через `Change image` на целевом Player;
- [ ] относительные и абсолютные URLs разрешаются корректно;
- [ ] `STANDARD` и `ACCESSIBLE` передаются без преобразования регистра;
- [ ] `400/404/422/429/503`, network, CORS и TLS errors имеют отдельные UI transitions;
- [ ] после Request failed не остаются видимыми cached данные;
- [ ] idle/reset полностью очищает пользовательский контекст и повторяет Bootstrap;
- [ ] полный demo flow пройден минимум десять раз без ручного reset Composer;
- [ ] результат и обнаруженные ограничения добавлены в основной план.
