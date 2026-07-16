# core-lib

Bibliothèque NeoForge partagée pour tous les mods Minecraft Bikininjas. Compatible **Minecraft 1.21.1 à 1.21.11**.

## Stack

| Composant | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.238 |
| Java | 21 |
| Gradle | 9.6.1 |
| Mappings | Parchment 2024.11.17 |
| NeoGradle | moddev 2.0.142 |

## Build

```bash
./gradlew build       # Build + 26 tests JUnit 5 + 40 GameTests (NeoForge Test Framework)
```

---

## API Reference

### Logging (`com.bikininjas.corelib.log`)

```java
// Obtenir un logger préfixé [modId][ClassName]
ModLogger LOGGER = LogManager.getLogger("mon_mod", MaClasse.class);

// Log simple
LOGGER.info("Joueur {} connecté", playerName);
LOGGER.debug("État: {}", state);
LOGGER.warn("Attention: {} ticks de retard", ticks);
LOGGER.error("Échec critique: {}", reason);

// ErrorBuilder fluent — erreurs structurées
LOGGER.error("Échec d'enregistrement")
    .ctx("recipe", recipeId)
    .ctx("input", inputItem)
    .cause(exception)
    .report();  // ← log avec tout le contexte

// Si le logger est désactivé, error() retourne ErrorBuilder.NOOP (zéro coût)
```

### Registres (`com.bikininjas.corelib.registry`)

```java
// DeferredRegister centralisés — dans ton @Mod constructor:
Registers.ITEMS.register(modBus);
Registers.BLOCKS.register(modBus);
Registers.BLOCK_ENTITY_TYPES.register(modBus);
Registers.ENTITY_TYPES.register(modBus);

// Enregistrement d'items
Registers.simpleItem("mon_item");
Registers.item("mon_item", new Item.Properties());
Registers.blockWithItem("mon_block", () -> new Block(BlockBehaviour.Properties.of()));

// Récupération d'un objet enregistré
Registers.ITEMS.get("mon_item");  // Item
```

### Temps (`com.bikininjas.corelib.time`)

```java
// Contrôle par ServerLevel (appelé automatiquement via CoreLib.initModules())
TimeManager.setTime(level, 1000);         // midi
TimeManager.setDay(level);                // 1000
TimeManager.setNight(level);              // 13000
TimeManager.addTime(level, 6000);         // +6000 ticks
TimeManager.setTimeRate(level, 2.0f);     // 2x vitesse
TimeManager.toggleTimeFreeze(level);      // Gèle/dégèle
TimeManager.isTimeFrozen(level);          // Temps gelé ?
TimeManager.getDayTime(level);            // Ticks actuels
  
// Utilitaire de calcul (pas d'état)
long extra = TimeManager.computeExtraTicks(rate=2.0f, tickDelta=1.0f);
```

### Spawn d'entités (`com.bikininjas.corelib.entity`)

```java
// Spawn à une position exacte
SpawnHelper.spawnAt(level, EntityType.ZOMBIE, x, y, z);
SpawnHelper.spawnAt(level, EntityType.ZOMBIE, new Vec3(x, y, z));

// Spawn proche d'un joueur
SpawnHelper.spawnAtPlayer(player, EntityType.ZOMBIE);          // 3 blocks
SpawnHelper.spawnAtPlayer(player, EntityType.ZOMBIE, 10.0);    // 10 blocks
SpawnHelper.spawnNearPlayer(player, EntityType.CREEPER, 5.0);  // radius 5
SpawnHelper.spawnMobAtPlayer(player, EntityType.SKELETON);     // monster, radius 5

// Spawn avec configurateur
SpawnHelper.spawnWithConfig(level, EntityType.ZOMBIE, pos, entity -> {
    entity.setBaby(true);
});

// Spawn aléatoire dans un rayon
SpawnHelper.spawnRandomNearby(level, EntityType.COW, center, 15.0);

// Utilitaires de position
Vec3 pos = SpawnHelper.randomOffset(5.0, angle, fraction);
Vec3 circlePos = SpawnHelper.circlePosition(center, 5.0, index, total);
```

### Enchantements (`com.bikininjas.corelib.enchantment`)

```java
// Vérifier si un niveau d'enchantement est possible (cap à 100)
EnchantmentUtils.canEnchantAtLevel(enchantment, 50);  // true si ≤ max level

// Obtenir le niveau max d'enchantement (plafonné à 100)
EnchantmentUtils.getMaxLevel(enchantment);
```

### Messages (`com.bikininjas.corelib.message`)

