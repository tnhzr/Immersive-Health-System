# Admin Guide

## Install

1. Drop `immersive-health-system-<version>.jar` into your `plugins/`
   folder.
2. Start the server. IHS will create
   `plugins/ImmersiveHealthSystem/` with all the YAML configs.
3. (Optional) install one of the supported resourcepack hosts:
   **CraftEngine**, **ItemsAdder**, **Nexo** or **Oraxen** — the
   included resourcepack will be auto-injected. See
   [Resource-Pack.md](Resource-Pack.md).
4. Adjust `config.yml`, restart (or `/ihs reload`).

## Commands

| Command                                      | Permission           | Description                              |
|----------------------------------------------|----------------------|------------------------------------------|
| `/ihs check <player> [scale\|infection]`     | `ihs.admin`          | Show scale or active infections          |
| `/ihs inject <player> <id> [chance]`         | `ihs.admin`          | Force-infect with optional chance roll   |
| `/ihs heal <player> [id]`                    | `ihs.admin`          | Cure all or one specific infection       |
| `/ihs modify <player> <value>`               | `ihs.admin`          | Set the disease scale                    |
| `/ihs give <player> <medicine_id> [count]`   | `ihs.give`           | Hand a medicine                          |
| `/ihs tremor <player> [seconds]`             | `ihs.admin`          | Force-trigger the cosmetic tremor        |
| `/ihs rpstatus [player]`                     | `ihs.admin`          | Debug the resourcepack tracker           |
| `/ihs menu`                                  | `ihs.menu`           | Open the players overview GUI            |
| `/ihs reload`                                | `ihs.admin`          | Reload all YAMLs                         |
| `/spit`                                      | `ihs.spit` (default) | Spit at the player you're looking at     |
| `/cough`, `/sneeze`, `/vomit`                | `ihs.debug.symptom`  | Manually trigger a symptom (debug)       |

## Permissions

| Permission           | Default | Effect                                          |
|----------------------|---------|-------------------------------------------------|
| `ihs.admin`          | op      | All `/ihs *` admin sub-commands                 |
| `ihs.menu`           | op      | Open the player health menu                     |
| `ihs.give`           | op      | `/ihs give`                                     |
| `ihs.spit`           | true    | `/spit`                                         |
| `ihs.debug.symptom`  | op      | `/cough`, `/sneeze`, `/vomit`                   |
| `ihs.admin.testgrab` | op      | Show the "test grab" button in the recipe GUI   |

## Files

| File                     | Purpose                                                       |
|--------------------------|---------------------------------------------------------------|
| `config.yml`             | Global toggles, laboratory block config, resourcepack wiring  |
| `infections.yml`         | Disease catalogue (transmission, stages, **death_message**)   |
| `medicines.yml`          | Medicine catalogue (incl. tranquilizer config)                |
| `lab_recipes.yml`        | Laboratory recipes and craft times                            |
| `symptoms.yml`           | Particles, sounds and resourcepack-detection knobs            |
| `lang/messages_ru.yml`   | Russian locale bundle                                         |
| `lang/messages_en.yml`   | English locale bundle                                         |

## Death messages

Each disease in `infections.yml` may declare its own broadcast message:

```yaml
diseases:
  tuberculosis:
    name: "Туберкулёз"
    death_message: "&7%player% умер от туберкулёза"
    ...
```

When a player dies and IHS attributes the kill to a disease, the death
message replaces the vanilla one and is sent through the normal Bukkit
death broadcast (so chat plugins see it as a regular death).

## Resource-pack-aware sound swap

`symptoms.yml`:

```yaml
resource_pack_detection:
  enabled: true
  grace_period_ms: 3000
  assume_loaded: false
```

- `enabled: true` — listen to `PlayerResourcePackStatusEvent` and remember
  which players have the IHS pack loaded.
- `grace_period_ms` — how long after `ACCEPTED` to wait for
  `SUCCESSFULLY_LOADED` before assuming the pack is loaded.
- `assume_loaded: true` — set this if you ship the pack out of band
  (modpack, launcher, separate installer) and the regular RP events
  never fire. With this flag every player is treated as having the pack.

Use `/ihs rpstatus <player>` to inspect the live tracker state for a
specific player.

## Tranquilizer config (`medicines.yml`)

```yaml
medicines:
  sleeping_pills:
    material: SUGAR
    name: "&9Снотворное"
    medicine_data:
      type: tranquilizer
      sleep_seconds: 30        # forced sleep duration
      onset_seconds: 5         # blindness phase before sleep
      reveal_in_lore: false    # show "[laced]" tag on poisoned food
```

- Drag-drop on food → mark food as laced.
- Drag-drop on arrows → coat the stack.
- Direct consumption → blindness then sleep.
- Arrow hit → players sleep, mobs hibernate (`AI off`, heavy slowness).

## Laboratory block config (`config.yml`)

```yaml
laboratory:
  block:
    display_name: "&fЛаборатория"
    lore:
      - "&7Кастомный крафтовый блок"
    break_tool_tier: STONE       # HAND/WOOD/STONE/IRON/DIAMOND/NETHERITE
    synthesis_tick_period: 20    # ambient sound cadence in ticks
    sounds:
      place:     "ihs:lab.place"
      break:     "ihs:lab.break"
      interact:  "ihs:lab.interact"
      close:     "ihs:lab.close"
      synthesis: "ihs:lab.synthesis"
      denied:    { key: "minecraft:block.anvil.land", volume: 0.3, pitch: 1.4 }
```

Each `sounds.*` entry accepts either a bare string (the sound key) or
an object with `{ key, volume, pitch }`. The block's facing rotates
automatically based on where the player stands when placing.
