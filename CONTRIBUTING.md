# Contributing to Immersive Health System

> **Note.** This project is officially **closed** for upstream development.
> Pull requests are not actively reviewed. The guide below is meant for
> people who want to **fork** the plugin and ship their own version.

## Building

Requirements:

- JDK 21
- Maven 3.9+

```bash
mvn -DskipTests package
```

The shaded jar lands at `target/immersive-health-system-<version>.jar`.

## Project layout

```
src/main/java/com/tnhzr/ihs/
├── ImmersiveHealthSystem.java     # JavaPlugin entry point
├── api/                           # PUBLIC API (stable, semver)
│   ├── IHSApi.java                # service umbrella
│   ├── DiseaseService.java
│   ├── MedicineService.java
│   ├── LaboratoryService.java
│   ├── ResourcePackService.java
│   ├── event/                     # Bukkit events fired by IHS
│   └── internal/                  # API impls — DO NOT import from forks
├── disease/                       # disease scale + symptoms
├── medicine/                      # medicine catalogue, tranquilizer
├── lab/                           # laboratory block + GUIs
├── pack/                          # resourcepack installers (CE/IA/Nexo/Oraxen/Manual)
└── ...
```

When forking:

- Anything under `com.tnhzr.ihs.api.*` (excluding `internal`) is **stable**
  and follows semver. Treat it as the contract for your own extensions.
- Anything under `com.tnhzr.ihs.api.internal.*` is implementation detail
  and may change between minor versions.
- Resources live in `src/main/resources/`. The bundled resourcepack lives
  at `src/main/resources/pack/resourcepack/`.

## Forking checklist

1. Update `pom.xml`:
   - `groupId` / `artifactId` to your namespace.
   - `version` — bump independently from upstream.
2. Update `src/main/resources/plugin.yml`:
   - `name` — must be unique on the server.
   - `main` — package path of your renamed entrypoint class.
   - `author` — your name.
3. If you rename the Java package:
   - Move `com/tnhzr/ihs/**` to your package path.
   - Update every `import com.tnhzr.ihs.*`.
   - Update `plugin.yml -> main` to match.
4. (Optional) edit the PDC namespace by changing `plugin.getName()`. All
   keys are derived from the plugin's name automatically.
5. Decide whether to keep IHS resourcepack assets or to ship your own —
   the registry only cares about the `pack/resourcepack/` jar prefix.

## Coding conventions

- Java 21 syntax allowed (records, switch expressions, pattern matching).
- Avoid `org.bukkit.craftbukkit.*` and NMS imports — keep the plugin
  cross-server.
- Wrap version-fragile API calls (`setItemModel`, `Player.sleep(loc, true)`,
  etc) in `try/catch` and document the supported versions on the
  surrounding javadoc.
- Public methods on `com.tnhzr.ihs.api.*` must keep their signature stable
  inside a major version. Add new methods rather than changing existing ones.

## Testing

There is no automated test harness in this fork. Manual testing on
PaperMC 1.21.x is the baseline. See [`docs/wiki/Compatibility.md`](docs/wiki/Compatibility.md)
for the supported version matrix.
