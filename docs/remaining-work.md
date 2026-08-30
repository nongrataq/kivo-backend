# Оставшиеся работы Kivo Backend

Актуально на 30 августа 2026 года после merge PR #8 (`8f6ef1e`) и финализации `main` (`b481adf`).
Подробный исторический план остаётся в
[`docs/plans/20260809-kivo-backend.md`](plans/20260809-kivo-backend.md).

## Подтверждённое состояние

- [x] события и анонимный feedback реализованы и протестированы;
- [x] admin publication защищён, настроены CORS, rate limiting, correlation ID и HTTP-логи;
- [x] readiness/liveness, метрики, backup/restore/rollback описаны;
- [x] воспроизводимый non-root image и production-like Compose подготовлены;
- [x] GitHub Actions содержит formatting, clean-DB, full-test, container-smoke и GHCR publish jobs;
- [x] локально 30 августа 2026 года прошли Spotless, clean-DB, 152 теста, Docker build и HTTPS smoke;
- [x] `gradlew` исправлен с mode `100644` на `100755`, содержимое wrapper не менялось;
- [x] удалённый job `quality` прошёл formatting, clean-DB и все тесты;
- [x] Linux-specific отказ `container-smoke` локализован: secret-файлы mode `0600` не читались
  non-root процессом `10001:10001`; smoke и deployment-инструкция исправлены;
- [!] следующий удалённый run должен подтвердить `container-smoke` и публикацию GHCR.

## Приоритет 0 — вернуть зелёный main

- [x] оформить согласованный прямой commit в `main`;
- [x] проверить в diff изменение `gradlew` только `100644 → 100755`;
- [ ] дождаться зелёных jobs `quality`, `container-smoke` и `publish` после исправления Linux secrets;
- [ ] проверить, что образ появился в GHCR с ожидаемыми `sha-*` и `edge` tags;
- [ ] сохранить ссылку на зелёный run в release/acceptance заметках.
- [ ] [+] обновить pinned GitHub Actions до Node 24-compatible major versions, включая
  `actions/setup-java@v5`; текущие версии пока запускаются через forced Node 24 и дают warning.

## Приоритет 1 — интеграция и приёмка

- [ ] выполнить Intuiface spike по инструкции
  [`docs/intuiface-integration.md`](intuiface-integration.md);
- [ ] подтвердить в используемых Composer/Player обработку ответов `201` и `204`;
- [ ] проверить GET/POST, JSON body, headers, HTTPS/CORS, error triggers и remote PNG;
- [ ] пройти kiosk flow: bootstrap → поиск → карточка → маршрут → overlay → QR → feedback;
- [ ] проверить idle/reset без серверной сессии и сокрытие устаревших данных после Request failed;
- [ ] добавить performance regression на импортированной L1 topology;
- [ ] провести нагрузочный прогон поиска, построения маршрута и sharing;
- [ ] выполнить acceptance восстановления PostgreSQL из backup и forward-only rollback приложения.

## Приоритет 1 — staging и эксплуатационная среда

- [ ] предоставить staging PostgreSQL и отдельную БД/схему;
- [ ] настроить secret storage для DB password и BCrypt admin hash;
- [ ] назначить HTTPS domain, сертификат и точный Intuiface CORS origin;
- [ ] настроить сбор JSON-логов, Prometheus scrape и алерты из `docs/operations.md`;
- [ ] проверить GHCR package permissions и pull с staging host;
- [ ] зафиксировать владельцев backup, restore drill и incident response.

## Внешние данные и блокеры

- [!] Ариадна/владелец площадки: финальные планы, размеры, двери, запретные зоны и точки переходов;
- [!] площадка: реальные расстояния и проверка доступности маршрутов для МГН;
- [!] команда контента: итоговые языки, переводы, места и события;
- [!] Егор: версия Composer, тип Player, целевая ОС и доступные license features;
- [!] DevOps: hosting, domain, TLS, monitoring, production PostgreSQL и secret storage.

Демо L1 нельзя использовать как as-built схему. Его координаты, метрика, якоря мест и доступность
должны быть подтверждены на площадке; подробности находятся в
[`docs/demo/tsum-l1-data.md`](demo/tsum-l1-data.md).

## Условия первого тега

- [ ] GitHub Actions на `main` полностью зелёный;
- [ ] Intuiface acceptance checklist пройден на целевом Player;
- [ ] staging развернут из опубликованного immutable image;
- [ ] нагрузочные и restore проверки завершены без блокирующих дефектов;
- [ ] требования и OpenAPI сверены с фактическим поведением;
- [ ] основной план перенесён в `docs/plans/completed/` только после выполнения пунктов выше.
