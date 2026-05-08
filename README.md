<div align="center">
  <img src=".github/assets/ihs_icon.png" alt="Immersive Health System" width="180" />

  # Immersive Health System

  **Иммерсивная система здоровья, болезней и лекарств для Paper 1.21.8.**
  *Immersive health, disease and laboratory plugin for Paper 1.21.8.*

  ![Paper](https://img.shields.io/badge/Paper-1.21.8-2b6cb0?style=flat-square)
  ![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square)
  ![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
  ![Languages](https://img.shields.io/badge/locale-RU%20%7C%20EN-orange?style=flat-square)
  ![Status](https://img.shields.io/badge/status-closed-red?style=flat-square)
</div>

> **🛑 Проект закрыт. Это финальный релиз — обновлений и поддержки не будет.**
> *Project is closed. This is the final release — no further updates or support.*

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

---

## Русский

### Описание

**Immersive Health System** — модульный плагин для Paper 1.21.8, реализующий
иммерсивную систему болезней, заражений, лекарств и собственного крафтового
блока «Лаборатория». Каждый модуль независим и включается/выключается в
`config.yml`. Все числовые значения, шансы, сообщения, звуки и тайминги
вынесены в конфиги — в коде нет «магических чисел».

> Плагин действует только на игроков. Мобы и прочие сущности не заражаются
> и не реагируют на болезни (за исключением транквилизатора — стрелы с ним
> усыпляют любую `LivingEntity`).

### Возможности

#### 🩺 Модуль 1 — Болезни (`modules.disease`)

- Шкала болезни 0 – 100 у каждого игрока с настраиваемыми диапазонами
  стадий (инкубация → лёгкие → тяжёлые → агония → смерть).
- Суточный прирост шкалы с модификаторами от низкого HP / голода.
- Полный сброс при смерти (опционально).
- Два типа заражения: `global` (случайный суточный ролл) и `local`
  (только контакт / частицы).
- Симптомы-ивенты `cough`, `sneeze`, `spit`, `vomit`, `cough_blood`
  со своими звуками и частицами; прямое попадание частицы в игрока
  даёт повышенный шанс заражения, в радиусе — базовый.
- **Тремор** (визуальный эффект «трясущихся рук»): при шкале выше порога
  игроку периодически отображается короткий visual-shake. Эффект
  **полностью косметический** — никакого замедления, никакого damage,
  никакого freezing-overlay.
- **Кастомные сообщения о смерти.** В `infections.yml` для каждой болезни
  можно задать собственное `death_message: "%player% умер от туберкулёза"`,
  оно отправляется всему серверу как обычный death-broadcast.
- Команда `/spit` — игрок может намеренно плюнуть в цель.
- Конфигурируемые экшены смертельной стадии (`kill`, `spawn_zombie`).

#### 💊 Модуль 2 — Лекарства (`modules.medicine`)

- Кастомные предметы с PDC-тегом (`ihs:medicine_id`) — подделать нельзя.
- Полный отказ от ванильных эффектов GOLDEN_APPLE через FoodComponent.
- Четыре типа лекарств:
  - `cure` — понижает шкалу и отменяет суточный рост на этот день;
  - `effect_clear` — снимает выбранные `PotionEffect`'ы;
  - `buff` — накладывает выбранные эффекты на заданное время;
  - `tranquilizer` — усыпляет цель (см. ниже).
- Дневной лимит приёма; при передозировке — `POISON` вместо лечения.
- Отключён ванильный эффект «молочное ведро снимает всё».
- **Транквилизатор.** Особый тип лекарства со встроенными механиками:
  - Употреблённый напрямую — даёт `BLINDNESS` на `onset_seconds`,
    после чего принудительно укладывает игрока спать (`Player.sleep`)
    в любом месте и в любое время на `sleep_seconds` секунд.
  - **Подмешивание в еду** — drag&drop одного транквилизатора на любой
    съедобный предмет (хоть ванильный, хоть кастомный) помечает его
    PDC-тегом. Когда жертва ест помеченную еду, срабатывает та же
    последовательность сна. Опционально (`reveal_in_lore`) можно
    показывать факт подмешивания в lore-описании.
  - **Покрытие стрел** — drag&drop транквилизатора на `ARROW`,
    `TIPPED_ARROW` или `SPECTRAL_ARROW` помечает стрелу. При попадании
    стрелы в игрока тот засыпает; в моба — moб «впадает в спячку»
    (AI отключается на длительность сна, накладывается тяжёлый
    `SLOWNESS`/`BLINDNESS`).
  - Длительность регулируется в `medicines.yml`:
    `medicine_data: { sleep_seconds: 30, onset_seconds: 5, reveal_in_lore: false }`.
- **`sleeping_pills`** — каноничное лекарство-транквилизатор в комплекте,
  с собственной текстурой и моделью.

#### 🧪 Модуль 3 — Лаборатория (`modules.laboratory`)

- Крафтится ванильным `ShapedRecipe`:
  ```
  I I I        I = IRON_INGOT
  F B C        F = FURNACE  B = BREWING_STAND  C = CAULDRON
  I I I
  ```
- **Поворот блока.** При установке лаборатория автоматически поворачивается
  лицевой стороной от игрока (4 стороны света). Реализовано через
  override блокстейта `note_block[note=0..3]` — каждое значение `note`
  соответствует одной из четырёх ориентаций.
- **Тиер инструмента.** В `laboratory.block.break_tool_tier` задаётся
  минимальная кирка для слома (`HAND` / `WOOD` / `STONE` / `IRON` /
  `DIAMOND` / `NETHERITE`, дефолт `STONE`). Без подходящей кирки блок
  не ломается, играется звук `denied`.
- **Без звука нотного блока.** ЛКМ по лаборатории не воспроизводит
  ванильный harp-note — кликовый событие гасится на `HIGHEST` приоритете.
- **Полная настройка блока** в `laboratory.block.*`:
  - `display_name`, `lore` — отображение в инвентаре и при наведении;
  - `sounds.{place,break,interact,close,synthesis,denied}` — каждый звук
    задаётся либо строкой-ключом (`ihs:lab.place`), либо объектом с
    `key/volume/pitch`;
  - `synthesis_tick_period` — интервал в тиках для амбиентного звука
    активного синтеза (по дефолту 20 = раз в секунду).
- 54-слотовый GUI: ввод, превью результата, кнопка «Начать»,
  область вывода, слот топлива (row 4 col 5 — slot 41).
- **Очередь крафта** — каждый клик по кнопке снимает 1 порцию ингредиентов
  и ставит +1 в очередь. Очередь продолжает работать при закрытом GUI.
- Поддержка ингредиентов `vanilla:`, `custom:` (по PDC) и
  «или-или»: `vanilla:coal|vanilla:charcoal:1`.
- **Recipe Detail GUI** — двойная страница с рецептом и результатом.
  Для администраторов с пермишеном `ihs.admin.testgrab` в углу появляется
  кнопка «Тестовая выдача» 🟢 — клик выдаёт результат рецепта без
  расхода ингредиентов (для тестов и тонкой настройки рецептов).
- Состояние лабораторий (инвентарь, прогресс, очередь, топливо)
  сохраняется в `laboratories.yml`.

#### 🔊 Звуки симптомов (`symptoms.yml`)

- Каждое событие (`cough`, `sneeze`, `spit`, `cough_blood`, `vomit`)
  имеет собственный блок настроек: `particle.*` (тип, count, offset,
  speed, конусная эмиссия, цвет dust) и `sound.{vanilla, custom, volume,
  pitch}`.
- **Resource-Pack-aware подмена звука.** Если у игрока загружен IHS-пак,
  плагин выдаёт ему `ihs:cough` / `ihs:sneeze` (кастомные `.ogg`).
  Если пак не загружен — ваниль (`minecraft:entity.fox.spit` и т.д.)
  без warning-спама в логах. Контроль:
  - `resource_pack_detection.enabled: true` — основной рубильник.
  - `resource_pack_detection.grace_period_ms: 3000` — сколько ждать
    `SUCCESSFULLY_LOADED` после `ACCEPTED`.
  - `resource_pack_detection.assume_loaded: false` — поставь `true`,
    если ты раздаёшь пак вне обычного RP-механизма (модпак / launcher /
    отдельная установка) — плагин будет считать что у каждого
    игрока пак уже есть.
- Дебаг: `/ihs rpstatus <player>` показывает статус трекера для
  конкретного игрока.

### Установка

1. Скачайте `immersive-health-system-1.0.0.jar`.
2. Положите в папку `plugins/` сервера Paper 1.21.8.
3. Запустите сервер — плагин создаст `plugins/ImmersiveHealthSystem/` со всеми
   конфигами и языковыми файлами.
4. (По желанию) измените `language: ru` на `en` в `config.yml`.

### Конфигурация

| Файл | Описание |
| --- | --- |
| `config.yml`        | Глобальные настройки, переключатели модулей, конфиг блока лаборатории. |
| `infections.yml`    | Каталог болезней (название, тип, стадии, передача, **death_message**). |
| `medicines.yml`     | Каталог лекарств (PDC, тип, лимиты, эффекты, **транквилизатор**). |
| `lab_recipes.yml`   | Рецепты Лаборатории и их время крафта. |
| `symptoms.yml`      | Звуки и партиклы симптомов + RP-detection. |
| `lang/messages_ru.yml` | Русская локализация. |
| `lang/messages_en.yml` | Английская локализация. |

### Команды

| Команда | Описание |
| --- | --- |
| `/ihs check <игрок> [scale\|infection]` | Показать шкалу или активные болезни. |
| `/ihs inject <игрок> <id_болезни> [шанс]` | Принудительно заразить с заданным шансом. |
| `/ihs heal <игрок> [id_болезни]` | Полностью или точечно вылечить игрока. |
| `/ihs modify <игрок> <значение>` | Установить точное значение шкалы. |
| `/ihs give <игрок> <id_лекарства> [count]` | Выдать лекарство в инвентарь. |
| `/ihs tremor <игрок> [секунды]` | Включить визуальный тремор для теста. |
| `/ihs rpstatus [игрок]` | Дебаг-инфо по resource-pack-трекеру. |
| `/ihs menu` | GUI со всеми онлайн-игроками. |
| `/ihs reload` | Перезагрузить YAML-конфиги и каталоги. |
| `/spit` | Намеренный плевок (повышенный шанс заражения цели). |
| `/cough`, `/sneeze`, `/vomit` | Дебаг-спавн соответствующего симптома. |

### Пермишены

| Пермишен | Дефолт | Что даёт |
| --- | --- | --- |
| `ihs.admin` | op | Все админ-подкоманды `/ihs *`. |
| `ihs.menu`  | op | Открыть GUI здоровья. |
| `ihs.give`  | op | `/ihs give`. |
| `ihs.spit`  | true | `/spit`. |
| `ihs.debug.symptom` | op | `/cough`, `/sneeze`, `/vomit`. |
| `ihs.admin.testgrab` | op | Кнопка «Тестовая выдача» в Recipe GUI. |

### Локализация

Плагин поставляется с двумя языками: **русский** (по умолчанию) и **английский**.
Активный язык выбирается в `config.yml`:

```yaml
language: ru
```

Файлы локализации лежат в `plugins/ImmersiveHealthSystem/lang/`.
Можно свободно править существующие или добавить свой `messages_<код>.yml` —
плагин подхватит его, если в `language` указать соответствующий код.
Если ключ отсутствует в выбранном языке, используется английский fallback.

### Расширение (добавление модулей)

1. Реализуй `com.tnhzr.ihs.module.Module` (методы `id`, `enable`, `disable`).
2. Зарегистрируй модуль в `ImmersiveHealthSystem#onEnable` через
   `moduleManager.register(id, module)`.
3. Добавь переключатель `modules.<id>: true|false` в `config.yml`.

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
> ignored, except for the tranquilizer — a coated arrow puts any
> `LivingEntity` to sleep.

### Features

#### 🩺 Module 1 — Disease System (`modules.disease`)

- Per-player disease scale 0 – 100 with configurable stage ranges
  (incubation → light → heavy → agony → death).
- Daily growth with low-health / hunger modifiers.
- Optional full reset on death.
- Two infection types: `global` (random daily roll) and `local`
  (contact / particles only).
- Symptom events: `cough`, `sneeze`, `spit`, `vomit`, `cough_blood` —
  each with its own sound and particles; particles physically hitting
  a player apply an elevated chance, otherwise the radius chance applies.
- **Tremor** (visual "shaking hands" effect): when the disease scale
  exceeds the configured threshold, the player gets a periodic short
  visual shake. The effect is **purely cosmetic** — no slowdown, no
  damage, no freezing overlay.
- **Custom death messages.** Each disease in `infections.yml` can declare
  its own `death_message: "%player% died of tuberculosis"`, broadcast
  to the whole server like any vanilla death message.
- `/spit` command lets infected players intentionally infect their target.
- Configurable terminal-stage actions (`kill`, `spawn_zombie`).

#### 💊 Module 2 — Medicines (`modules.medicine`)

- Custom items with PDC tag (`ihs:medicine_id`) — cannot be forged.
- Completely bypasses vanilla `GOLDEN_APPLE` regen/absorption via
  `FoodComponent`.
- Four medicine types:
  - `cure` — lowers the scale and suppresses today's growth;
  - `effect_clear` — removes the listed `PotionEffect`s;
  - `buff` — applies the listed effects;
  - `tranquilizer` — sedates the target (see below).
- Daily-limit enforcement; overdose applies `POISON` and skips the cure.
- Vanilla milk-bucket "clear everything" behaviour disabled.
- **Tranquilizer.** A special medicine type with three mechanics:
  - Consumed directly — applies `BLINDNESS` for `onset_seconds`, then
    forces the player to sleep (`Player.sleep`) anywhere/anytime for
    `sleep_seconds` seconds.
  - **Food lacing** — drag&drop one tranquilizer onto any edible
    item (vanilla or custom) tags it with a PDC marker. Eating a laced
    food triggers the same sleep routine. Optional (`reveal_in_lore`)
    can disclose the lacing in the item lore.
  - **Arrow coating** — drag&drop a tranquilizer onto an `ARROW`,
    `TIPPED_ARROW` or `SPECTRAL_ARROW` tags it. On hit:
    players are forced to sleep; mobs go into hibernation
    (AI off + heavy `SLOWNESS`/`BLINDNESS`) for the duration.
  - Configurable in `medicines.yml`:
    `medicine_data: { sleep_seconds: 30, onset_seconds: 5, reveal_in_lore: false }`.
- **`sleeping_pills`** — bundled canonical tranquilizer with its own
  texture and model.

#### 🧪 Module 3 — Laboratory (`modules.laboratory`)

- Vanilla `ShapedRecipe`:
  ```
  I I I        I = IRON_INGOT
  F B C        F = FURNACE  B = BREWING_STAND  C = CAULDRON
  I I I
  ```
- **Block rotation.** On placement the laboratory automatically faces away
  from the player (4 cardinal directions). Implemented via a blockstate
  override on `note_block[note=0..3]` — each `note` value maps to one
  rotation.
- **Tool tier gating.** `laboratory.block.break_tool_tier` defines the
  minimum pickaxe to break the lab (`HAND` / `WOOD` / `STONE` / `IRON` /
  `DIAMOND` / `NETHERITE`, default `STONE`). Wrong tool: break is
  cancelled and the `denied` sound plays.
- **No note-block click sound.** Left-clicking the laboratory does not
  play the vanilla harp note — the click is suppressed at `HIGHEST`
  priority.
- **Fully configurable block** under `laboratory.block.*`:
  - `display_name`, `lore` — inventory tooltip;
  - `sounds.{place,break,interact,close,synthesis,denied}` — each entry
    is either a bare string key (`ihs:lab.place`) or an object with
    `key/volume/pitch`;
  - `synthesis_tick_period` — tick interval for the ambient synthesis
    sound (default 20 = once per second).
- 54-slot GUI with input, preview, craft button, output area and fuel
  slot (row 4 col 5 — slot 41).
- **Queue system** — every craft-button click consumes one batch and adds
  one item to the queue; queues keep running while the GUI is closed.
- Ingredient grammar supports `vanilla:`, `custom:` (PDC-matched) and
  either-of syntax: `vanilla:coal|vanilla:charcoal:1`.
- **Recipe Detail GUI** — double-page view with the recipe and the
  result. Admins with `ihs.admin.testgrab` get a corner button — one
  click hands them the result for free (recipe-tweaking aid).
- Lab state (inventory, progress, queue, fuel) is persisted in
  `laboratories.yml`.

#### 🔊 Symptom sounds (`symptoms.yml`)

- Each event (`cough`, `sneeze`, `spit`, `cough_blood`, `vomit`) has its
  own settings block: `particle.*` (type, count, offset, speed, cone
  emission, dust color) and `sound.{vanilla, custom, volume, pitch}`.
- **Resource-pack-aware sound swap.** If the player has the IHS pack
  loaded, the plugin plays them the `ihs:cough` / `ihs:sneeze` custom
  ogg. Without the pack — vanilla (`minecraft:entity.fox.spit` etc.),
  no warning spam. Controls:
  - `resource_pack_detection.enabled: true` — master switch.
  - `resource_pack_detection.grace_period_ms: 3000` — how long to wait
    for `SUCCESSFULLY_LOADED` after `ACCEPTED`.
  - `resource_pack_detection.assume_loaded: false` — set to `true` if
    you ship the pack out-of-band (modpack / launcher / separate install)
    so the plugin treats every player as having the pack.
- Debug: `/ihs rpstatus <player>` prints the tracker state for a player.

### Installation

1. Download `immersive-health-system-1.0.0.jar`.
2. Drop it into your Paper 1.21.8 `plugins/` folder.
3. Start the server — `plugins/ImmersiveHealthSystem/` will be populated
   with config and locale files.
4. (Optional) switch `language: ru` to `en` in `config.yml`.

### Configuration

| File | Purpose |
| --- | --- |
| `config.yml`        | Global toggles, module switches, **laboratory block** config. |
| `infections.yml`    | Disease catalogue (with **death_message**). |
| `medicines.yml`     | Medicine catalogue (with **tranquilizer** config). |
| `lab_recipes.yml`   | Laboratory recipes and craft times. |
| `symptoms.yml`      | Symptom particles + sounds + RP detection. |
| `lang/messages_ru.yml` | Russian locale bundle. |
| `lang/messages_en.yml` | English locale bundle. |

### Commands

| Command | Description |
| --- | --- |
| `/ihs check <player> [scale\|infection]` | Show scale or active infections. |
| `/ihs inject <player> <infection_id> [chance]` | Forcefully infect with optional chance. |
| `/ihs heal <player> [infection_id]` | Cure entirely or by infection id. |
| `/ihs modify <player> <value>` | Set exact disease-scale value. |
| `/ihs give <player> <medicine_id> [count]` | Hand out a medicine to a player. |
| `/ihs tremor <player> [seconds]` | Force-trigger the visual tremor. |
| `/ihs rpstatus [player]` | Debug the resource-pack tracker. |
| `/ihs menu` | Open the players-overview GUI. |
| `/ihs reload` | Reload YAML files and re-parse catalogues. |
| `/spit` | Spit at your target — elevated infection chance. |
| `/cough`, `/sneeze`, `/vomit` | Debug-spawn the matching symptom. |

### Permissions

| Permission | Default | Effect |
| --- | --- | --- |
| `ihs.admin` | op | All `/ihs *` admin sub-commands. |
| `ihs.menu`  | op | Open the health GUI. |
| `ihs.give`  | op | `/ihs give`. |
| `ihs.spit`  | true | `/spit`. |
| `ihs.debug.symptom` | op | `/cough`, `/sneeze`, `/vomit`. |
| `ihs.admin.testgrab` | op | "Test grab" button in the Recipe GUI. |

### Localization

The plugin ships with two languages: **Russian** (default) and **English**.
Active language is selected in `config.yml`:

```yaml
language: ru
```

Locale files live in `plugins/ImmersiveHealthSystem/lang/`. Edit them freely or
drop in your own `messages_<code>.yml` — the plugin will load it whenever
`language` matches the code. Missing keys fall back to English automatically.

### Extending (adding modules)

1. Implement `com.tnhzr.ihs.module.Module` (`id`, `enable`, `disable`).
2. Register it in `ImmersiveHealthSystem#onEnable` via
   `moduleManager.register(id, module)`.
3. Add a toggle `modules.<id>: true|false` to `config.yml`.

### Build

```bash
mvn -DskipTests package
```

Output jar — `target/immersive-health-system-1.0.0.jar`.

---

## License

MIT.
