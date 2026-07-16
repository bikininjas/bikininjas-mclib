# Idées de fonctionnalités pour core-lib

Thème : mods fun "défis Minecraft" ou "finir Minecraft".

---

## 🔧 Utilitaires haute priorité

### 1. PlayerStateManager
Snapshots d'état joueur pour checkpoints/rollback dans les défis.

- `PlayerState` : inventaire, armure, XP, santé, faim, effets de potions
- `save(player)` → retourne un snapshot
- `load(player, state)` → restaure intégralement
- `clear(player)` → reset complet

*Utile pour :* début de défi (sauvegarder l'équipement), permadeath, "retour au spawn les mains vides".

### 2. KitManager
Système de kits/gabarits d'équipement.

- `Kit` : liste d'items (ItemStack avec NBT), effets de potions, XP
- `KitManager.register("nom", kit)`
- `KitManager.give(player, "nom")` → donne le kit, drop l'inventaire existant ou merge
- Chargement depuis JSON data-driven

*Utile pour :* donner un équipement de départ, récompenses de palier, kits de classe.

### 3. ObjectiveTracker
Suivi d'objectifs pour progression dans un défi.

- Objectifs : kill X mobs, collect Y items, atteindre Z position, stay alive T ticks
- `ObjectiveTracker.startChallenge(player, objectives)`
- Événements : `onPlayerKill`, `onItemPickup`, `onPlayerMove`
- Callback de complétion (déclenche récompense, portail, etc.)
- Timer intégré (speedrun)

*Utile pour :* défis "tuer le wither en moins de 10min", "craft un beacon", bingo.

### 4. ZoneManager
Régions avec callbacks d'entrée/sortie/tick.

- `Zone.cuboid(start, end)`, `Zone.sphere(center, radius)`, `Zone.union(zones...)`
- `ZoneManager.register(zone, events)` : `onEnter(player)`, `onExit(player)`, `onTick(player)`
- Empilable et priorisé (une zone peut contenir des sous-zones)

*Utile pour :* arènes de boss, zones de danger, no-PvP, déclencheurs d'événements à l'entrée.

---

## 🎮 Mécaniques de jeu

### 5. GameRuleManager
Modifier les règles du jeu à la volée via API.

- Wrapper autour des gamerules Minecraft : `set(DO_DAYLIGHT_CYCLE, false)`, `toggle(KEEP_INVENTORY)`
- Sauvegarde/restauration de l'état des gamerules
- Règles customs (register new gamerule via DeferredRegister)

*Utile pour :* challenges "hardcore sans keepInventory", "nuit permanente", "pas de regen naturelle".

### 6. DifficultyScaler
Ajustement dynamique de la difficulté.

- Modificateurs par type de mob : `scale(EntityType.ZOMBIE, 2.0f)` → 2× HP/dégâts
- Facteurs : temps écoulé, nombre de joueurs, kills total
- `getScaledDamage(entity, baseDamage)`, `getScaledHealth(entity, baseHealth)`
- Chargement depuis JSON data maps

*Utile pour :* difficulté progressive, scaling multijoueur, mode "chaos".

### 7. DamageManager
Interception et modification des dégâts.

- `registerHandler(predicate, handler)` : `onDamage(event)` → modifie, annule, redirige
- Boucliers temporaires, reflet de dégâts, dégâts élémentaires, dégâts en % de vie max
- Utilitaire de calcul : `multiplyDamage(event, factor)`, `negateDamage(event)`

*Utile pour :* boss avec phases d'invulnérabilité, mobs immunisés à certains dégâts.

---

## 📢 Communication & Interface

### 8. MessageHelper
Notifications unifiées.

- `title(player, title, subtitle, fadeIn, stay, fadeOut)`
- `actionBar(player, message)`
- `broadcastAll(messages...)` avec différents canaux
- Builder de composants JSON : `text("rouge").red().bold()`, hover, click
- Compteurs et timers affichés dans l'action bar

*Utile pour :* annonces de début/fin de défi, warning de zone dangereuse, timer de speedrun.

---

## 🧩 Améliorations des modules existants

### 9. TimeManager — ajouter :
- `getDayCount(level)` — nombre de jours Minecraft écoulés
- `getElapsedTicks(level)` — ticks depuis le début de la partie
- `scheduleEvent(level, ticks, Runnable)` — exécuter dans N ticks

### 10. SpawnHelper — ajouter :
- `spawnWithGear(...)` — spawner un mob avec équipement (armure, weapon, enchants)
- `spawnPersistent(...)` — mob qui ne despawn pas
- `spawnWave(level, center, List<EntityType>, int count, int interval)` — vagues

### 11. RandomEventManager — ajouter :
- Événements conditionnels (ne se déclenchent que si une condition est remplie)
- Événements déclenchés manuellement par commande
- Intégration avec ObjectiveTracker pour récompenses

---

## 📦 Priorité suggérée

| Priorité | Feature | Complexité | Réutilisabilité |
|----------|---------|------------|-----------------|
| P0 | PlayerStateManager | Faible | Très haute |
| P0 | KitManager | Faible | Très haute |
| P1 | ObjectiveTracker | Haute | Très haute |
| P1 | MessageHelper | Faible | Très haute |
| P2 | ZoneManager | Haute | Haute |
| P2 | GameRuleManager | Faible | Haute |
| P3 | DifficultyScaler | Moyenne | Moyenne |
| P3 | DamageManager | Haute | Haute |
