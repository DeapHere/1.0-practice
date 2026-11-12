# OldschoolPrac

Speedrun practice for the oldest version people run, Minecraft 1.0. The dragon fight drops you into a fresh End with your kit, times the fight and tells you after every bed how close it was to a perfect one. Endgame practice covers the tower build and the stronghold too, split by split, with a pit planner. There's also a crystal aim trainer, open and enclosed blaze spawners, PBs and stats, and a kit editor.

Practice only. It is not meant for runs and isn't submitted for approval.

Fabric mod for Minecraft 1.0 (Fabric Loader 0.18.1 or newer, Ornithe intermediary). Built against 1.0; the metadata allows 1.0 to 1.2.5 but only 1.0 has been tested.

## Use

1. Put `oldschoolprac-<version>.jar` in the instance's `.minecraft/mods` folder.
2. On the title screen click the ender pearl button left of Singleplayer and pick a mode, or press **P**. You land on the obsidian platform in a new End, with full health, full hunger and the kit.
3. Fight. **P** restarts at any time while practicing. **K** saves your current inventory as the kit for every later attempt.

Each attempt is a new world with a random seed, so the island and towers are different every time. End fight worlds have structures turned off: it makes no difference in the End, and it avoids a vanilla 1.0 crash in the overworld you respawn in after dying. The practice world is the `EndPractice` save and is replaced on each attempt. You go in through vanilla's own End portal code, the same way as stepping into the portal in a run.

## Endgame practice

Three more modes, in the practice menu or with a key on the title screen (the same key restarts while in one of them):

- **O**, full endgame: tower build, portal room, enter End, dragon
- **I**, tower build only
- **L**, stronghold to dragon: starts with the stronghold kit (14 eyes of ender) and goes portal room, enter End, dragon

Each attempt is a new world with structures on, so the stronghold is the seed's own. You start on a flat plains or desert spot with no ocean, river or lake nearby, a random 128 to 384 blocks from a stronghold, any direction (it becomes the spawn point), and only where an eye of ender thrown there or anywhere 205 blocks around (where the pit goes) leads to that stronghold (on the ground and from the top of the tower). 1.0's nearest stronghold search has a bug that can send eyes to one over 600 blocks away, with the tower kit: nether bricks, a stack of leaves, nether brick fences, a boat, a fence gate, cactus, a pressure plate and a water bucket in the hotbar, and two more stacks of leaves, stone tools, 16 steak, 30 oak planks, string, feathers, flint, sticks, 7 blaze rods, 21 wool, a crafting table, flint and steel, 3 ladders and 3 dirt in the inventory. The stronghold kit drops the tower blocks for 16 cobblestone and a stack of leaves, and adds 14 eyes and a spare pearl.

- **Tower build** ends when you die (death reset in the pit). In tower-only mode that's the end of the attempt. A death before you've been 20 blocks above the ground doesn't count.
- **Portal room** starts when you respawn and ends when you're next to an End portal frame. Deaths after that first one (more death resets in the pit) don't stop the run.
- **Enter End** ends when you arrive in the End, and **Dragon** when it dies. The End has the same HUD and bed report as the End fight practice.

The tower build starts at 8:30 in-game time, with a countdown to night (11:00) on the HUD that turns into +overtime until your death reset; the death reset then says how far before or after night it was. Stronghold attempts start as the night ends. Each mode keeps its own PBs, so stronghold-only times never count against full runs: the HUD shows the split you're on with its PB (a finished split stays for 10 seconds, and its time is in chat), and a whole attempt that kills the dragon has one shown next to the timer. Every finished split is saved in `config/endpractice-splits.txt`.

Through the night (from night or the first enderman until the night is over or the first eye of ender is thrown) the HUD shows the endermen killed and the pearls they dropped, with the share of endermen that dropped one.

