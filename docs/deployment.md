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
$secretFile = New-TemporaryFile
$databasePassword = [Convert]::ToHexString(
  [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
).ToLowerInvariant()
[IO.File]::WriteAllText($secretFile.FullName, $databasePassword)
$env:KIVO_DB_PASSWORD_FILE = $secretFile.FullName
$env:KIVO_PUBLIC_BASE_URL = 'https://localhost:8443'
$projectName = "kivo-local-$PID"
try {
  docker compose -p $projectName -f compose.production.yaml up -d --wait
  curl.exe --fail --silent --show-error --insecure `
    https://localhost:8443/actuator/health/readiness
} finally {
  docker compose -p $projectName -f compose.production.yaml down -v
  Remove-Item -LiteralPath $secretFile.FullName
}
```

Пароль поступает в Compose только через путь `KIVO_DB_PASSWORD_FILE`, создаётся как Docker secret и
монтируется в приложение как config tree. Не сохраняйте production-значения в `.env`, shell history
или Git; в реальном оркестраторе материализуйте файл из его secret storage. `.env` и `.env.*`
игнорируются Git.

На Linux полный автоматический smoke gate можно повторить так:

```bash
docker build --pull --tag kivo-backend:smoke .
KIVO_IMAGE=kivo-backend:smoke bash scripts/ci/container-smoke.sh
```

Скрипт генерирует временный пароль, ждёт container healthcheck, проверяет readiness через HTTPS,
non-root пользователя и версию `flyway_schema_history`, затем всегда удаляет контейнеры и volumes.

## Эксплуатационные переменные

| Переменная | Обязательность | Назначение |
|---|---:|---|
| `KIVO_IMAGE` | да | Проверенный OCI image reference, предпочтительно digest |
| `KIVO_DB_PASSWORD_FILE` | да | Путь к runtime secret-файлу с паролем PostgreSQL |
| `KIVO_PUBLIC_BASE_URL` | да | Внешний HTTPS origin для мобильных/QR-ссылок |
| `KIVO_DB_NAME` | нет | Имя БД, по умолчанию `kivo` |
| `KIVO_DB_USERNAME` | нет | Пользователь БД, по умолчанию `kivo` |
| `KIVO_PUBLIC_HOST` | нет | Host для локального Caddy, по умолчанию `localhost` |
| `KIVO_HTTPS_PORT` | нет | Локальный published port, по умолчанию `8443` |
| `KIVO_JAVA_TOOL_OPTIONS` | нет | JVM limits; по умолчанию container-aware RAM/OOM policy |

Для прямого запуска приложения без Compose production-профиль требует `DB_URL`, `DB_USERNAME`,
пароль через `DB_PASSWORD` или config tree и `PUBLIC_BASE_URL`.

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

## Reverse proxy и будущая security-интеграция

Caddy `reverse_proxy` формирует стандартные `X-Forwarded-For`, `X-Forwarded-Proto` и
`X-Forwarded-Host`, сохраняет исходный `Host` и не публикует app port. Security-ветка должна отдельно:

1. включить доверенную обработку forwarded headers в Spring только за известным proxy;
2. добавить HTTPS redirect/policy и CORS без дублирования TLS termination в приложении;
3. заменить локальный `tls internal` на сертификат/ACME-конфигурацию целевого окружения;
4. ограничить доверенные proxy ranges на уровне платформы и проверить spoofed forwarded headers.

До интеграции security-ветки приложение не принимает решений авторизации на основании forwarded
headers. Caddyfile намеренно остаётся минимальным стандартным proxy-контрактом.

## Публикация GHCR

Workflow запускает gates для pull request, любого branch push, manual run и опубликованного GitHub
Release. Публикация в `ghcr.io/<owner>/<repository>` разрешена только после зелёных `quality` и
`container-smoke` jobs:

- push в `main` публикует `sha-<commit>` и `edge`;
- опубликованный Release публикует `sha-<commit>`, release tag и `latest` только для non-prerelease;
- pull request, feature branch и manual run никогда не публикуют image.

Release/staging остаются внешними операциями: нужны GHCR package permissions, production PostgreSQL,
домен, доверенный TLS и platform secret storage.
