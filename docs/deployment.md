# Deployment foundation

Kivo поставляется как один OCI-образ и ожидает PostgreSQL 17. Перед публикацией GitHub Actions
проверяет форматирование, полный набор тестов, миграции чистой БД, сборку образа и HTTPS smoke test.

## Сборка образа

```powershell
docker build --pull --tag kivo-backend:local .
docker image inspect kivo-backend:local --format '{{.Config.User}}'
```

Ожидаемый пользователь runtime-образа — `10001:10001`. Multi-stage build использует зафиксированные
patch-теги и multi-platform digests Eclipse Temurin Java 25, Gradle Wrapper с SHA-256 distribution
и воспроизводимые Gradle архивы. PostgreSQL/Caddy также закреплены tag+digest. Обновление любого
base image выполняется отдельным проверяемым изменением.

## Production-like запуск с HTTPS

`compose.production.yaml` поднимает PostgreSQL, Kivo и Caddy. PostgreSQL не публикует порт наружу,
Kivo доступен только внутри закрытой Docker-сети, наружу открыт только HTTPS reverse proxy. Локальный
Caddy использует внутренний CA, поэтому сертификат не будет доверенным для браузера без импорта CA.

PowerShell 7:

```powershell
$env:KIVO_IMAGE = 'kivo-backend:local'
$databaseSecretFile = New-TemporaryFile
$adminHashSecretFile = New-TemporaryFile
$databasePassword = [Convert]::ToHexString(
  [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
).ToLowerInvariant()
[IO.File]::WriteAllText($databaseSecretFile.FullName, $databasePassword)
[IO.File]::WriteAllText(
  $adminHashSecretFile.FullName,
  '$2a$10$jSFJWQ5.f1dUERLQRaGrAeV4lUa.Ki18LHtMLIrF2CQ/9F7nppvim'
)
$env:KIVO_DB_PASSWORD_FILE = $databaseSecretFile.FullName
$env:KIVO_ADMIN_PASSWORD_HASH_FILE = $adminHashSecretFile.FullName
$env:KIVO_ADMIN_USERNAME = 'local-admin'
$env:KIVO_PUBLIC_BASE_URL = 'https://localhost:8443'
$env:KIVO_CORS_ALLOWED_ORIGINS = $env:KIVO_PUBLIC_BASE_URL
$env:KIVO_MINIMUM_ACTIVE_GRAPHS = '0'
$projectName = "kivo-local-$PID"
try {
  docker compose -p $projectName -f compose.production.yaml up -d --wait
  curl.exe --fail --silent --show-error --insecure `
    https://localhost:8443/actuator/health/readiness
} finally {
  docker compose -p $projectName -f compose.production.yaml down -v
  Remove-Item -LiteralPath $databaseSecretFile.FullName
  Remove-Item -LiteralPath $adminHashSecretFile.FullName
}
```

Пароль БД и BCrypt-хеш admin-пароля поступают в Compose только через пути к файлам, создаются как
Docker secrets и монтируются в приложение как config tree. Известный хеш выше предназначен только
для локального smoke (пароль `password`). Не сохраняйте production-значения в `.env`, shell history
или Git; в реальном оркестраторе материализуйте файлы из его secret storage. `.env` и `.env.*`
игнорируются Git.

На Linux Compose сохраняет host ownership и mode у bind-mounted secret-файлов. Оба файла должны
быть доступны на чтение runtime-пользователю `10001:10001`: для production предпочтительны owner
`10001:10001` и mode `0400` либо группа `10001` и mode `0440`. Smoke-скрипт хранит сгенерированные
файлы внутри приватного каталога `0700`, но задаёт самим файлам mode `0644` для переносимости между
Linux CI и Docker Desktop; каталог удаляется сразу после проверки.

На Linux полный автоматический smoke gate можно повторить так:

```bash
docker build --pull --tag kivo-backend:smoke .
KIVO_IMAGE=kivo-backend:smoke bash scripts/ci/container-smoke.sh
```

Скрипт генерирует временные secrets, ждёт liveness container healthcheck, проверяет readiness через
HTTPS, non-root пользователя и версию `flyway_schema_history`, затем всегда удаляет контейнеры и
volumes. Для пустой smoke-БД он явно задаёт минимум активных графов `0`; production по умолчанию
требует хотя бы один активный граф.

## Эксплуатационные переменные

| Переменная | Обязательность | Назначение |
|---|---:|---|
| `KIVO_IMAGE` | да | Проверенный OCI image reference, предпочтительно digest |
| `KIVO_DB_PASSWORD_FILE` | да | Путь к runtime secret-файлу с паролем PostgreSQL |
| `KIVO_ADMIN_PASSWORD_HASH_FILE` | да | Путь к secret-файлу с BCrypt-хешем admin-пароля |
| `KIVO_ADMIN_USERNAME` | да | Имя staff-пользователя admin API |
| `KIVO_CORS_ALLOWED_ORIGINS` | да | Точный список разрешённых HTTPS origins |
| `KIVO_PUBLIC_BASE_URL` | да | Внешний HTTPS origin для мобильных/QR-ссылок |
| `KIVO_DB_NAME` | нет | Имя БД, по умолчанию `kivo` |
| `KIVO_DB_USERNAME` | нет | Пользователь БД, по умолчанию `kivo` |
| `KIVO_PUBLIC_HOST` | нет | Host для локального Caddy, по умолчанию `localhost` |
| `KIVO_HTTPS_PORT` | нет | Локальный published port, по умолчанию `8443` |
| `KIVO_TRUSTED_PROXY_REGEX` | нет | Regex доверенной proxy-сети; default — loopback и private Docker ranges |
| `KIVO_MINIMUM_ACTIVE_GRAPHS` | нет | Readiness-порог; production default `1`, для clean-DB smoke — `0` |
| `KIVO_JAVA_TOOL_OPTIONS` | нет | JVM limits; по умолчанию container-aware RAM/OOM policy |

Для прямого запуска приложения без Compose профиль `prod` требует `DB_URL`, `DB_USERNAME`, пароль
через `DB_PASSWORD` или config tree, `KIVO_ADMIN_USERNAME`, `KIVO_ADMIN_PASSWORD_HASH`,
`KIVO_CORS_ALLOWED_ORIGINS` и `PUBLIC_BASE_URL`.

## Миграции и пустая БД

Flyway запускается до JPA validation. `spring.flyway.validate-on-migrate=true` в production-профиле
останавливает startup при несовместимой истории. Отдельный Testcontainers-тест начинает с пустой
PostgreSQL 17.10, применяет все versioned migrations, запускает `Flyway.validate()` и сравнивает
текущую версию с последней доступной миграцией.

Локальные gates:

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle-user-home"
.\gradlew.bat --no-daemon spotlessCheck
.\gradlew.bat --no-daemon test `
  --tests com.kivo.backend.deployment.FlywayCleanDatabaseIntegrationTest
