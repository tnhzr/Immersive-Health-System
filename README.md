<div align="center">
  <img src=".github/assets/ihs_icon.png" alt="Immersive Health System" width="180" />

  # Immersive Health System

  **Иммерсивная система здоровья, болезней и лекарств для Paper 1.21.8.**
  *Immersive health, disease and laboratory plugin for Paper 1.21.8.*

  ![Paper](https://img.shields.io/badge/Paper-1.21.8-2b6cb0?style=flat-square)
  ![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square)
  ![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
  ![Languages](https://img.shields.io/badge/locale-RU%20%7C%20EN-orange?style=flat-square)
</div>

---

## Содержание · Table of contents

- [Русский](#русский)
  - [Описание](#описание)
  - [Возможности](#возможности)
  - [Установка](#установка)
  - [Конфигурация](#конфигурация)
  - [Команды](#команды)
  - [Локализация](#локализация)
  - [Расширение](#расширение-добавление-модулей)
- [English](#english)
  - [Overview](#overview)
  - [Features](#features)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Commands](#commands)
  - [Localization](#localization)
  - [Extending](#extending-adding-modules)

---

## Русский

### Описание

**Immersive Health System** — модульный плагин для Paper 1.21.8, реализующий
иммерсивную систему болезней, заражений, лекарств и собственного крафтового
блока «Лаборатория». Каждый модуль независим и включается/выключается в
`config.yml`. Все числовые значения, шансы, сообщения, звуки и тайминги
вынесены в конфиги — в коде нет «магических чисел».

> Плагин действует только на игроков. Мобы и прочие сущности не заражаются
> и не реагируют на болезни.

### Возможности

#### 🩺 Модуль 1 — Болезни (`modules.disease`)

- Шкала болезни 0 – 100 у каждого игрока с настраиваемыми диапазонами
  стадий (инкубация → лёгкие → тяжёлые → агония → смерть).
- Суточный прирост шкалы с модификаторами от низкого HP / голода.
- Полный сброс при смерти (опционально).
- Два типа заражения: `global` (случайный суточный ролл) и `local`
  (только контакт / частицы).
- Симптомы-ивенты `cough`, `sneeze`, `vomit`, `cough_blood`
  со своими звуками и частицами; прямое попадание частицы в игрока
  даёт повышенный шанс заражения, в радиусе — базовый.
- Команда `/spit` — игрок может намеренно плюнуть в цель.
- Конфигурируемые экшены смертельной стадии (`kill`, `spawn_zombie`).

#### 💊 Модуль 2 — Лекарства (`modules.medicine`)

- Кастомные предметы с PDC-тегом (`ihs:medicine_id`) — подделать нельзя.
- Полный отказ от ванильных эффектов GOLDEN_APPLE через FoodComponent.
- Три типа лекарств: `cure` (понижает шкалу и отменяет суточный рост),
  `effect_clear` (снимает выбранные эффекты), `buff` (накладывает эффекты).
- Дневной лимит приёма; при передозировке — `POISON` вместо лечения.
- Отключён ванильный эффект «молочное ведро снимает всё».

#### 🧪 Модуль 3 — Лаборатория (`modules.laboratory`)

- Крафтится ванильным `ShapedRecipe`:
  ```
  I I I        I = IRON_INGOT
  F B C        F = FURNACE  B = BREWING_STAND  C = CAULDRON
  I I I
  ```
- Визуал блока отвязан от логики (`laboratory.block_material`) — пакет
  CraftEngine может подменить host-блок без изменения плагина.
- 54-слотовый GUI: ввод, превью результата, кнопка «Начать»,
  область вывода, слот топлива.
- **Очередь крафта** — каждый клик по кнопке снимает 1 порцию ингредиентов
  и ставит +1 в очередь. Очередь продолжает работать при закрытом GUI.
- Поддержка ингредиентов `vanilla:`, `custom:` (по PDC) и
  «или-или»: `vanilla:coal|vanilla:charcoal:1`.
- Состояние лабораторий (инвентарь, прогресс, очередь, топливо)
  сохраняется в `laboratories.yml`.

### Установка

1. Скачайте `immersive-health-system-1.0.0.jar`.
2. Положите в папку `plugins/` сервера Paper 1.21.8.
3. Запустите сервер — плагин создаст `plugins/ImmersiveHealthSystem/` со всеми
   конфигами и языковыми файлами.
4. (По желанию) измените `language: ru` на `en` в `config.yml`.

### Конфигурация

| Файл | Описание |
| --- | --- |
| `config.yml`        | Глобальные настройки и переключатели модулей. |
| `infections.yml`    | Каталог болезней (название, тип, стадии, передача). |
| `medicines.yml`     | Каталог лекарств (PDC, тип, лимиты, эффекты). |
| `lab_recipes.yml`   | Рецепты Лаборатории и их время крафта. |
| `lang/messages_ru.yml` | Русская локализация. |
| `lang/messages_en.yml` | Английская локализация. |

### Команды

| Команда | Описание |
| --- | --- |
| `/ihs check <игрок> [scale\|infection]` | Показать шкалу или активные болезни. |
| `/ihs inject <игрок> <id_болезни> [шанс]` | Принудительно заразить с заданным шансом. |
| `/ihs heal <игрок> [id_болезни]` | Полностью или точечно вылечить игрока. |
| `/ihs modify <игрок> <значение>` | Установить точное значение шкалы. |
| `/ihs menu` | GUI со всеми онлайн-игроками. |
| `/ihs reload` | Перезагрузить YAML-конфиги и каталоги. |
| `/spit` | Намеренный плевок (повышенный шанс заражения цели). |

### Локализация

Плагин поставляется с двумя языками: **русский** (по умолчанию) и **английский**.
Активный язык выбирается в `config.yml`:

```yaml
# Plugin language: ru / en. Falls back to en if missing.
language: ru
```

Файлы локализации лежат в `plugins/ImmersiveHealthSystem/lang/`. Можно
свободно править существующие или добавить свой `messages_<код>.yml` —
плагин подхватит его, если в `language` указать соответствующий код. Если
ключ отсутствует в выбранном языке, используется английский fallback.

### Расширение (добавление модулей)

1. Реализуй `com.tnhzr.ihs.module.Module` (методы `id`, `enable`, `disable`).
2. Зарегистрируй модуль в `ImmersiveHealthSystem#onEnable` через
   `moduleManager.register(id, module)`.
3. Добавь переключатель `modules.<id>: true|false` в `config.yml`.

Система загрузки уважает переключатели и не падает целиком при ошибке
включения отдельного модуля.

### Сборка

```bash
mvn -DskipTests package
```

Готовый jar — `target/immersive-health-system-1.0.0.jar`.

---

## English

### Overview

**Immersive Health System** is a modular plugin for Paper 1.21.8 that adds
an immersive disease, infection and medicine system together with a custom
crafting station — the **Laboratory**. Every module is independent and can be
toggled in `config.yml`. Every numeric value, chance, message, sound and timing
is configurable — there are no magic numbers in the source.

> The plugin only affects players. Mobs and other entities are completely
> ignored.

### Features

#### 🩺 Module 1 — Disease System (`modules.disease`)

- Per-player disease scale 0 – 100 with configurable stage ranges
  (incubation → light → heavy → agony → death).
- Daily growth with low-health / hunger modifiers.
- Optional full reset on death.
- Two infection types: `global` (random daily roll) and `local`
  (contact / particles only).
- Symptom events: `cough`, `sneeze`, `vomit`, `cough_blood` — each with
  its own sound and particles; particles physically hitting a player
  apply an elevated chance, otherwise the radius chance applies.
- `/spit` command lets infected players intentionally infect their target.
- Configurable terminal-stage actions (`kill`, `spawn_zombie`).

#### 💊 Module 2 — Medicines (`modules.medicine`)

- Custom items with PDC tag (`ihs:medicine_id`) — cannot be forged.
- Completely bypasses vanilla `GOLDEN_APPLE` regen/absorption via
  `FoodComponent`.
- Three medicine types: `cure`, `effect_clear`, `buff`.
- Daily-limit enforcement; overdose applies `POISON` and skips the cure.
- Vanilla milk-bucket "clear everything" behaviour disabled.

#### 🧪 Module 3 — Laboratory (`modules.laboratory`)

- Vanilla `ShapedRecipe`:
  ```
  I I I        I = IRON_INGOT
  F B C        F = FURNACE  B = BREWING_STAND  C = CAULDRON
  I I I
  ```
- Visual layer decoupled from logic via `laboratory.block_material` —
  CraftEngine packs can swap the host block without changing code.
- 54-slot GUI with input, preview, craft button, output area and fuel slot.
- **Queue system** — every craft-button click consumes one batch and adds
  one item to the queue; queues keep running while the GUI is closed.
- Ingredient grammar supports `vanilla:`, `custom:` (PDC-matched) and
  either-of syntax: `vanilla:coal|vanilla:charcoal:1`.
- Lab state (inventory, progress, queue, fuel) is persisted in
  `laboratories.yml`.

### Installation

1. Download `immersive-health-system-1.0.0.jar`.
2. Drop it into your Paper 1.21.8 `plugins/` folder.
3. Start the server — `plugins/ImmersiveHealthSystem/` will be populated
   with config and locale files.
4. (Optional) switch `language: ru` to `en` in `config.yml`.

### Configuration

| File | Purpose |
| --- | --- |
| `config.yml`        | Global toggles and module switches. |
| `infections.yml`    | Disease catalogue. |
| `medicines.yml`     | Medicine catalogue. |
| `lab_recipes.yml`   | Laboratory recipes and craft times. |
| `lang/messages_ru.yml` | Russian locale bundle. |
| `lang/messages_en.yml` | English locale bundle. |

### Commands

| Command | Description |
| --- | --- |
| `/ihs check <player> [scale\|infection]` | Show scale or active infections. |
| `/ihs inject <player> <infection_id> [chance]` | Forcefully infect with optional chance. |
| `/ihs heal <player> [infection_id]` | Cure entirely or by infection id. |
| `/ihs modify <player> <value>` | Set exact disease-scale value. |
| `/ihs menu` | Open the players-overview GUI. |
| `/ihs reload` | Reload YAML files and re-parse catalogues. |
| `/spit` | Spit at your target — elevated infection chance. |

### Localization

The plugin ships with two languages: **Russian** (default) and **English**.
Active language is selected in `config.yml`:

```yaml
# Plugin language: ru / en. Falls back to en if missing.
language: ru
```

Locale files live in `plugins/ImmersiveHealthSystem/lang/`. Edit them freely or
drop in your own `messages_<code>.yml` — the plugin will load it whenever
`language` matches the code. Missing keys fall back to English automatically.

### Extending (adding modules)

1. Implement `com.tnhzr.ihs.module.Module` (`id`, `enable`, `disable`).
2. Register it in `ImmersiveHealthSystem#onEnable` via
   `moduleManager.register(id, module)`.
3. Add a `modules.<id>: true|false` toggle to `config.yml`.

The bootstrap respects the toggle and never lets one failing module take
down the rest of the plugin.

### Building

```bash
mvn -DskipTests package
```

The output jar lands at `target/immersive-health-system-1.0.0.jar`.
