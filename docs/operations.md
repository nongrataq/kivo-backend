# Эксплуатация Kivo Backend

Документ описывает production-контур безопасности и базовые операции для одного экземпляра Kivo
Backend. Команды приведены как шаблоны: адреса, имена контейнеров и secret references нужно
адаптировать к конкретной платформе.

## Production-конфигурация

Запускайте приложение с профилем `prod` за HTTPS reverse proxy. До старта передайте секреты через
переменные окружения или secret storage, не через файлы в Git:

| Переменная | Назначение |
| --- | --- |
| `DB_URL` | JDBC URL PostgreSQL |
| `DB_USERNAME` | отдельный пользователь приложения |
| `DB_PASSWORD` | пароль пользователя приложения |
| `KIVO_ADMIN_USERNAME` | имя staff-пользователя admin API |
| `KIVO_ADMIN_PASSWORD_HASH` | BCrypt-хеш пароля, а не открытый пароль |
| `KIVO_CORS_ALLOWED_ORIGINS` | точный список HTTPS origins киосков и Intuiface |
| `KIVO_TRUSTED_PROXY_REGEX` | регулярное выражение только для адресов доверенных proxy |
| `PUBLIC_BASE_URL` | внешний HTTPS URL для QR-ссылок |

Не используйте `*` для CORS production-контура. Reverse proxy должен перезаписывать, а не
дописывать недоверенные клиентские `Forwarded`/`X-Forwarded-*` заголовки. В
`KIVO_TRUSTED_PROXY_REGEX` нельзя включать публичные сети или адреса клиентов.

Admin endpoints под `/api/v1/admin/**` требуют HTTP Basic пользователя с ролью `STAFF`. Basic
authentication допустима только поверх HTTPS. Публичные endpoints остаются без аутентификации,
но создание маршрута, поиск, sharing и feedback ограничиваются rate limiter.

## Health, метрики и логи

- liveness: `GET /actuator/health/liveness`;
- readiness: `GET /actuator/health/readiness`;
- Prometheus: `GET /actuator/prometheus`;
- correlation ID: входной `X-Correlation-ID` принимается только в допустимом формате, иначе
  генерируется новый; значение возвращается в ответе и добавляется в MDC логов;
- production-логи выводятся в структурированном JSON формате.

Readiness считается успешной только при доступной БД и наличии минимального числа активных
навигационных графов. После запуска сначала дождитесь readiness, затем включайте экземпляр в
балансировщик. Сигналы для alerting:

- readiness остаётся `DOWN` после установленного deployment timeout;
- растёт `kivo.http.rate_limit.rejected`;
- появляются ответы `5xx` или частые `401/403` для admin API;
- отсутствует активный граф после публикации или рестарта;
- заканчивается место PostgreSQL либо резервные копии не проходят проверку восстановления.

## Резервное копирование PostgreSQL

Создавайте logical backup утилитой той же major-версии PostgreSQL, что и сервер. Пример custom
format без включения пароля в командную строку:

```powershell
$env:PGPASSWORD = '<получить из secret storage>'
pg_dump --host '<db-host>' --port 5432 --username '<backup-user>' `
  --dbname kivo --format custom --file 'kivo-YYYYMMDD-HHMM.dump' `
  --no-owner --no-privileges
Remove-Item Env:PGPASSWORD
```

Backup считается готовым только после проверки ненулевого размера, checksum и пробного
восстановления в изолированную БД. Храните несколько поколений в зашифрованном хранилище вне
основного сервера; срок хранения и RPO/RTO задаются инфраструктурной политикой.

## Проверка восстановления

Восстановление всегда выполняется сначала в новую пустую БД. Не запускайте приложение против неё,
пока `pg_restore` и Flyway validation не завершились успешно.

```powershell
$env:PGPASSWORD = '<получить из secret storage>'
createdb --host '<db-host>' --username '<restore-user>' kivo_restore_check
pg_restore --host '<db-host>' --username '<restore-user>' `
  --dbname kivo_restore_check --clean --if-exists --no-owner --no-privileges `
  'kivo-YYYYMMDD-HHMM.dump'
Remove-Item Env:PGPASSWORD
```

После восстановления:

1. Запустите один Kivo Backend с профилем `prod` и URL тестовой БД.
2. Убедитесь, что Flyway validation успешен и новых миграций поверх backup нет либо они применились
   штатно.
3. Проверьте readiness, bootstrap тестового киоска, поиск, построение маршрута и admin publication
   под staff-учётной записью.
4. Удалите изолированную БД только после фиксации результата проверки.

## Rollback приложения

Flyway-миграции считаются forward-only: откат контейнера не должен автоматически откатывать или
удалять схему. Перед release сохраните digest текущего образа и backup БД.

1. Уберите проблемный экземпляр из балансировщика.
2. Остановите дальнейшую публикацию графов и административные изменения.
3. Если новая версия не меняла схему несовместимо, запустите предыдущий image по сохранённому
   digest против текущей БД и дождитесь readiness.
4. Если схема несовместима с предыдущим image, создайте новую БД из pre-release backup, проверьте
   её изолированно и только затем переключите приложение/секреты подключения.
5. Выполните smoke-сценарий: bootstrap → поиск → маршрут → sharing → публичное получение.
6. Зафиксируйте incident, correlation IDs, версии image/графа и решение о повторном release.

Нельзя выполнять Flyway `clean`, вручную удалять migration history или восстанавливать backup
поверх единственной production-БД. Переключение на восстановленную БД должно оставаться обратимым
до завершения smoke-проверок.

## Публикация навигационного графа

Перед публикацией проверьте, что версия графа относится к нужному ТЦ и все публичные места
достижимы от активных киосков. Вызов admin endpoint выполняется только из доверенной сети через
HTTPS. После успешной публикации проверьте readiness и один маршрут каждого поддерживаемого
профиля движения. При ошибке публикации активный immutable snapshot должен остаться прежним.