.\gradlew.bat --no-daemon test
```

Все тесты требуют доступный Docker daemon из-за обязательных PostgreSQL Testcontainers.

## Reverse proxy и security

Caddy `reverse_proxy` формирует стандартные `X-Forwarded-For`, `X-Forwarded-Proto` и
`X-Forwarded-Host`, сохраняет исходный `Host` и не публикует app port. Профиль `prod` включает HTTPS
redirect, HSTS, CORS и admin authentication. Default trusted-proxy regex охватывает private Docker
ranges, потому что app доступен только в закрытой backend-сети; на целевой платформе задайте
`KIVO_TRUSTED_PROXY_REGEX` точным CIDR/regex адресов ingress. Caddy должен заменять недоверенные
forwarded headers. Локальный `tls internal` перед production заменяется на реальный сертификат или
ACME-конфигурацию целевого окружения.

## Публикация GHCR

Workflow запускает gates для pull request, любого branch push, manual run и опубликованного GitHub
Release. Публикация в `ghcr.io/<owner>/<repository>` разрешена только после зелёных `quality` и
`container-smoke` jobs:

- push в `main` публикует `sha-<commit>` и `edge`;
- опубликованный Release публикует `sha-<commit>`, release tag и `latest` только для non-prerelease;
- pull request, feature branch и manual run никогда не публикуют image.

Release/staging остаются внешними операциями: нужны GHCR package permissions, production PostgreSQL,
домен, доверенный TLS и platform secret storage.
