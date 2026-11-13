# OldschoolPrac

A practice mod for speedrunning Minecraft 1.0, the oldest version people run. It covers the stuff that's hard to practice in real runs:

- **End fight**: drops you into a fresh End with your kit, times the fight, and tells you after every bed how close it was to a perfect one
- **Endgame**: the tower build, stronghold and dragon, with splits and a pit planner that picks your pit spot for you
- **Trainers**: crystal shooting (a full island, or one crystal at a time with a streak) and eye throws, where you find the stronghold and see how close your guess was
- **Blaze fight**: real open and enclosed fortress spawners, with your spawner kit
- **PBs, stats and a kit editor**, since 1.0 can't switch game modes to fix your inventory
- **Singleplayer commands**: chat works in singleplayer, with /gamemode (survival, creative and a spectator mode 1.0 never had), /tp, /time and more

It's a Fabric mod (Fabric Loader 0.18.1 or newer, Ornithe intermediary). It's built and tested on 1.0. The metadata allows up to 1.2.5, but I haven't tried those.

## Getting started

1. Drop `oldschoolprac-<version>.jar` into your instance's `.minecraft/mods` folder.
2. On the title screen, click the ender pearl button next to Singleplayer and pick a mode.
3. Each mode also has a hotkey you can press on the title screen if you prefer (P for the End fight, and the rest are below). The same key restarts the mode while you're in it. Every hotkey can be changed or set to NONE under Settings, Hotkeys.

Every attempt is a brand new world with a random seed, so you never get the same island, tower or fortress twice. The practice worlds overwrite themselves each attempt, so they won't fill up your saves.

## End fight

Press **P** (or Dragon fight in the menu). You spawn on the obsidian platform with full health, full hunger and your kit, and you go in through the game's own End portal code, so it's the same as jumping in during a run. **P** restarts any time. **K** pressed twice saves your current inventory as the kit for next time.

The HUD in the top left (hidden with F3) shows the timer, dragon health, crystals left, your PB and your last bed.

After each bed near the dragon, chat tells you how it went:

- `Bed 47/81, 3.2 from head: 3.0 behind, 1.0 below`: the damage out of 81 (a perfect bed), and where the bed was compared to the head, in the dragon's own directions
- `3 ticks late (0.15 s): the perfect tick does up to 63 (+16)`: how early or late you clicked, and what the best tick would have done. Nailing it says `Perfect tick`, and 1 tick off says `Good`
- whether it only hit the body, tail or wings (those only take a quarter of the damage, plus 1)
- whether the dragon was still invulnerable from your last hit, and for how long

When the dragon dies you get the time, whether it's a PB, how many beds you used, your average (all time and last 10) and your completion rate. Attempts you die in, leave or reset count against the completion rate.

### How bed damage works in 1.0

A bed is a power 5 explosion, which gets doubled to 10 for hurting entities, so it reaches 10 blocks. Each dragon part in range takes `(q*q + q) / 2 * 80 + 1`, where `q = (1 - distance / 10) * exposure`. Distance is to the bottom middle of the part's hitbox, and exposure is how much of the hitbox the explosion can see.

- The head is a 3x3x3 box whose bottom middle sits 5.5 blocks in front of the dragon. A bed right there does 81.
- Every other part takes a quarter of that plus 1, so 21 at most.
- After a hit the dragon is invulnerable for 10 ticks, and a hit during that only counts for whatever it does above the last one.

It's the same maths as 1.7.10. The difference is that 1.7 draws the dragon a few ticks behind where it really is, while in 1.0 the hitbox is right where you see it.

## Endgame

Three modes, all in the menu:

- **O**: full endgame, from tower build to portal room to entering the End to the dragon
- **I**: just the tower build
- **L**: stronghold to dragon, starting with the stronghold kit (14 eyes)

Structures are on here, so the stronghold is the seed's real one. You start on a flat plains or desert spot away from water, somewhere random 128 to 384 blocks from the stronghold, in any direction. 1.0 has an eye bug where eyes can lead you to a stronghold 600+ blocks away, so the mod only uses spots where eyes thrown from the tower and from around where your pit will be actually lead to the nearby one.

The tower kit is what you'd have by then: nether bricks, leaves, nether brick fences, a boat, a fence gate, cactus, a pressure plate and a water bucket in the hotbar, plus more leaves, stone tools, steak, planks, string, feathers, flint, sticks, 7 blaze rods, wool, a crafting table, flint and steel, ladders and dirt. The stronghold kit swaps the tower blocks for cobble and leaves, and adds the eyes and a spare pearl.

