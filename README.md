# Kivo Backend

Бэкенд системы навигации внутри крупных торговых пространств. Сервис предоставляет JSON REST API
для киосков на Intuiface, строит маршруты по этажам и позволяет передавать маршрут на телефон с
помощью QR-кода.

## Технологический стек

- Java 25;
- Spring Boot 4.1;
- Gradle с Kotlin DSL и Java toolchain;
- PostgreSQL и Flyway;
- Spring Data JPA;
- JUnit 5 и Testcontainers для интеграционных тестов;
- Spotless с google-java-format.

Приложение разрабатывается как модульный монолит. Каталог, карты, киоски, навигация, события,
передача маршрута и отзывы остаются частями одного развёртываемого сервиса, но изолируются друг от
друга на уровне пакетов и контрактов.

Направление зависимостей внутри feature-модуля: `api → application → persistence`. Контроллеры
работают только с DTO, а repositories доступны только внутри владеющего ими модуля. Эти правила
проверяются архитектурными тестами.

Для Intuiface API возвращает плоский JSON без массивов внутри массивов. Базовые карты загружаются
по HTTPS, а маршруты отображаются прозрачными PNG-слоями, уникальными для каждого route ID.

## Локальный запуск

Для запуска необходимы Docker Desktop и JDK 17 или новее. Требуемую для компиляции Java 25 Gradle
toolchain загрузит автоматически.

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

Локальные параметры PostgreSQL определены в `compose.yaml`. В других окружениях их нужно задавать
через переменные `DB_URL`, `DB_USERNAME` и `DB_PASSWORD`.

## Проверки

```powershell
.\gradlew.bat spotlessCheck test
```

Интеграционные тесты поднимают чистый PostgreSQL 17 через Testcontainers и применяют все миграции
Flyway. Поэтому для полного quality gate должен быть запущен Docker Desktop.

## Локальная демонстрация navigation-core MVP

Профиль `demo` использует отдельную схему PostgreSQL `kivo_demo`, применяет основные миграции и
добавляет демонстрационный ТЦ, этаж, киоск, два места и опубликованный навигационный граф. Эти
данные не попадают в production-профиль.

```powershell
docker compose up -d postgres
.\gradlew.bat bootRun --args='--spring.profiles.active=demo'
```

После запуска можно пройти основной сценарий:

```powershell
$bootstrap = Invoke-RestMethod 'http://localhost:8080/api/v1/kiosks/demo-kiosk/bootstrap?lang=ru'
$places = Invoke-RestMethod 'http://localhost:8080/api/v1/malls/demo-mall/places?lang=ru'
$route = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/v1/routes' `
  -ContentType 'application/json' `
  -Body '{"kioskCode":"demo-kiosk","placeId":"00000000-0000-0000-0000-000000000401"}'
Invoke-WebRequest `
  -Uri ("http://localhost:8080{0}" -f $route.overlays[0].imageUrl) `
  -OutFile 'route-overlay.png'
$share = Invoke-RestMethod -Method Post `
  -Uri ("http://localhost:8080/api/v1/routes/{0}/share" -f $route.routeId)
$shareToken = ([uri]$share.routeUrl).Segments[-1]
Invoke-WebRequest `
  -Uri $share.qrImageUrl `
  -OutFile 'route-qr.png'
$publicRoute = Invoke-RestMethod `
  ("http://localhost:8080/api/v1/public/routes/{0}" -f $shareToken)
```

Базовый план этажа доступен по адресу `http://localhost:8080/demo/maps/L1.svg`. Профиль создаёт
схему автоматически; для другой demo-БД задайте `DEMO_DB_URL`, `DEMO_DB_USERNAME` и
`DEMO_DB_PASSWORD`.

## Реализованный API

```text
GET /api/v1/kiosks/{kioskCode}/bootstrap?lang=
GET /api/v1/malls/{mallCode}/categories?lang=
GET /api/v1/malls/{mallCode}/places?q=&categoryId=&lang=
GET /api/v1/places/{placeId}?lang=
POST /api/v1/routes
GET /api/v1/routes/{routeId}
GET /api/v1/routes/{routeId}/floors/{floorCode}/overlay.png
POST /api/v1/routes/{routeId}/share
GET /api/v1/public/routes/{shareToken}
GET /api/v1/public/routes/{shareToken}/qr.png
GET /api/v1/public/routes/{shareToken}/floors/{floorCode}/overlay.png
GET /r/{shareToken}
```

`lang` необязателен: без него используется язык ТЦ по умолчанию. Ответы не содержат внутренних
navigation node codes; bootstrap возвращает только плоские элементы массивов языков и этажей.

## Документация

Живой план реализации и архитектурные решения находятся в
[`docs/plans/20260809-kivo-backend.md`](docs/plans/20260809-kivo-backend.md). План обновляется после
каждой завершённой задачи или изменения объёма работ.

## Работа с Git

- новые возможности: `feature/<название>`;
- срочные исправления: `hotfix/<название>`;
- исправления: `fix/<название>`;
- документация: `docs/<название>`;
- технические изменения: `chore/<название>`.

Каждая ветка создаётся от актуального `main`. После прохождения тестов и форматирования однозначные
изменения можно слить в `main` самостоятельно; спорные решения отправляются отдельным pull request.

## Лицензия

Copyright © 2026 nongrataq. Все права защищены. Использование, запуск, копирование, изменение или
распространение кода без предварительного письменного разрешения правообладателя запрещено.
Полные условия приведены в файле [`LICENSE`](LICENSE).
