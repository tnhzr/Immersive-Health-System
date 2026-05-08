# Developer API

IHS exposes a stable, semver-controlled API at
`com.tnhzr.ihs.api.*`. Forks and third-party plugins should depend on
this package and **not** on classes under `com.tnhzr.ihs.internal.*`.

## Add IHS as a dependency

### Maven

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.tnhzr</groupId>
    <artifactId>Immersive-Health-System</artifactId>
    <version>v1.1.0</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

> Replace the version tag with the release you want to pin against.

### plugin.yml

Mark IHS as a soft- or hard-dependency:

```yaml
softdepend: [ImmersiveHealthSystem]
```

## Acquiring the API

```java
import com.tnhzr.ihs.api.IHSApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<IHSApi> rsp =
        Bukkit.getServicesManager().getRegistration(IHSApi.class);
if (rsp == null) {
    getLogger().warning("Immersive Health System is not loaded.");
    return;
}
IHSApi ihs = rsp.getProvider();
```

The service is registered during the IHS `onEnable`, so make sure your
plugin's `loadbefore` / `softdepend` order is correct.

## Services

### `DiseaseService`

```java
ihs.diseases().infect(player, "tuberculosis", 1.0);   // 100% chance
ihs.diseases().heal(player, "tuberculosis", 25);      // -25 scale
ihs.diseases().healAll(player);                       // wipe all
int scale = ihs.diseases().scale(player, "tuberculosis");
Set<String> active = ihs.diseases().activeInfections(player);
```

### `MedicineService`

```java
ItemStack stack = ihs.medicines().itemOf("aspirin", 4);
ihs.medicines().give(player, "aspirin", 1);

if (ihs.medicines().isMedicine(stack)) {
    String id = ihs.medicines().idOf(stack);
}

ihs.medicines().applyTranquilizerSleep(player, 5, 30); // 5s onset, 30s sleep
```

### `LaboratoryService`

```java
ItemStack labBlock = ihs.laboratories().labItem();
boolean here = ihs.laboratories().isLaboratoryAt(player.getLocation());
ihs.laboratories().openRecipeBrowser(player);

Set<String> recipes = ihs.laboratories().knownRecipes();
String result = ihs.laboratories().recipeResult("painkiller_recipe");
```

### `ResourcePackService`

```java
String installer = ihs.resourcePack().activeInstaller();
// "craftengine", "itemsadder", "nexo", "oraxen", "manual" or "none"

if (ihs.resourcePack().hasPack(player)) {
    player.playSound(player.getLocation(), "ihs:cough", 1f, 1f);
} else {
    player.playSound(player.getLocation(), "minecraft:entity.fox.spit", 1f, 1f);
}

ihs.resourcePack().markLoaded(player); // out-of-band pack delivery
```

## Bukkit events

All events live in `com.tnhzr.ihs.api.event`. They follow the standard
Bukkit pattern (`HandlerList`, `EventHandler`).

| Event                              | When fired                                            | Cancellable |
|------------------------------------|-------------------------------------------------------|-------------|
| `IHSPlayerInfectedEvent`           | before a new infection is applied                     | yes         |
| `IHSPlayerCuredEvent`              | after a player's infection scale dropped to 0         | no          |
| `IHSPlayerSleptEvent`              | when a tranquilizer routine knocks a player out       | no          |
| `IHSLabSynthesisCompletedEvent`    | when a laboratory finishes producing one batch        | no          |

```java
import com.tnhzr.ihs.api.event.IHSPlayerInfectedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class CovidImmunity implements Listener {
    @EventHandler
    public void onInfect(IHSPlayerInfectedEvent e) {
        if (e.getPlayer().hasPermission("plague.immune")) {
            e.setCancelled(true);
        }
    }
}
```

## Stability contract

- Patch / minor versions add methods but never break signatures.
- Major versions may break compatibility — see the corresponding GitHub
  release notes for the migration path.
- `com.tnhzr.ihs.api.internal.*` is implementation detail. Do not import
  it from a downstream plugin — it can change at any time.