**Enderman tracker** (off by default, in the Lab; it only turns on with Pearl luck set to vanilla, so the saved drop rates are real, and Pearl luck is in the Lab too): for full endgame runs (**O**), it also counts every enderman that spawns in the loaded overworld, and separately the ones on the surface: under the open sky (nothing above them but air, leaves, plants, snow or vines; the rest are in caves) and within the surface range of you at some point, 87 blocks by default since that's as far as 1.0 draws an enderman (they spawn up to about 128 blocks out). The range is a setting; 0 counts surface endermen anywhere in the loaded world. The HUD shows `Spawned 7 (3 surface)` with the averages through the night. The run's endermen spawned, surface, killed, dropped and pearls are saved once the night is over (the last tick monsters can spawn on the surface: sky light above 7, which is 22811 in clear weather, about 19:00 into a run; later in rain, and never while it's thundering) or the first eye of ender is thrown, whichever comes first, and chat shows them with the averages over every saved run; after that the endermen lines leave the HUD. Runs reset before then aren't saved. **Enderman outlines** (also in the Lab) draws every enderman's hitbox as a pink box that shows through blocks, so they're easy to spot; like vanilla, endermen are only drawn within about 87 blocks. **Enderman aggro dot** puts a red dot above every enderman that's after you (the same moment vanilla opens its mouth), also through blocks. **Spawn overlay** (Lab) shows how 1.0 spawns monsters, read from the real spawner, in the top right: `Cap 80/79` (red when full), `Despawned 6` since the last spawn, and `Spawned 8, 12s ago` for the last burst (`+E` when it had an enderman). Every tick the loaded monsters are at or under the cap (70 x 289 chunks / 256 = 79, counting every loaded monster, caves included), the spawner tries every chunk around you at once, so one tick can spawn a dozen and go over the cap. Then nothing spawns until monsters despawn (over 128 blocks away straight away, over 32 blocks now and then) or die, and the next open tick spawns another burst. **Flush overlay** (Lab) shows `Flushed 32/83 39%` in the top right after you respawn on the tower: how many of the monsters loaded while you were dead despawned in the 2 seconds after (they were over 128 blocks from the tower), with the endermen in pink (`3/7 E`). It stays until the next respawn. **Enderman stats** in the Lab shows the averages, the range and the last runs. The runs are in `config/endpractice-endermen.txt` (`time,spawned,killed,dropped,pearls,surface`); delete it to start the averages over. **K** pressed twice saves your inventory as the tower kit, or as the stronghold kit in stronghold mode.

### Pit planner

On by default in full endgame and tower-only runs. The tower goes at the world spawn, since that's where you respawn on top after every death in the pit, and the mod already starts you on a flat spot there. The planner picks where to dig the pit, the set distance out (205 blocks by default) in the best of 36 directions (every 10 degrees), and draws the run on the ground: a cyan line from the tower to the pit, a yellow beam over the tower spot and a red beam over the pit, all seen through blocks. Chat says where the pit is when it's done (it starts 10 seconds after the world loads, while the game is still busy loading around you, and takes about half a minute more. The biome map, which costs nothing to read, picks the 16 directions worth a closer look first, with no ocean, river or swamp and few forests or hills. Then it generates the terrain those need, at most one chunk a tick and none while the game is already lagging. The HUD shows `Planning pit 40%` meanwhile), and the HUD shows `Pit -600 63 741  S 211m  flush 90%`: the pit, which way and how far it is from you, and the flush share.

Each of those directions is scored on:

- the run: water (and much worse, lava) to swim through, trees in the way, and climbs and drops along the line, with big steps costing more than gentle slopes
- view: from a point every 40 blocks along the route and from the pit (counted double), how much of the ground within 48 blocks is open, spawnable and in your line of sight (a line from your eyes to an enderman's chest over it hits no terrain). A hill that hides the ground costs, and a rise with a wide view pays, so the line can go over a hill instead of around it when the view is worth the climb
- spawnable ground: the share of open, solid, dry ground around the tower and the pit, instead of water, trees or nothing, so more of each spawn burst lands on the surface near you instead of in caves
- the pit spot itself must be dry, flat (within a block over 3x3) and solid 44 blocks down under all of that 3x3: no cave, water or lava below it. If the spot on the line isn't, the nearest one within 8 blocks that is gets used

Flush is the share of the area within 128 blocks of the tower that is over 128 blocks from the pit: every monster there despawns the moment you reach the pit, freeing the mob cap for the next burst. At 256 blocks it's everything; at 205 it's 90%, and what's left is the strip 77 to 128 blocks out from the tower toward the pit. 205 costs less running (and hunger) per lap. The distance is a setting (180 to 256), so you can try both and compare the averages with the enderman tracker. **J** plans again from where you are standing, as the tower spot. The lines go away at your death reset, once the pit is built.

## Crystal aim trainer

**C** on the title screen (or Crystal aim trainer in the practice menu) puts you in the middle of a new End island with a bow and 3 stacks of arrows, and the dragon taken out. The timer starts with your first arrow and stops when the last crystal goes. Every crystal gets a split in chat, the HUD shows crystals left, arrows shot and the share that hit, and the finish line gives the time per crystal (islands have different numbers of crystals). The best time is kept in `config/endpractice-crystals.txt`. **C** gives a new island.

After every arrow that misses, chat says where it went past the crystal and which way to aim the next one, measured from the crystal's middle at the point where the arrow got level with it: `Miss: 2.4 high: aim lower (left/right was fine)`, `Miss: 1.8 left: aim right (height was fine)`, or `hit a block 3.5 below it before it got there: aim higher` when it hit the tower or the ground short of the crystal. Within 1.3 of the middle in both directions counts as a hit, so anything inside that is `only just past it`. The last miss stays on the HUD.

### Random crystals

**R** on the title screen (or Random in the practice menu) is the trainer one crystal at a time. The island's crystals are taken out and one is put back on a random tower. You stand on a random end stone spot 10 to 56 blocks from it, facing roughly its way (up to 45 degrees off). Every spot is checked by simulating fully drawn bow shots from it (the arrow's real speed, drag and gravity, against the blocks in the way and the crystal's hitbox), and only spots with at least 1.5 degrees of aim that hit are used. Close shots stay in, but a wide tower whose top edge hides the crystal from where you'd stand is never given. Hit it and half a second later you are on a new spot with a new crystal on a different tower, with full health and arrows.

Crystals hit without a missed arrow in between make a streak. A miss ends it (an arrow counts as a miss once it lands or flies more than 3 blocks past the crystal). The HUD shows the streak and your best, the time on the current crystal, the average time per hit, and hits, arrows and accuracy. Every ended streak is saved in `config/endpractice-streaks.txt` and the best one is kept from there. **R** gives a new island.

## What it shows

Top left (hidden while F3 is open):

- the timer, in game ticks from arriving to the dragon dying (pausing stops it)
- dragon health and crystals left
- your PB and the last bed

In chat after each bed that goes off near the dragon:

- `Bed 47/81, 3.2 from head: 3.0 behind, 1.0 below`: the damage done out of the 81 a perfect bed does, and where the bed was compared with the head's damage point, in the dragon's own directions (behind/in front, below/above, left/right)
- `3 ticks late (0.15 s): the perfect tick does up to 63 (+16)`: how early or late your click was. Within half a second either side, the perfect tick is when the head came closest to where the bed went off. The line shows what a bed would have done then and how much more that is. `Perfect tick` means you hit it, and 1 tick off shows as `Good`.
- if the bed only reached the body, tail or wings, which part, since those take a quarter of the damage plus 1
- if the dragon was still invulnerable from an earlier hit, how much longer, since only damage above that hit counts

On a kill: the time, whether it's a PB, the beds used and the session average. Every finished time is saved in `config/endpractice-times.txt`.

## How bed damage works in 1.0

A bed is an explosion of power 5. Before damaging entities the explosion doubles its power to 10, so it reaches 10 blocks. For each dragon part in range the damage is `(q*q + q) / 2 * 80 + 1`, where `q = (1 - distance / 10) * exposure`. Distance is measured to the part's position, which is the bottom center of its hitbox, and exposure is the share of the part's hitbox the explosion can see through blocks.

- The head is a 3x3x3 box whose bottom center is 5.5 blocks in front of the dragon's center. A bed right on that point does 81.
- Every other part takes a quarter of its damage plus 1, so at most 21.
- After a hit the dragon is invulnerable for 10 ticks, and a new hit in that time only counts for the amount above the last one.

This is the same math as 1.7.10. What differs is where the dragon is drawn. 1.7 runs the dragon on the built-in server and draws it a few ticks late, so the real head is ahead of the one on screen. 1.0 has no built-in server, so the hitbox is where the dragon is drawn.

## Blaze practice

**B** on the title screen (or Open blaze in the practice menu) practices an open blaze spawner, and **N** (Enclosed blaze) an enclosed one. Each attempt is a new world. You go straight into its nether through vanilla's own portal code and land in the doorway of a real fortress's blaze spawner room, facing the spawner, with the blaze kit. That's what a run has at the spawner: stone tools, 3 stacks of leaves, 48 oak planks, 5 gravel, 5 feathers, 3 string, 4 cobblestone, 16 steak, a crafting table and flint and steel, so you can overlap the crafts for the pit while you get rods. Blazes already there are cleared so every attempt starts the same.

The timer runs from arriving until you have the set number of blaze rods (7 by default, or 6, a setting) in your inventory. The HUD shows the time, your PB, the rods, the blazes you've killed and the share that dropped a rod (vanilla: 0 or 1 a blaze). PBs are kept per spawner type and number of rods in `config/endpractice-blaze.txt`.

**Blaze: tower crafts** (Settings, off by default) is for 1.0 tower runs: the attempt only finishes once you also have what the tower needs, a pressure plate, a fence gate, 3 ladders, 3 nether brick fences and a boat, all crafted from the kit and the fortress's nether bricks. The HUD shows what's still missing (`Need gate, ladders 1/3, boat`), and these attempts keep their own PBs.

The fortress spots come from the seed the same way the game places them, so only the fortresses themselves are generated. A spawner counts as enclosed when under 25% of the space 1 to 3 blocks around its room (sides and top, not counting the rest of the fortress) is air or lava, and as open when over 50% is. If none of the nearby fortresses has the type you asked for, you get the closest match and chat says so. **K** pressed twice saves your inventory as the blaze kit.

## Stats and the Lab

The practice menu groups the modes: **Endgame** (Full run, Tower build, Stronghold), **End Fight** (Dragon fight), **Aim Trainer** (Crystals, Random crystals) and **Blaze Fight** (Open spawner, Enclosed spawner), with each section's keys in grey next to its name. **Mode keys on the title screen** (Settings) turns the keys off on the title screen if you'd rather only use the menu; in game they still restart the mode you're in. Under the modes are four more buttons:

- **Settings**: keys, the HUD, the bed report, kits and the endgame times (below)
- **Kits**: the kit editor (below)
- **Stats**: your PBs. The End fight (with your average kill time over all kills and the last 10, and how many attempts you finished), every endgame mode's whole run and split PBs with the sum of best, the crystal trainer's best time, your best random crystal streak and the blaze PBs
- **Lab**: the endgame night testing tools: Enderman stats, the enderman tracker and its surface range, enderman outlines and aggro dot, the spawn and flush overlays, and the pit planner with its distance and key

### Kit editor

1.0 has no creative inventory, so **Kits** edits the four kits (End fight, Tower, Stronghold, Blaze) directly. Pick an item from the list on the right (type to search, scroll for more; left click gives a full stack, right click just one), then click inventory slots to put it there. Right click a slot to empty it. Click a slot with nothing picked to select it, then use -1, +1, Stack or Empty. Reset this kit goes back to the default, Done saves all four and Cancel throws the changes away. **K** pressed twice in an attempt still saves your current inventory as that mode's kit.

End fight attempts that you die in, leave, reset or abandon are saved in `config/endpractice-endfights.txt`, for the completion rate (kills from before 0.9.0 count as finished attempts). After every kill chat shows the all-time average, the last 10 and the completion rate.

## Settings

In game: the ender pearl button on the title screen, then Settings (or Lab for the testing tools). Click a setting to change it (shift+click goes back), click a key and press the new one. Done saves and applies straight away, Escape leaves without saving. The kits can be reset to the defaults there too.

The same settings are in `config/endpractice.properties`, created on first start. Restart the game after editing the file by hand.

| Setting | Default | |
|---|---|---|
| `keyStart` | `P` | Start an attempt (title screen) or restart (while practicing) |
| `keySaveKit` | `K` | Save your inventory as the kit |
| `kit` | sword, bow and 7 beds in the hotbar; arrows, water bucket, cobble, pearls, bread, pickaxe | `slot:itemId:count[:damage]`, comma separated. Slots 0-8 hotbar, 9-35 inventory, 36-39 armor |
| `keyEndgame` | `O` | Full endgame practice |
| `keyTower` | `I` | Tower build only |
| `keyStronghold` | `L` | Stronghold to dragon |
| `keyCrystals` | `C` | Crystal aim trainer |
| `keyRandomCrystals` | `R` | Random crystals: one at a time from random spots, with a streak |
| `modeKeys` | `true` | The mode keys on the title screen (off: start modes from the menu only) |
| `keyBlazeOpen` | `B` | Blaze practice at an open spawner |
| `keyBlazeEnclosed` | `N` | Blaze practice at an enclosed spawner |
| `blazeRods` | `7` | Rods to collect in blaze practice: 6 or 7 |
| `blazeTowerCrafts` | `false` | Blaze practice also needs a pressure plate, fence gate, 3 ladders, 3 nether brick fences and a boat |
| `blazeKit` | see Blaze practice | Same format as `kit` |
| `towerKit` | see above | Same format as `kit` |
| `strongholdKit` | 14 eyes, a pearl, tools, food, 16 cobble, leaves, wool, planks, crafting table, flint and steel | Same format as `kit` |
| `towerStartTime` | `10200` | World time a tower build starts at, in ticks (10200 is 8:30 into a run, -1 leaves it alone) |
| `nightTime` | `13200` | When night starts (13200 is 11:00 into a run). The tower build counts down to it |
| `pearlLuck` | `average` | Endgame practice pearls. `average`: exactly 1 every 2 endermen (the first one drops), so 24 kills is always 12. `lucky`: 2 every 3, so 24 kills is 16. `vanilla`: random 0 or 1 |
| `strongholdStartTime` | `22500` | World time stronghold attempts start at: the end of the night, turning to day |
| `endermanTracker` | `false` | Only with `pearlLuck=vanilla`. Save the endermen of every full endgame run at night end or the first eye, and show the averages |
| `endermanOutline` | `false` | Draw every enderman's hitbox as a pink box, seen through blocks, in practice worlds |
| `endermanAggroDot` | `false` | A red dot above every enderman that's angry at you, seen through blocks, in practice worlds |
| `flushOverlay` | `false` | HUD line with how many of the loaded monsters despawned when you last respawned on the tower |
| `spawnOverlay` | `false` | HUD lines with the loaded monsters against the mob cap, despawns, and the last spawn bursts |
| `endermanRange` | `87` | Surface endermen only count within this many blocks of you (0: anywhere loaded) |
| `pitPlanner` | `true` | Plan the pit for full endgame and tower runs and draw the route |
| `pitDistance` | `205` | How far from the tower the pit goes (180, 205, 220, 235, 250 or 256 in the menu) |
| `keyPitPlan` | `J` | Plan the pit again from where you stand |
| `hud` | `true` | Top left overlay |
| `bedReport` | `true` | Chat report after each bed |
| `bedReportRange` | `16.0` | Only report beds this close to the head |

## Building

Standard Fabric Loom build with Ornithe's Ploceus plugin and Feather mappings. Gradle needs JDK 25 or newer.

```
./gradlew build          # the jar is in build/libs
./gradlew runClient      # a development client with the mod
./gradlew runSelfTest    # starts an attempt and sets off beds around the dragon's head, results in the log
./gradlew runSelfTest -Pstress=60   # resets 60 times at random moments
./gradlew runSelfTest -Pendgame=6   # runs 6 endgame attempts through every split, cycling the three modes
./gradlew runSelfTest -Pcrystals=2  # 2 crystal trainer attempts, shooting arrows at the crystals
./gradlew runSelfTest -Prandomcrystals=20   # 20 shots in random crystals: hits, and misses high, low and to the side
```