```java
// Chat
MessageHelper.chat(player, "Salut!");
MessageHelper.chat(player, Component.literal("Salut!"));
MessageHelper.broadcastChat("Serveur redémarre!", server);
MessageHelper.broadcastTitle(title, subtitle, 10, 70, 20, server);

// Titre / actionbar
MessageHelper.title(player, "Titre", "Sous-titre");
MessageHelper.actionBar(player, "Texte en bas de l'écran");

// Sons
MessageHelper.playSound(player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);

// Formatage couleurs (codes & → couleurs Minecraft)
MutableComponent rouge = MessageHelper.red("Attention!");
MutableComponent formatté = MessageHelper.format("&aVert &lGras &rNormal");
```

### Monde (`com.bikininjas.corelib.world`)

```java
// Manipulation de blocs
WorldUtils.setBlock(level, pos, Blocks.STONE.defaultBlockState());
WorldUtils.getBlock(level, pos);                  // BlockState
WorldUtils.isAir(level, pos);
WorldUtils.isLoaded(level, pos);

// Remplissage de zone
WorldUtils.fillArea(level, from, to, Blocks.STONE.defaultBlockState());

// Entités dans une zone
List<ServerPlayer> players = WorldUtils.getPlayersInRange(level, pos, 20.0);
ServerPlayer nearest = WorldUtils.getNearestPlayer(level, pos, 50.0);
List<Entity> entities = WorldUtils.getEntities(level, Entity.class);
```

### Joueurs (`com.bikininjas.corelib.player`)

```java
// Capturer / restaurer l'état d'un joueur
PlayerState state = PlayerStateManager.save(player);
PlayerStateManager.load(player, state);
PlayerStateManager.clear(player);           // Vide l'inventaire + reset

// Record immuable
PlayerState state = PlayerState.capture(player);
// → mainInventory, armorInventory, offhand, health, food, saturation,
//   xpLevel, xpProgress, effects, gameType
```

### Kits (`com.bikininjas.corelib.kit`)

```java
// Créer un kit simple (items uniquement)
Kit starterKit = Kit.of("starter", 
    new ItemStack(Items.STONE_SWORD), 
    new ItemStack(Items.BREAD, 16));

// Kit complet (items + armure + offhand + effets)
Kit fullKit = new Kit("full", 
    List.of(new ItemStack(Items.DIAMOND_SWORD)),
    List.of(helmet, chestplate, leggings, boots),
    new ItemStack(Items.SHIELD),  // offhand
    List.of(new MobEffectInstance(MobEffects.REGENERATION, 200, 1)));

// Enregistrer et donner
KitManager.register(starterKit);
KitManager.give(player, "starter");     // true si trouvé
KitManager.get("starter");               // Kit ou null
KitManager.getAll();                     // Liste des noms
KitManager.remove("starter");
KitManager.clear();
```

### Événements aléatoires (`com.bikininjas.corelib.randomevent`)

```java
// Interface à implémenter
new RandomEvent() {
    public void execute(ServerLevel level, Vec3 origin) { /* ton code */ }
    public int weight() { return 10; }     // Plus élevé = plus probable
    public String name() { return "mon_event"; }
}

// Manager singleton (thread-safe)
RandomEventManager mgr = RandomEventManager.getInstance();
mgr.register(event);
mgr.selectRandomEvent();     // Sélection pondérée, respecte les cooldowns
mgr.trigger(level, origin);  // Sélectionne ET exécute un event aléatoire
mgr.reset();                 // Vide tout

// Events préfabriqués via RandomEvents (méthodes factory)
RandomEvents.announce(server, "Un événement se produit!");  // /say
RandomEvents.spawnEntity(EntityType.ZOMBIE, 5, 10.0);        // Spawn groupé
RandomEvents.explosion(4.0f, true);                            // Explosion destructive
RandomEvents.clearWeather();                                   // Beau temps
RandomEvents.randomWeather();                                  // Météo aléatoire
```

### Restrictions (`com.bikininjas.corelib.restriction`)

```java
// Types d'actions restreignables
enum RestrictionType { PLACE_BLOCK, BREAK_BLOCK, USE_ITEM, SPAWN_ENTITY, ENTER_DIMENSION }

// Manager — bloque automatiquement via handlers NeoForge
RestrictionManager.register(RestrictionType.BREAK_BLOCK, new ResourceLocation("minecraft", "stone"));
RestrictionManager.isRestricted(RestrictionType.BREAK_BLOCK, stoneId);  // boolean
RestrictionManager.unregister(RestrictionType.BREAK_BLOCK, stoneId);
RestrictionManager.getAll(RestrictionType.BREAK_BLOCK);                  // Set<ResourceLocation>
RestrictionManager.clear();  // Vide toutes les restrictions
```

