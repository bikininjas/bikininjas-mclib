# Core Lib — Documentation de la Librairie

## Rôle

API générique réutilisable par tous les mods enfants (`mod-*`). Ne contient
**aucune feature concrète** : seulement des utilitaires, helpers, et managers
que les mods consomment via `api` dependency.

---

## Modules

### 1. Logging — `com.bikininjas.corelib.log`

Système de logs structuré prefixé par mod + classe.

- **`ModLogger`** : wrapper SLF4J. Auto-prefixe `[modId][ClassName]`.
  Fournit `info()`, `debug()`, `warn()`, `error()` + `ErrorBuilder` chaînable.
- **`ErrorBuilder`** : `.ctx(key, val)`, `.cause(exception)`, `.mod(overrideId)`.
  Appel terminal `.report()` pour emission formatée.
- **`LogManager`** : fabrique de `ModLogger`. Usage :
  ```java
  private static final ModLogger LOG = LogManager.getLogger("my_mod", MyClass.class);
  LOG.error("Failed to load")
      .ctx("file", path)
      .cause(e)
      .report();
  ```
- **NOOP sentinel** : si le niveau error est désactivé, `error()` retourne un
  `ErrorBuilder.NOOP` dont toutes les méthodes sont des no-op (pas de formatage).

### 2. Compteurs & Objectifs — `com.bikininjas.corelib.objective`

- **`ObjectiveTracker`** : singleton thread-safe avec `ConcurrentHashMap`.
  API : `get(player, key)`, `set(player, key, count)`, `add(player, key, delta)`,
  `resetAll(player)`, `getAll(player)` → `Map<String, Integer>`.

### 3. Gestionnaire d'Événements Aléatoires — `com.bikininjas.corelib.randomevent`

- **`RandomEventManager`** : singleton. Planifie et exécute des événements
  aléatoires côté serveur avec intervalle configurable et sélection pondérée.
- **`RandomEvent`** : interface fonctionnelle `execute(ServerLevel, Vec3)`.
- API : `register(RandomEvent)`, `register(event, name)`, `remove(name)`,
  `fireRandomEvent(level)`, `fireEvent(name, level, origin)`.

### 4. Temps / Scheduler — `com.bikininjas.corelib.time`

- **`TimeManager`** : gestion de temps Minecraft (cycle jour/nuit, ticks).
  Fournit des utilitaires pour les mods qui ont besoin de timing.

### 5. Spawn — `com.bikininjas.corelib.spawn`

- **`SpawnHelper`** : utilitaires pour le spawn d'entités (positionnement,
  conditions, validations).

### 6. Commandes — `com.bikininjas.corelib.command`

- **`CommandRegister`** : enregistrement centralisé des commandes.
  Kit command (`/kit`), etc.

### 7. Joueurs / Stats — `com.bikininjas.corelib.stats`

- **`PlayerStatsManager`** : suivi de statistiques joueur (K/D, etc.)

### 8. Kits — `com.bikininjas.corelib.kit`