The splits are:

- **Tower build**: ends when you die (your death reset in the pit). Dying before you've been 20 blocks up doesn't count.
- **Portal room**: from your respawn until you're next to an End portal frame. More death resets on the way are fine.
- **Enter End**, then **Dragon**, with the same HUD and bed report as the End fight.

The tower starts at 8:30 in-game time, and the HUD counts down to night (11:00), then counts the overtime. Your death reset tells you how far before or after night it was. Stronghold attempts start as the night ends. Each mode keeps its own PBs, so a stronghold-only time never counts against a full run. Through the night the HUD also shows endermen killed, pearls dropped and the drop rate.

**K** pressed twice saves your inventory as the tower kit (or the stronghold kit in stronghold mode).

### Pit planner

This is on by default for full and tower runs. The tower goes at spawn, since that's where you respawn after each death, and the planner works out the best spot for the pit 205 blocks out (you can change the distance). It draws the route on the ground through blocks: a cyan line to the pit, a yellow beam over the tower and a red beam over the pit. The HUD shows something like `Pit -600 63 741  S 211m  flush 90%`, so you always know where it is.

It starts about 10 seconds after the world loads and takes around half a minute, which is fine since you're busy building anyway. It loads the terrain slowly so it doesn't lag you, and shows `Planning pit 40%` while it works.

It checks 36 directions, skips the ones with oceans, rivers, swamps or lots of forest and hills, and scores the rest on:

- **the run**: water (lava is way worse), trees in the way, and climbs and drops, with big steps costing more than gentle slopes
- **view**: how much spawnable ground you can actually see along the way and from the pit. A hill that blocks your view costs points, and one with a wide view earns them
- **spawnable ground**: open, dry, solid ground near the tower and the pit, so more of each spawn lands on the surface near you instead of in caves
- **the pit spot itself**: it has to be dry, flat and solid all the way 44 blocks down, with no caves, water or lava underneath. If the spot on the line isn't, it uses the nearest one within 8 blocks that is

"Flush" is how much of the area around the tower is over 128 blocks from the pit. Everything there despawns the moment you reach the pit, which frees up the mob cap. At 256 blocks out that's all of it, and at 205 it's 90%, but 205 is a lot less running. **J** replans from where you're standing. The lines disappear after your death reset.

## Trainers

**C** (Crystals) puts you in the middle of a fresh End island with a bow, 3 stacks of arrows and no dragon. The timer starts on your first arrow and stops when the last crystal goes. You get a split for every crystal, and the HUD shows crystals left, arrows and accuracy.

Every miss tells you how to fix the next shot, like `Miss: 2.4 high: aim lower (left/right was fine)`, or `hit a block 3.5 below it before it got there: aim higher` if you hit the tower.

**R** (Random crystals) is one crystal at a time. You get a random spot 10 to 56 blocks from a single crystal, roughly facing it. Every spot is checked by simulating real bow shots, so you'll never get one that's impossible to hit. Hit it and you're moved to a new spot and a new tower straight away. Hits in a row build a streak, and a miss ends it.

### Eye throw

