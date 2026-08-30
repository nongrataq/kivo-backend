# Финализация main и передача интеграции Intuiface

## Overview

- Восстановить работоспособность GitHub Actions после merge `chore/deployment-foundation`.
- Выполнить локальный эквивалент всех backend gates и зафиксировать фактический результат.
- Синхронизировать основной план с уже реализованными задачами.
- Подготовить для Егора самостоятельную инструкцию по интеграции Kivo API с Intuiface.

## Context (from discovery)

- `main` синхронизирован с `origin/main`, HEAD: merge PR #8.
- Последний workflow завершился до тестов с `./gradlew: Permission denied`.
- Git хранит `gradlew` с mode `100644`; Linux runner требует `100755`.
- CI выполняет `spotlessCheck`, Flyway clean-database test, полный `test`, Docker build и
  production-like HTTPS smoke.
- `README.md`, `docs/deployment.md` и `docs/operations.md` уже описывают реализованную основу.
- `docs/plans/20260809-kivo-backend.md` отстаёт от фактического состояния задач 9, 10 и 12.
- Отдельного handoff по Intuiface пока нет.

## Development Approach

- **Testing approach:** regular — сначала минимальное исправление, затем все проверки CI.
- Не менять доменную модель и публичный API без подтверждённой причины.
- Завершать и проверять каждый логический шаг до перехода к следующему.
- Обновлять этот план при изменении объёма или появлении блокеров.

## Testing Strategy

- Проверить Spotless тем же Gradle task, что использует CI.
- Отдельно проверить Flyway на пустой PostgreSQL через Testcontainers.
- Выполнить полный набор Gradle-тестов.
- Собрать production image и выполнить HTTPS/container smoke.
- Проверить документацию, diff, Git file mode и итоговый статус рабочей копии.

## Solution Overview

- Исправить только Git file mode у `gradlew`, не изменяя содержимое wrapper.
- Устранять дополнительные проблемы лишь при воспроизведённом падении gate или подтверждённой
  ошибке контракта.
- Обновить основной backlog по фактическим merge и явно отделить оставшуюся внутреннюю работу от
  внешних зависимостей.
- Описать Intuiface как последовательность экранов и состояний: bootstrap, каталог, место, маршрут,
  overlay, share/QR, события, feedback, ошибки и reset.

## Implementation Steps

### Task 1: Восстановить запуск Gradle Wrapper в Linux CI

**Files:**
- Modify metadata: `gradlew`

- [x] установить Git executable bit `100755` для `gradlew`
- [x] убедиться, что содержимое `gradlew` не изменилось
- [x] проверить staged/working diff file mode

### Task 2: Выполнить все backend gates

**Files:**
- Verify: `.github/workflows/ci.yml`
- Verify: `scripts/ci/container-smoke.sh`

- [x] выполнить `spotlessCheck`
- [x] выполнить Flyway clean-database integration test
- [x] выполнить полный `test`
- [x] собрать production Docker image
- [x] выполнить production-like HTTPS/container smoke
- [x] исправить подтверждённые ошибки и повторить затронутые проверки

### Task 3: Синхронизировать основной план

**Files:**
- Modify: `docs/plans/20260809-kivo-backend.md`
- Create: `docs/remaining-work.md`

- [x] отметить фактически завершённые пункты задач 9, 10 и 12
- [x] обозначить оставшиеся backend-задачи и внешние блокеры
- [x] зафиксировать результат финальных gates
- [x] не переносить основной план в `completed`, пока остаются незавершённые задачи

### Task 4: Подготовить handoff по Intuiface для Егора

**Files:**
- Create: `docs/intuiface-integration.md`
- Verify: `docs/api/openapi.yaml`
- Verify: `docs/demo/tsum-l1-data.md`

- [x] описать окружение, base URL, HTTPS/CORS и demo-ограничения
- [x] сопоставить экраны и действия с API endpoints и полями ответа
- [x] привести POST payloads, overlay/QR URL и правила обработки ошибок
- [x] описать состояние приложения, idle/reset и отсутствие backend-сессии
- [x] добавить пошаговый demo flow и acceptance checklist

### Task 5: Финальная проверка изменений

**Files:**
- Verify: все изменённые файлы

- [x] применить проектное форматирование к изменённым исходникам, если такие появятся
- [x] проверить Markdown и ссылки вручную
- [x] выполнить сборку через Amplicode
- [x] просмотреть итоговый diff и `git status`
- [x] отметить завершённые пункты этого плана

## Validation result

- `spotlessCheck`: passed;
- Flyway clean-database integration test: 1 test, 0 failures/errors;
- full `test`: 152 tests in 51 suites, 0 failures/errors/skipped;
- Docker production image build: passed;
- container smoke: passed (non-root app, HTTPS readiness, Flyway 5);
- OpenAPI example serialization test: passed after contract correction;
- Markdown links and Amplicode rebuild: passed.

## Post-Completion

- Commit/push и новый GitHub Actions run выполняются только после явного решения владельца репозитория.
- Staging, production secrets, домен/TLS, натурная проверка топологии и проверка в реальном Intuiface
  требуют внешней среды и участия команды.
