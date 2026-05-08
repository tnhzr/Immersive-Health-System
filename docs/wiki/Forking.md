# Forking IHS

This page is for people who want to ship their own version of the
plugin (renamed package, custom assets, removed/added features).
Upstream is closed but the codebase is MIT-licensed — fork freely.

## 1. Bootstrap

```bash
git clone https://github.com/tnhzr/Immersive-Health-System.git my-fork
cd my-fork
git remote rename origin upstream
```

## 2. Rename

### `pom.xml`

```xml
<groupId>com.example.fork</groupId>
<artifactId>my-immersive-health</artifactId>
<version>2.0.0-SNAPSHOT</version>
```

### `src/main/resources/plugin.yml`

```yaml
name: MyImmersiveHealth
version: ${project.version}
main: com.example.fork.MyImmersiveHealth
api-version: '1.20'
author: ExampleAuthor
```

### Java packages

If you also want to rename the Java package (recommended to avoid PDC
collisions with upstream):

1. Move `src/main/java/com/tnhzr/ihs/**` to `src/main/java/com/example/fork/**`.
2. `find src -name '*.java' -exec sed -i 's|com.tnhzr.ihs|com.example.fork|g' {} +`
3. Update `plugin.yml -> main`.
4. Rename `ImmersiveHealthSystem` (the JavaPlugin class) to your liking.

## 3. PDC keys

All `NamespacedKey`s the plugin stores are derived from
`plugin.getName()`. Renaming the plugin is enough — old upstream keys
will simply be unreachable on a fresh install.

If you need to **read** existing IHS data on an existing world, keep the
plugin name as `ImmersiveHealthSystem` or write a one-off migration that
copies the data over.

## 4. Resourcepack

The bundled resourcepack lives at
`src/main/resources/pack/resourcepack/`. The pack installer registry
copies everything under that prefix into the host plugin's pack folder
(or into `plugins/<your-plugin>/resourcepack/` for the manual fallback).

You can:

- Replace any texture / model / sound under that path.
- Add new namespaces — anything you put under
  `pack/resourcepack/assets/<your_namespace>/...` ships as part of the
  pack.
- Drop the resourcepack entirely by setting
  `resourcepack.installer: none` in the default config.

## 5. Public API stability

Forks should:

- **Re-export** the existing `com.tnhzr.ihs.api.*` interfaces (or
  re-package them under your new namespace) so downstream plugins that
  used IHS keep working.
- **Avoid** breaking the `IHSApi` contract within a major version.

If you want to add new services, extend `IHSApi` with new getter
methods — never replace existing ones.

## 6. Releases

Suggested release naming when forking:

```
my-immersive-health-<upstream-version>+fork.<your-version>
```

This makes it obvious which upstream baseline your fork tracks and how
many fork-side iterations have happened on top.