### Recettes (`com.bikininjas.corelib.recipe`)

```java
// Builder fluent — retourne Optional<RecipeHolder<?>>
RecipeBuilder.shaped(new ItemStack(Items.DIAMOND, 1))
    .pattern("XXX")
    .pattern("X X")
    .pattern("XXX")
    .where('X', Items.STICK)
    .build();

RecipeBuilder.shapeless(new ItemStack(Items.BREAD, 4))
    .requires(Items.WHEAT, 3)
    .build();

RecipeBuilder.smelting(new ItemStack(Items.IRON_INGOT), 
    new ItemStack(Items.IRON_ORE), 0.7f, 200)
    .build();

// API serveur
RecipeAPI.addRecipe("mon_mod:diamond_recipe", holder);
RecipeAPI.removeRecipe("minecraft:diamond_sword");
RecipeAPI.syncToPlayer(player);       // Sync vers un joueur
RecipeAPI.syncToAll(server);          // Sync vers tous
```

### Stats (`com.bikininjas.corelib.stats`)

```java
// Récupérer les stats d'un joueur (tracking automatique)
PlayerStats stats = PlayerStatsManager.getStats(player);
// → deaths, kills, blocksBroken, crafts

// Préférences d'affichage HUD (persistées)
StatsDisplayPrefs.toggle(player);                        // Active/désactive l'overlay
StatsDisplayPrefs.isEnabled(player);                     // true par défaut
StatsDisplayPrefs.setVisibleFields(player, Set.of("deaths", "kills"));
StatsDisplayPrefs.getVisibleFields(player);              // Set<String>
```

### Défis / Objectifs (`com.bikininjas.corelib.objective`)

```java
// Sealed interface Objective — 4 types
KillObjective kill = new KillObjective("Tue 10 zombies", EntityType.ZOMBIE, 10);
CollectObjective collect = new CollectObjective("Ramasse 5 diamants", Items.DIAMOND, 5);
ReachObjective reach = new ReachObjective("Va aux coordonnées", new BlockPos(100, 64, 100), 5);
SurvivalObjective survive = new SurvivalObjective("Survis 5 min", 6000);

// Définition de challenge
ChallengeDefinition def = ChallengeDefinition.of("mon_challenge", "Titre",
    List.of(kill, collect), timeLimitSeconds=300);

// Registre
ChallengeRegistry.register(def);
ChallengeRegistry.get("mon_challenge");        // ChallengeDefinition ou null
ChallengeRegistry.getAvailable();              // Filtre par mods chargés
ChallengeRegistry.areModsLoaded(def);          // Tous les mods requis présents ?

// Tracker — instances actives par joueur
ObjectiveTracker.startChallenge(player, def.toChallenge());
ObjectiveTracker.stopChallenge(player);
ObjectiveTracker.getObjectives(player);         // Objectifs en cours
ObjectiveTracker.getProgress(player);           // 0.0 à 1.0
ObjectiveTracker.isChallengeComplete(player);   // Tous les objectifs remplis ?
```

### Réseau (`com.bikininjas.corelib.network`)

```java
// Initialisation (depuis @Mod constructor)
NetworkHandler.register(modBus);

// Envoyer stats + prefs à un joueur
NetworkHandler.sendStatsSync(player);

// Côté client — cache thread-safe
StatsClientData.getLatest();             // StatsSyncPayload
StatsClientData.isOverlayEnabled();      // boolean
StatsClientData.isFieldVisible(bit);     // boolean (bitmask)
```

### Commandes (`com.bikininjas.corelib.command`)

```java
// Enregistrement de commandes (depuis le constructeur @Mod)
CommandRegister.register(dispatcher, buildContext, selection);  // /kit list, /kit give
```

### Client (`com.bikininjas.corelib.client`)

```java
// StatsOverlayRenderer — rendu HUD automatique via RenderGuiEvent.Post
// S'enregistre automatiquement dans CoreLib (FMLClientSetupEvent)
// Respecte StatsDisplayPrefs (enabled + champs visibles)
```

### GameTests (`com.bikininjas.corelib.gametest`)

```java
// 40 tests in-game couvrant 100% des features testables
// Framework : NeoForge Test Framework (net.neoforged:testframework)
// Annotation : @ForEachTest(groups="core_lib") + @EmptyTemplate("3x3x3", floor=true)
// Helper : ExtendedGameTestHelper (étend GameTestHelper vanilla)
// Pas de fichier .snbt — @EmptyTemplate gère la structure
// Exécution : ./gradlew build (intégré au test source set JUnit)
```