**Y** (Eye throw) puts you on a random spot 300 to 650 blocks from a stronghold, with 16 eyes of ender that keep refilling, a stack of cobble to pillar up with, and food. The HUD shows your coordinates in this mode. The kit is the Eye throw tab in Kits, or press **K** twice to save your inventory as it. Throw as many as you like, work out where the stronghold is, then either stand where you think it is and press **G**, or type `/guess x z`. Chat tells you where the portal room really was (that's where the eye drops once the stronghold has loaded), how many blocks off you were and which way it was, and your average and best are kept in Stats.

Spots are 300 to 650 blocks from the stronghold, and about half are picked so an eye thrown from the ground hits the 1.0 eye bug and flies to a farther stronghold. 1.0 scores each stronghold as `dx·(1 + dy²) + dz²` (dy is the height gap) instead of the real distance, and until a stronghold has generated near you it treats all three as being at y 64. The trainer works the eye out the same way. Closer than about 300 blocks the stronghold has already generated when the area loads, and eyes always find it.

Every throw is listed in chat with the height you threw from and which way it went. With **Eye trainer: coaching** on (Settings, on by default), each eye that hits the bug says so. After two throws far enough apart, chat shows where their lines cross and whether both eyes went to the same stronghold. It also says where the other two strongholds can be: 1.0 puts the three 120 degrees apart around 0,0, each 640 to 1152 blocks out and moved up to 112 blocks for its biome. From that it tells you whether either could be closer to you than the one you found. After your guess it says which stronghold your eyes led to and whether that was the closest one to where you started. Turn coaching off to practise without help. **Eye trainer: eye angles** (Settings, off by default) also gives each eye's angle, in the same yaw Minecraft uses.

## Blaze fight

**B** (Open spawner) and **N** (Enclosed spawner) put you in the doorway of a real fortress's blaze spawner room, facing the spawner. You get the kit you'd actually have at that point: stone tools, 3 stacks of leaves, 48 oak planks, 5 gravel, 5 feathers, 3 string, 4 cobble, steak, a crafting table and flint and steel, so you can overlap your pit crafts while you get rods. Any blazes already there are cleared, so every attempt starts the same.

The timer runs until you have 7 rods (or 6, it's a setting). The HUD shows the time, your PB, your rods, blazes killed and the drop rate.

If you play 1.0 tower, turn on **Blaze: tower crafts** in Settings. Then an attempt only finishes once you've also crafted a pressure plate, a fence gate, 3 ladders, 3 nether brick fences and a boat, and the HUD tells you what's still missing. These attempts get their own PBs.

The mod finds fortresses from the seed the same way the game does, so it only has to generate the fortress itself. A spawner counts as enclosed when less than 25% of the space around its room is air or lava, and open when more than 50% is. If no nearby fortress has the kind you asked for, you get the closest one and chat lets you know.

## The menu

The pearl menu has the modes grouped into Endgame, End Fight, Trainers and Blaze Fight, with their keys in grey. If you only use the menu, turn off **Mode keys on the title screen** in Settings. Underneath are:

- **Settings**: keys, HUD, bed report, kits and times
- **Kits**: the kit editor
- **Stats**: all your PBs in one place, with End fight averages and completion rate, endgame splits and sum of best, crystal times, your best random crystal streak, blaze PBs and your eye trainer average
- **Lab**: tools for testing the endgame night (see below)

### Kit editor

1.0 has no creative mode, so this is how you change your kits. Pick a tab (End fight, Tower, Stronghold or Blaze), then pick an item from the list on the right. Type to search and scroll for more. Left click takes a stack and right click takes one. Click a slot to put it there, or right click a slot to empty it. You can also select a slot and use -1, +1, Stack or Empty. Done saves, Cancel doesn't, and Reset puts the default kit back.

### Lab

For digging into how the night works:

- **Enderman tracker**: counts every enderman that spawns in full runs, and how many were on the surface near you, then saves the run when the night ends or you throw your first eye. Chat shows your averages over every run. It only works with Pearl luck set to vanilla, so the numbers are real.
- **Tracker: cycles a night**: with the tracker on, also counts your death reset cycles each night (every death in the pit until the night is over, the tower death included) and shows the count on the HUD and the average in chat and Enderman stats
- **Respawn timer**: on the death screen after your first death in the pit, counts down the ticks until the sky light hits its lowest (11:23 into a run in clear weather, sooner in rain), then says `RESPAWN NOW`. Chat tells you how many ticks early or late you respawned
- **Enderman stats**: your averages and last runs from the tracker
- **Enderman outlines**: pink boxes around every enderman, through walls
- **Aggro dot**: a red dot over every enderman that's coming for you
- **Spawn overlay**: the mob cap (`Cap 80/79`), despawns, and when the last spawn burst happened. In 1.0 the game tries to spawn in every chunk at once whenever you're under the cap, so it comes in big bursts
- **Flush overlay**: after you respawn on the tower, how many of the mobs that were loaded while you were dead despawned (`Flushed 32/83 39%`, endermen in pink)
- **Pearl luck**: `average` (exactly 1 pearl every 2 endermen), `lucky` (2 every 3) or `vanilla` (random)
- **Pit planner** on or off, its distance and its key

## Commands

1.0 only has chat in multiplayer, so the mod adds it to singleplayer: press **T**, type, and Enter. Lines starting with `/` are commands:

- `/gamemode s`, `c` or `sp` (or `/gm`, `/gms`, `/gmc`, `/gmsp`): survival, creative or spectator. Spectator is new, since 1.0 doesn't have it: you fly through blocks and see through them (everything is lit up, so caves and strongholds show from inside the ground), mobs ignore you, and you can't break, place, use or pick up anything
- `/tp x y z`, with `~` for where you are (`/tp ~ ~20 ~` goes up 20)
- `/time set day|noon|night|midnight|<ticks>`, `/time add <ticks>`, or `/time` to see it
- `/weather clear|rain|thunder`
- `/give <id> [count] [damage]`, like `/give 381 12` for 12 eyes of ender
- `/heal`, `/kill`, `/pos`, `/seed`, `/guess x z` (eye trainer) and `/help`

Using a command that changes the game during a practice attempt (anything but `/pos`, `/seed`, `/help`, `/kill` and `/guess`) means that attempt's times and stats aren't saved, so your PBs stay real. Commands can be turned off in Settings.

## Settings

Everything's in game under Settings (and Lab). Click a setting to change it (shift click goes back). The Hotkeys section at the top has every mode's key: click one and press a new key, or shift click it to set it to NONE. A key shows red if something else already uses it, and there are buttons to set them all to NONE or back to the defaults. Done saves, Escape doesn't.

They're also in `config/endpractice.properties` if you'd rather edit the file. Restart the game after editing it.

| Setting | Default | |
|---|---|---|
| `keyStart` | `P` | Start or restart the End fight |
| `keySaveKit` | `K` | Save your inventory as the kit (press twice) |
| `kit` | sword, bow and 7 beds in the hotbar; arrows, water bucket, cobble, pearls, bread, pickaxe | `slot:itemId:count[:damage]`, comma separated. Slots 0-8 hotbar, 9-35 inventory, 36-39 armor |
| `keyEndgame` | `O` | Full endgame |
| `keyTower` | `I` | Tower build only |
| `keyStronghold` | `L` | Stronghold to dragon |
| `keyCrystals` | `C` | Crystal aim trainer |
| `keyRandomCrystals` | `R` | Random crystals |
| `modeKeys` | `true` | Mode keys on the title screen (off: menu only) |
| `keyBlazeOpen` | `B` | Open blaze spawner |
| `keyBlazeEnclosed` | `N` | Enclosed blaze spawner |
| `blazeRods` | `7` | Rods to get in blaze practice: 6 or 7 |
| `blazeTowerCrafts` | `false` | Blaze practice also needs the tower crafts |
| `keyEyeTrainer` | `Y` | Eye throw trainer |
| `keyGuess` | `G` | Eye trainer: guess the stronghold is where you stand |
| `eyeAngles` | `false` | Eye trainer: say each eye's angle |
| `eyeCoaching` | `true` | Eye trainer: eye bug warnings, where your throws cross, where the other strongholds can be |
| `commands` | `true` | Singleplayer chat and commands |
| `eyeKit` | 16 eyes, 64 cobble, 16 steak | Same format as `kit` |
| `blazeKit` | see Blaze fight | Same format as `kit` |
| `towerKit` | see Endgame | Same format as `kit` |
| `strongholdKit` | 14 eyes, a pearl, tools, food, cobble, leaves, wool, planks, crafting table, flint and steel | Same format as `kit` |
| `towerStartTime` | `10200` | World time the tower build starts at (10200 is 8:30 in, -1 leaves it alone) |
| `nightTime` | `13200` | When night starts (13200 is 11:00 in) |
| `pearlLuck` | `average` | `average`, `lucky` or `vanilla` |
| `strongholdStartTime` | `22500` | World time stronghold attempts start at (the end of the night) |
| `endermanTracker` | `false` | Track endermen in full runs (needs `pearlLuck=vanilla`) |
| `endermanOutline` | `false` | Pink boxes around endermen |
| `endermanAggroDot` | `false` | Red dot over angry endermen |
| `flushOverlay` | `false` | How many mobs despawned on your last respawn |
| `cycleTracker` | `true` | With the enderman tracker, count death reset cycles each night |
| `respawnTimer` | `true` | Count down to the lowest light on the death screen after your first pit death |
| `spawnOverlay` | `false` | Mob cap, despawns and spawn bursts |
| `endermanRange` | `87` | Surface endermen only count within this many blocks of you (0: anywhere) |
| `pitPlanner` | `true` | Plan the pit and draw the route |
| `pitDistance` | `205` | How far out the pit goes (180 to 256) |
| `keyPitPlan` | `J` | Replan the pit from where you stand |
| `hud` | `true` | Top left overlay |
| `bedReport` | `true` | Chat report after each bed |
| `bedReportRange` | `16.0` | Only report beds this close to the head |

Your times, PBs and runs are saved in the `config/endpractice-*.txt` files. Delete one to start that stat over.

## Building it yourself

It's a standard Fabric Loom build with Ornithe's Ploceus plugin and Feather mappings. Gradle needs JDK 25 or newer.

```
./gradlew build          # the jar ends up in build/libs
./gradlew runClient      # a dev client with the mod
./gradlew runSelfTest    # automated tests (see build.gradle for the options)
```
