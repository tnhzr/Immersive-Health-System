# Resource Pack

IHS bundles its own resourcepack (laboratory textures, medicine models,
custom symptom sounds). This page explains how the plugin delivers it
to clients.

## Selecting an installer

Open `config.yml`:

```yaml
resourcepack:
  installer: auto
```

`installer` picks the strategy:

| Value         | Target folder                                                                | Notes                                              |
|---------------|------------------------------------------------------------------------------|----------------------------------------------------|
| `auto`        | first available host plugin                                                  | CraftEngine ➜ ItemsAdder ➜ Nexo ➜ Oraxen ➜ manual  |
| `craftengine` | `plugins/CraftEngine/<target_folder>/`                                       | default behaviour from 1.0.x                       |
| `itemsadder`  | `plugins/ItemsAdder/contents/<namespace>/resourcepack/`                      | run `/iazip` after first start                     |
| `nexo`        | `plugins/Nexo/pack/external_packs/<folder>/`                                 | reload Nexo or restart                             |
| `oraxen`      | `plugins/Oraxen/pack/external_packs/<folder>/`                               | reload Oraxen or restart                           |
| `manual`      | `plugins/ImmersiveHealthSystem/resourcepack/`                                | zip + serve via your own HTTP / launcher           |
| `none`        | _(skipped)_                                                                  | use this if you don't want any auto-injection      |

Auto-detection walks the list in order and picks the first plugin that
is loaded on the server. If none of the supported hosts are present,
the `manual` fallback always runs.

## Per-installer options

```yaml
resourcepack:
  craftengine:
    target_folder: "resources/immersive_health"
  itemsadder:
    namespace: "ihs"
  nexo:
    folder: "ihs"
  oraxen:
    folder: "ihs"
```

`force_overwrite: true` (default) wipes the target on every server
start. Flip to `false` if you intend to hand-edit any files in the
target directory.

## Manual delivery

When `installer: manual`, IHS unpacks the bundled pack into
`plugins/ImmersiveHealthSystem/resourcepack/` on each start. From there
you can:

1. Zip the folder.
2. Upload it to a public HTTPS endpoint (or your existing CDN).
3. Configure your `server.properties` (`resource-pack=`,
   `resource-pack-sha1=`) or use a server-side pack manager.

If clients receive the pack via a launcher / modpack and the
`PlayerResourcePackStatusEvent` is never fired, set
`symptoms.yml -> resource_pack_detection.assume_loaded: true` so the
plugin treats every player as having the pack and uses the custom
`ihs:cough` / `ihs:sneeze` sound keys.

## Pack contents

```
pack/resourcepack/
├── pack.mcmeta
├── pack.png
└── assets/
    ├── ihs/
    │   ├── items/         # 1.21.4+ item-definition layer
    │   ├── models/        # block + item models
    │   ├── textures/      # block + item textures
    │   └── sounds/        # cough.ogg, sneeze.ogg, lab.* (if shipped)
    └── minecraft/
        ├── blockstates/   # note_block override (laboratory rotation)
        └── sounds.json    # sound key registrations
```

When the laboratory is placed, IHS overrides the `note_block` blockstate
and uses the `note=0..3` slots to encode 4-way rotation. Make sure your
custom blockstates file does not collide with this override unless you
explicitly want to.