---

## Intégration Mod Enfant

### 1. Dépendance `neoforge.mods.toml` (runtime obligatoire)

Ajoute dans ton `src/main/templates/META-INF/neoforge.mods.toml` :

```toml
[[dependencies.ton_mod]]
    modId="core_lib"
    type="required"
    versionRange="[1.0.0,2.0.0)"
    ordering="BEFORE"
    side="BOTH"
```

→ **NeoForge refuse de charger ton mod si `core_lib` n'est pas dans `mods/`.** Aucun code additionnel nécessaire.

### 2. Build Gradle

```gradle
// settings.gradle (développement local — composite build)
includeBuild('../core-lib')

// build.gradle
repositories {
    mavenCentral()
    flatDir { dirs 'libs' }       // fallback pour CI
}

dependencies {
    implementation 'com.bikininjas.corelib:core_lib:1.0.+'
    // Résolu par composite build en dev local
    // Résolu par libs/core_lib-*.jar en CI
}
```

### 3. CI/CD

**Option A — workflow réutilisable (recommandé)** : `version` optionnel, auto `latest` par défaut.

```yaml
jobs:
  download-corelib:
    uses: bikininjas/bikininjas-core-lib/.github/workflows/download-corelib.yml@master
    # version est optionnel — si omis, résout automatiquement la dernière release

  build:
    needs: download-corelib
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin
      - uses: gradle/actions/setup-gradle@v6
      - run: ./gradlew build
```

**Option B — inline** : résoudre la dernière version via l'API GitHub.

```yaml
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Resolve core-lib version
        id: corelib
        run: |
          VERSION=$(curl -sSf https://api.github.com/repos/bikininjas/bikininjas-core-lib/releases/latest | jq -r .tag_name | sed 's/^v//')
          echo "version=${VERSION}" >> $GITHUB_OUTPUT

      - name: Download core-lib JAR
        run: |
          V="${{ steps.corelib.outputs.version }}"
          mkdir -p libs
          curl -sSfL -o "libs/core_lib-${V}.jar" \
            "https://github.com/bikininjas/bikininjas-core-lib/releases/download/v${V}/core_lib-${V}.jar"

      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin
      - uses: gradle/actions/setup-gradle@v6
      - run: ./gradlew build
```

### 4. Constructeur `@Mod`

```java
@Mod("ton_mod")
public class TonMod {
    public TonMod(IEventBus modBus) {
        // Enregistrer les DeferredRegister de core-lib
        Registers.ITEMS.register(modBus);
        Registers.BLOCKS.register(modBus);
        Registers.BLOCK_ENTITY_TYPES.register(modBus);
        Registers.ENTITY_TYPES.register(modBus);

        // Réseau
        NetworkHandler.register(modBus);

        // Tes registrations...
    }
}
```

---

## Patterns de Code

### Logger

```java
// PAS : org.slf4j.LoggerFactory.getLogger()
// OUI :
private static final ModLogger LOGGER = LogManager.getLogger("ton_mod", TonMod.class);
```

### Modules utilitaires

Toutes les classes sont `final` avec constructeur `private` :

```java
public final class TonModule {
    private TonModule() {}

    // Event bus via static initializer (pas @EventBusSubscriber)
    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    // init() pour forcer le chargement
    public static void init() {}

    private static final class EventHandler {
        @SubscribeEvent
        static void onEvent(SomeEvent event) { /* ... */ }
    }
}
```

### Séparation Client/Serveur

- Code client dans `com.bikininjas.tonmod.client.*`
- Enregistrement explicite via `FMLClientSetupEvent` (pas `@EventBusSubscriber(bus=Bus.MOD)`)

### Null-safety

```java
@NotNull / @Nullable sur paramètres et retours publics
Objects.requireNonNull() en début de méthode publique
```

---

## CI/CD

| Workflow | Déclencheur | Action |
|---|---|---|
| **CI** (`ci.yml`) | Push/PR sur `master` | `./gradlew build test` (Java 21, setup-gradle@v6) |
| **CD** (`cd.yml`) | Après succès CI (`workflow_run`) | Auto-version semver → build JAR → GitHub Release |

**Règle : CD ne se lance JAMAIS en parallèle du push.** Elle dépend de la CI via `workflow_run: workflows: ["CI"]`.

### Versioning

- `feat!:` ou `BREAKING CHANGE` → **major** (2.0.0)
- `feat:` → **minor** (1.1.0)
- `fix:`, `perf:` → **patch** (1.0.1)
- `chore:`, `docs:`, `style:`, `refactor:`, `test:` → skip release
