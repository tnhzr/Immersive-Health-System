# Compatibility Matrix

> Tested on PaperMC 1.21.8 (the canonical target). Other versions are
> supported on a best-effort basis — see notes per row.

| Server software | Version range  | Status        | Notes                                                                                       |
|-----------------|----------------|---------------|---------------------------------------------------------------------------------------------|
| **Paper**       | 1.21.8         | ✅ Primary    | Reference platform. Fully tested.                                                          |
| **Paper**       | 1.21.4 – 1.21.7| ✅ Supported  | All features work. Item models route through 1.21.4 item-definition layer.                |
| **Paper**       | 1.21.0 – 1.21.3| ⚠️ Best-effort | `setItemModel` is missing — items render via the legacy `customModelData` path. |
| **Paper**       | 1.20.x         | ⚠️ Best-effort | `Particle.SNEEZE`/`SPIT` exist; `Player.sleep(loc, true)` exists. Lab/medicine work; custom item-models fall back to vanilla appearance unless you ship `customModelData` overrides. |
| **Paper**       | < 1.20         | ❌ Unsupported | Several APIs the plugin relies on do not exist.                                            |
| **Spigot**      | any            | ❌ Unsupported | The plugin uses Adventure API (Components / MiniMessage), which Spigot does not bundle.    |
| **Folia**       | any            | ❌ Untested    | Region-threading model not vetted. Forks welcome.                                          |

## Java

- **Required:** Java 21.
- The build target is set to `--release 21` in `pom.xml`. Earlier JREs
  will fail to load the jar.

## Companion plugins (resourcepack hosts)

| Plugin         | Tested versions          | Notes                                                                                       |
|----------------|--------------------------|---------------------------------------------------------------------------------------------|
| **CraftEngine**| 1.x                      | Primary host. Default for `installer: auto` when present.                                  |
| **ItemsAdder** | 3.x                      | Pack lands in `plugins/ItemsAdder/contents/<ns>/resourcepack/`. `/iazip` after first start. |
| **Nexo**       | 1.x                      | Pack lands in `plugins/Nexo/pack/external_packs/<folder>/`. Reload Nexo to pick up.        |
| **Oraxen**     | 1.x                      | Pack lands in `plugins/Oraxen/pack/external_packs/<folder>/`. Reload Oraxen to pick up.    |
| _none_         | —                        | `installer: manual` always works — pack is staged in the IHS data folder.                  |

## Client (Minecraft Java Edition)

- **1.21.x** — full visuals (custom models, custom sounds).
- **1.20.x** — vanilla materials/sounds; the plugin still functions but
  custom textures may render as fallback variants depending on the
  installed pack host.
- Mods that aggressively rewrite the freezing overlay or sleep flow may
  visually conflict with the tremor / tranquilizer mechanics. The
  underlying plugin behaviour is unaffected.