- **`KitManager`** : gestion de kits (ensembles d'items donnés au joueur).

### 9. Restrictions — `com.bikininjas.corelib.restriction`

- **`RestrictionManager`** : empêche certaines actions (vol, casse de bloc,
  interaction) dans des zones ou conditions définies.

### 10. Monde / Utilitaires — `com.bikininjas.corelib.world`

- **`WorldUtils`** : helpers pour le monde Minecraft (téléportation, zones, etc.)

### 11. Recettes — `com.bikininjas.corelib.recipe`

- **`RecipeAPI`** : API pour ajouter des recettes programmatiquement via
  reflection sur `RecipeManager` du serveur.
- **`RecipeBuilder`** : builder fluide pour créer des recettes shapeless/shaped
  avec sortie, entrées, id.

### 12. Réseau — `com.bikininjas.corelib.network`

- **`NetworkHandler`** : enregistrement des payloads réseau (CustomPacketPayload).
  Utilise `StreamCodec.ofMember` helper.
- Payloads : `StatsClientData` (sync stats client-side).

### 13. Client — `com.bikininjas.corelib.client`

- **`StatsOverlayRenderer`** : rendu overlay des stats en jeu (F3-like).
- Code client isolé dans `com.bikininjas.corelib.client.*`.
- Enregistré depuis `FMLClientSetupEvent`.

### 14. Registres — `com.bikininjas.corelib.registry`

- **`Registers`** : DeferredRegisters centralisés (ITEMS, BLOCKS, BLOCK_ENTITY_TYPES,
  ENTITY_TYPES). Fournit des helpers :
  - `simpleItem(name)`, `item(name, props)`, `item(name, factory)`
  - `blockWithItem(name, blockFactory)` → `Registration<Block, Item>`
  - `blockEntity(name, factory)`, `entity(name, builder)`
- **Actuellement vide** : aucun item/block concret enregistré. Les mods enfants
  utilisent leurs propres DeferredRegisters.

---

## Structure du Code

```
core-lib/
├── FEATURES.md
├── build.gradle
├── settings.gradle
├── gradle.properties
├── src/
│   ├── main/java/com/bikininjas/corelib/
│   │   ├── CoreLib.java                  @Mod class, initModules()
│   │   ├── client/
│   │   │   └── StatsOverlayRenderer.java
│   │   ├── command/
│   │   │   └── CommandRegister.java
│   │   ├── kit/
│   │   │   └── KitManager.java
│   │   ├── log/
│   │   │   ├── LogManager.java
│   │   │   └── ModLogger.java
│   │   ├── network/
│   │   │   └── NetworkHandler.java
│   │   ├── objective/
│   │   │   └── ObjectiveTracker.java
│   │   ├── randomevent/
│   │   │   ├── RandomEvent.java
│   │   │   └── RandomEventManager.java
│   │   ├── recipe/
│   │   │   ├── RecipeAPI.java
│   │   │   └── RecipeBuilder.java
│   │   ├── registry/
│   │   │   └── Registers.java
│   │   ├── restriction/
│   │   │   └── RestrictionManager.java
│   │   ├── spawn/
│   │   │   └── SpawnHelper.java
│   │   ├── stats/
│   │   │   └── PlayerStatsManager.java
│   │   ├── time/
│   │   │   └── TimeManager.java
│   │   └── world/
│   │       └── WorldUtils.java
│   └── test/java/com/bikininjas/corelib/unit/
│       ├── ModLoggerTests.java
│       ├── ObjectiveTrackerTests.java
│       └── ...
```

---

## Patterns de Conception

- **Modules statiques** : classes `final` + constructeur `private`.
  `init()` pour forcer le chargement (appelé depuis `CoreLib.initModules()`).
- **Event handlers** : static inner class `EventHandler` avec méthodes `static @SubscribeEvent`.
- **Réseau** : `CustomPacketPayload` + `StreamCodec.ofMember`.
- **Client** : package `client/`, enregistré via `FMLClientSetupEvent`.
- **Thread-safe** : `ConcurrentHashMap`, `AtomicReference`.
- **Logs** : `ModLogger` uniquement (pas de `LoggerFactory` direct).

---

## CI / CD

- **CI** (`.github/workflows/ci.yml`) : build + test sur push et PR.
- **CD** (`.github/workflows/cd.yml`) : semver auto-bump, GitHub Release avec JAR.
  - `feat!: /BREAKING CHANGE` → MAJOR
  - `feat:` → MINOR
  - `fix: /perf:` → PATCH
  - `chore: /docs: /style: /refactor: /test:` → skip

---

## Dépendances

- Mods enfants déclarent `implementation "com.bikininjas.corelib:core_lib:${version}"`
- Build local : `build-local.sh` publie core-lib dans `build/repo/` + mods le résolvent via `-Pcore_lib_repo`
- CI : composite build (`includeBuild`) dans `settings.gradle` du mod
- Version unique (pas de suffixe `-local`)
