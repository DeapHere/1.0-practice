# OldschoolPrac

A practice mod for speedrunning Minecraft 1.0, the oldest version people run. It covers the stuff that's hard to practice in real runs:

- **End fight**: drops you into a fresh End with your kit, times the fight, and tells you after every bed how close it was to a perfect one
- **Double beds**: the dragon charges at you again and again, coming in at head height, with a countdown to the second bed and a score for each pair
- **Endgame**: the tower build, stronghold and dragon, with splits and a pit planner that picks your pit spot for you
- **Trainers**: crystal shooting (a full island, or one crystal at a time with a streak) and eye throws, where you find the stronghold and see how close your guess was
- **Blaze fight**: real open and enclosed fortress spawners, with your spawner kit
- **Overworld**: fresh worlds with trees, a cave with iron and a surface lava pool by spawn, with splits to the nether and furnace stats
- **Pit sandbox**: build pit designs at a held midnight, see mob paths, teleports and spawnable blocks, and time each design to compare them
- **PBs, stats and a kit editor**, since 1.0 can't switch game modes to fix your inventory
- **Singleplayer commands**: chat works in singleplayer, with /gamemode (survival, creative and a spectator mode 1.0 never had), /tp, /time, /summon and more

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
- for two beds close together, the double: see [Double beds](#double-beds)

When the dragon dies you get the time, whether it's a PB, how many beds you used, your average (all time and last 10) and your completion rate. Attempts you die in, leave or reset count against the completion rate.

### How bed damage works in 1.0

A bed is a power 5 explosion, which gets doubled to 10 for hurting entities, so it reaches 10 blocks. Each dragon part in range takes `(q*q + q) / 2 * 80 + 1`, where `q = (1 - distance / 10) * exposure`. Distance is to the bottom middle of the part's hitbox, and exposure is how much of the hitbox the explosion can see.

- The head is a 3x3x3 box whose bottom middle sits 5.5 blocks in front of the dragon. A bed right there does 81.
- Every other part takes a quarter of that plus 1, so 21 at most.
- After a hit the dragon is invulnerable for 10 ticks, and a hit during that only counts for whatever it does above the last one.

It's the same maths as 1.7.10. The difference is that 1.7 draws the dragon a few ticks behind where it really is, while in 1.0 the hitbox is right where you see it.

### Double beds

Two beds on one charge, both counting in full. Because of the 10 ticks of invulnerability, the second bed has to go off at least 10 ticks (half a second) after the first, or it only adds what it does over the first one.

A bed that hits also points the dragon at a spot 5 blocks ahead of it and a bit up, which slows it down, so it's still a few blocks past you 10 ticks later. That's why the first bed goes one block down in the ground where the head comes in, and the second goes behind you once you've turned round.

The height matters because the dragon can only drop about 0.6 blocks for every block it flies. Sent from 64 blocks out, from 50 up it's still about 20 up when it reaches you and flies right over. From 10 to 15 up it comes in at head height. In the mod's test runs (dragon sent from 64 out at a player standing still), 10 to 15 up gave a first bed of about 70 and the best second bed 25 to 50, and from 20 up the second bed fell off. Starting further back it needs a bit more height: from 96 out, 20 up comes in at head height (15 up missed the player 2 times in 4), and from 128 out, 25 to 35.

#### Double bed practice

**V** (or Double beds in the menu, next to Dragon fight) puts you on a flat spot of a fresh End island with no crystals and the double beds kit: an iron pickaxe for the holes, a stack of cobblestone and 7 beds. Change it in the kit editor (the Doubles tab), or set your inventory up in the mode and press **K** twice. Then it repeats:

1. `Charge in 3.0 s`: you face the middle of the island. Look the way you want the dragon to come from.
2. The dragon charges at you at full health from 96 blocks out the way you're looking, from about 20 up (18 to 22, a bit different each time). That gives you about 4 seconds to set up.
3. Bed 1 one block down as it comes in, turn round, and bed 2 behind you once the HUD says `Bed 2: now`.
4. Chat scores the pair and your inventory is reset to exactly the kit (the slot you have selected stays). Mined end stone and anything else that isn't in the kit is never picked up. After a double (two beds on the charge) you're moved to a different flat spot on the island, and the spot you left is put back the way it was (holes filled, fire, cobblestone and beds gone), so a spot you land on again is clean. If extra beds kill the dragon, it's straight back at full health in the same End. Then the next charge counts down.

**V** in the mode moves you to a new spot (the old one put back, you healed and your inventory reset to the kit) and starts the next countdown, say after a missed double. `/charge 25` changes the height (and charges). Start distance, start height, height spread, countdown and whether you move every rep are all under Double beds in Settings. By default you can't be hurt, so the beds' own blasts don't end a rep. Turn off **Double beds: can't be hurt** in Settings to take real damage: you're healed to full (and put out) before every charge, and if a bed kills you, you're straight back up on the spot with your kit for the next charge (no death screen or new world). The HUD shows the rep, your doubles this session, your best this session and your best ever. Every rep is saved in `config/endpractice-doublebeds.txt`, and the Stats screen shows your best double and your double rate.

#### What it tells you

- In any End fight, **`/charge [height]`** sends the dragon at you the same way. There it counts as a command, so that attempt isn't saved.
- After a bed hits, the HUD counts down `Bed 2: wait 6 ticks` until the dragon can take full damage again, then says `Bed 2: now`.
- Under the report of a bed that hits, a `Charge:` line gives the dragon's speed and how high its head was, and whether that charge is slow enough for a double.
- When two beds go off within 25 ticks, chat adds them up: `Double 73 + 68 = 141, 12 ticks apart (0.60 s)`. If the second was too soon, it says how much was lost to invulnerability and how many more ticks to wait. It keeps a count of doubles this session (both beds counted, and together more than a perfect single bed: over 81) and your best.

## Endgame

Three modes, all in the menu:

- **O**: full endgame, from tower build to portal room to entering the End to the dragon
- **I**: just the tower build
- **L**: stronghold to dragon, starting with the stronghold kit (14 eyes)

Structures are on here, so the stronghold is the seed's real one. You start on a flat plains or desert spot away from water, somewhere random 128 to 384 blocks from the stronghold, in any direction. 1.0 has an eye bug where eyes can lead you to a stronghold 600+ blocks away, so the mod only uses spots where eyes thrown from the tower and from around where your pit will be actually lead to the nearby one, even with Vanilla eye targeting on (off, eyes always fly to the closest portal room anyway).

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

In practice, eyes always fly to the closest portal room of the three strongholds, so there's no eye bug to work around. 1.0's own eye scores each stronghold as `dx·(1 + dy²) + dz²` (dy is the height gap) instead of the real distance, and until a stronghold has generated near you it treats all three as being at y 64, so a real eye can lead you to one 600+ blocks farther. Turn on **Vanilla eye targeting** in Settings to practise with that real 1.0 behaviour: eyes go where the game sends them, and about half the eye throw spots are then picked so an eye thrown from the ground hits the bug. Outside the practice modes (your own worlds) eyes always work the vanilla way.

Every throw is listed in chat with the height you threw from and which way it went. With **Eye trainer: coaching** on (Settings, on by default), after two throws far enough apart, chat shows where their lines cross and whether both eyes went to the same stronghold. It also says where the other two strongholds can be: 1.0 puts the three 120 degrees apart around 0,0, each 640 to 1152 blocks out and moved up to 112 blocks for its biome. From that it tells you whether either could be closer to you than the one you found. After your guess it says which stronghold your eyes led to and whether that was the closest one to where you started. With vanilla eye targeting on, each eye that hits the bug says so too. Turn coaching off to practise without help. **Eye trainer: eye angles** (Settings, off by default) also gives each eye's angle, in the same yaw Minecraft uses.

### The Keima method

A way to judge how far the stronghold is without F3. Throw an eye and line the side of your crosshair up with it while it hovers. Hold strafe and jump sideways 4 times without turning, then throw again. The second eye sits off from your crosshair by a few of its own pixels (the eye is 16 pixels wide, so it's its own ruler). The stronghold is about **3190 / pixels** blocks away.

That 3190 is 1.0's own number. Later versions use about 3800, but in 1.0 the eye hovers 12 blocks out and 4 sideways jumps (holding strafe and jump from standing, to the fourth landing) cover 8.3 blocks, both measured in game. A pixel of the eye is 1/32 of a block at 12 blocks, so distance = 32 × 12 × 8.3 / pixels.

In the eye throw trainer (**Eye trainer: Keima method** in Settings, on by default), a box pops up when your second eye drops: type how many pixels the crosshair moved (or, 100 or more, your distance guess in blocks). Chat then shows your estimate against the real distance from your second throw to the portal room, the error, the true pixel offset and how far you really moved sideways. `/keima <pixels>` answers it later if you skip the box. Your tries are kept in `config/endpractice-keima.txt`, with your best and the average of your last 10 in Stats. **Eye trainer: Keima offset** (off by default) shows the true pixel offset on the HUD as soon as you throw, for learning what each count looks like.

Chat also gives every estimate in sprint jumps, so you know how far to run: 1.0's sprint jump covers 3.75 blocks on flat ground (measured in game over 30 jumps, 12 ticks each), so 490 blocks is about 131 jumps.

**Calibration.** The 3190 assumes your 4 jumps cover exactly 8.3 blocks and that you count exactly, which nobody does, so the mod works out your own constant. Every try where you type a pixel count saves your count × the real distance, which is the constant that would have made that try perfect. The median of your last 10 is your calibrated constant, shown after each try and in Stats with its spread as "Your constant: ~XXXX. In a run, distance ~ XXXX / pixels." Once you have 5 tries the trainer uses it (turn **Keima: use my calibrated constant** off to use the fixed one instead, 3190 unless you change it in Settings or with `/keima fixed <n>`). Chat always shows the estimate with both, so you can see which is closer.

`/keima calibrate` runs a guided calibration in the eye throw trainer: 5 tries of throw, line up, 4 jumps, throw, count, then it tells you the constant to use in real runs. `/keima reset` clears your tries and calibration (they're kept in a `.old` file).

## Overworld

**U** (or Overworld in the menu) puts you in a fresh world with 4 logs, right by some trees, a cave with iron and gravel showing, and a lava pool on the surface. The timer starts the moment you move. Splits:

- **Stone pick**
- **Iron**: once you've smelted 6 iron (iron you've already turned into a bucket or flint and steel counts too). `overworldIron` changes the number
- **Bucket + flint & steel**
- **Portal lit**
- **Portal entered**: the moment you step into the portal, which ends the run, as long as you're holding 2 stacks of leaves (any kind) and have had at least 12 logs over the run (the 4 you spawn with count, and crafting them into planks doesn't take any off). If something's missing, chat says what, and the run finishes as soon as you have it while you're still in the portal. The HUD shows both counts

Each split counts on its own, so if you skip one (say you get by with less iron) the rest still split, and stepping into the portal always ends the run. Skipped splits show as skipped. Each split keeps its PB, and the HUD shows how long you've been smelting and how much of that you spent just standing at the furnace. When you get your iron, chat tells you how many furnaces you used (the most cooking at once, so one you pick up and put down somewhere else still counts as one), how long you waited, and roughly how much a second furnace would have saved if one furnace had ore queued up. The kit is the Overworld tab in Kits.

How it finds the worlds: lava lakes are placed with each chunk's own random number generator, which can be worked out from the seed alone, so the mod knows where surface lava pools are before generating anything. Then it builds the area around one and checks for trees, a cave you can walk into, and at least 6 exposed iron and 3 gravel. Finding one takes a few seconds.

## Pit sandbox

**H** (or Pit sandbox in the menu) drops you in a flat sand desert, in creative, with the time held at midnight, so you can build a pit and see how it does without the clock running out. The desert is flat sand in every direction (desert biome, no trees, caves, lakes or cacti), with spawn at 0 0 and the ground at y 64, so every test runs on the same terrain and the numbers compare pits rather than worlds. Turn off Flat desert in Settings for a random world with a flat plains or desert spawn instead. A saved sandbox world reopened as a normal world makes normal terrain past what was already generated. Animals and squid are cleared out since they only cost ticks (turn that off with No animals in Settings). **J** or `/pit plan` runs the pit planner from where you stand. It doesn't run on its own here because it loads a lot of terrain. Switch to `/gm s` when you want mobs to actually come for you. Only the sky and mob spawning see the held time, and the world clock keeps running, so water flows and sand and gravel fall as normal.

**M** spawns one enderman where you're looking (up to 64 blocks away), here or in your own worlds. **X** opens the saved pits menu (only in the sandbox): every design you've saved with Load and Delete (click Delete twice), and a name box with Save here to save the pit you're standing in. Hover a design to see how it did in its tests.

Things you can turn on and off with `/sandbox <name>` or in Settings:

- **paths**: the path every hostile mob within 64 blocks is walking, coloured by mob (endermen purple, creepers green, zombies teal, skeletons white, spiders red), with a post where it's headed and a thin red line to whatever it's after
- **teleports**: a pink line from where an enderman teleported to where it landed
- **spawns**: a coloured post where each monster spawned, for 30 seconds
- **spawnable**: crosses on every block within 24 of you that a monster could spawn on right now, purple where an enderman fits (3 blocks of room). Off by default since it's busy
- **rings**: two faint dashed rings at your feet: nothing spawns inside 24 blocks of you (green), and mobs past 32 can randomly despawn (yellow)

#### In your own worlds

`/test`, `/pit`, `/summon` and the **M** enderman key also work in your own singleplayer worlds (anything that isn't one of the practice modes). To get the overlays there too, type `/sandbox on`, or turn on Overlays in normal worlds in Settings. `/sandbox off` turns them off again. Be careful with `/pit load` in a world you care about, since it pastes the design over whatever is there.

### Saving pit designs

Stand where you'd stand in the pit and type `/pit save <name>`. It saves the blocks 9x9 around you from 44 below your feet to 6 above, so a normal pit fits. For a different box, stand on one corner and type `/pit pos1`, then on the opposite corner `/pit pos2` (a white outline shows the box), and `/pit clear` goes back to the default. `/pit load <name>` pastes the design where you're standing, in any sandbox world (or your own world), and puts you back on your spot facing the way you saved it. `/pit list` shows your designs and `/pit delete <name>` removes one. They're plain text files in `config/endpractice-pits/`, so you can share them.

### Testing designs

To compare designs, build your tower and pit, stand at the bottom of the pit and type `/test <name>`, like `/test 2 wide trench`. It works like a death reset in a run: you respawn at spawn (on top of your tower, if it's at spawn) the game's own way, keeping your inventory but switched to survival, and the time jumps to 11:20 into the day (13600) and runs for real. Monsters already around are cleared, since at 11:20 in a run the night hasn't started yet. Head to your pit the way you would in a run. The test lasts one full night, until 18:52 (22652), the first tick monsters can no longer spawn on open ground, and then ends by itself (`/test <name> 3` stops after 3 minutes, `/test stop` any time). If endermen are still after you when the night ends, it goes into overtime so you can finish them off: it ends once none has been after you for 5 seconds (2 minutes at most), and those kills and pearls count. If the time was held, it's held again afterwards. Pausing (Esc) stops the test clock until you're back in the game. The HUD shows the time as minutes:seconds into the day.

What matters is what you kill: the HUD during a test is just the time, how many endermen you've killed and the pearls you got, each with its rate per minute, and that's what's saved for each test. The summary at the end also gives how many surface endermen spawned within 87 blocks of you (on the ground layer, so under a tree counts and in a cave doesn't; the range is the surface enderman range in the Endgame Lab). Summoned mobs don't count. After `/pit load`, plain `/test` uses the design's name. Each test is saved. `/tests` (or Test stats in the **X** menu) opens a scrolling stats page with every design's totals and each of its tests, newest first; click one and press Delete twice to remove that test, or every test of a design. **Z** (or `/hud`) turns the HUD off and on. HUD... in the **X** menu (or Sandbox HUD... in Settings) picks which lines the sandbox HUD shows and their order: time of day, test timer, kills, pearls, surface endermen within 87 right now, surface spawns during a test, loaded mobs, light level and the pit planner. Each change shows straight away behind the editor; Escape throws them away.

To keep a sandbox world, type `/sandbox save <name>` (or type a name in the **X** menu and press Save the whole world): it's copied into your saves as "Pit <name>" and shows up in Singleplayer, where `/test`, `/pit` and **M** still work. Nothing in the sandbox counts towards your stats or PBs.

The HUD's enderman count is the surface ones within the surface enderman range (87 by default, in the Endgame Lab), with everything loaded, caves included, in grey after it.

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

1.0 has no creative mode, so this is how you change your kits. Pick a tab (End fight, Tower, Stronghold, Blaze, Eye throw, Overworld or Doubles), then use it like an inventory. Left click an item in the list on the right to pick one up, and keep clicking it for +1 each time (right click picks up a full stack, shift click puts a full stack straight into the first empty slot). Type to search and scroll for more. Left click a slot to put what you're holding down, pick up what's there, or swap the two; right click a slot to put down just one, or to empty it when your hand is empty. Click outside the slots or press Esc to drop what you're holding. -1, +1, Stack and Empty change what you're holding, or the selected slot. Done saves, Cancel doesn't, and Reset puts the default kit back.

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

- `/gamemode s`, `c` or `sp` (or `/gm`, `/gms`, `/gmc`, `/gmsp`): survival, creative or spectator. Spectator is new, since 1.0 doesn't have it: you fly through blocks and see through them (everything is lit up, so caves and strongholds show from inside the ground), mobs ignore you, and you can't break, place, use or pick up anything. Dying in creative no longer fills your hotbar with 9 random blocks
- `/tp x y z`, with `~` for where you are (`/tp ~ ~20 ~` goes up 20)
- `/time set day|noon|night|midnight|<ticks>`, `/time add <ticks>`, or `/time` to see it
- `/weather clear|rain|thunder`
- `/give <id> [count] [damage]`, like `/give 381 12` for 12 eyes of ender
- `/summon <mob> [count] [x y z]`, at the block you're looking at (up to 64 away) unless you give coordinates. Any 1.0 mob works, like `enderman`, `zombie`, `creeper`, `pigman`, `blaze`, `dragon`. `/summon list` shows them all
- `/killall` (or `/butcher`) removes every mob, `/killall monsters` just the monsters, or `/killall enderman` one kind
- `/mobs` counts what's loaded
- `/charge [height]` sends the dragon at you in the End fight (see Double beds)
- `/clear` empties your inventory, `/ws` (or `/spawn`) takes you to world spawn, on top of whatever is built there
- `/time lock [time]` holds the time of day (where it is, or the time you give), `/time unlock` lets it run again
- `/heal`, `/kill`, `/pos`, `/seed`, `/guess x z` and `/keima <pixels>|calibrate|reset|fixed <n>` (eye trainer) and `/help`
- In the pit sandbox: `/pit save|load|list|delete <name>`, `/pit pos1|pos2|clear|plan`, `/test <name> [minutes]`, `/test stop`, `/tests` and `/sandbox`

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
| `keyPitSandbox` | `H` | Pit sandbox |
| `keyOverworld` | `U` | Overworld practice |
| `overworldIron` | `6` | Overworld: iron for the iron split |
| `overworldLeaves`, `overworldLogs` | `128`, `12` | Overworld: leaves held and logs had before the portal finishes the run |
| `eyeAngles` | `false` | Eye trainer: say each eye's angle |
| `eyeCoaching` | `true` | Eye trainer: where your throws cross, where the other strongholds can be (and eye bug warnings with vanilla eye targeting) |
| `vanillaEyes` | `false` | Eyes go where 1.0 sends them, eye bug and all, instead of to the closest portal room |
| `keima` | `true` | Eye trainer: ask for your Keima pixel count after the second eye |
| `keimaOverlay` | `false` | Eye trainer: show the true Keima pixel offset on the HUD |
| `keimaCalibrated` | `true` | Keima: use your calibrated constant once you have 5 tries |
| `keimaFixed` | `3190` | Keima: the fixed constant (distance ~ this / pixels) |
| `sbPaths`, `sbTeleports`, `sbSpawns`, `sbRings` | `true` | Pit sandbox: mob paths, enderman teleports, new spawns, spawn and despawn rings |
| `sbSpawnable` | `false` | Pit sandbox: crosses on the blocks monsters can spawn on |
| `sbFlat` | `true` | Pit sandbox: a flat sand desert instead of a random world |
| `sbNoAnimals` | `true` | Pit sandbox: clears out animals and squid |
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
| `keyPitMenu` | `X` | Saved pits menu (pit sandbox) |
| `keyEnderman` | `M` | Spawn an enderman where you're looking (pit sandbox and your own worlds) |
| `sbInWorlds` | `false` | Pit sandbox overlays in your own worlds too |
| `keyHud` | `Z` | Turns the HUD on and off |
| `sbHud` | `title,test,surface,kills,pearls,-spawned,-loaded,-light,planner` | Pit sandbox HUD lines in order, `-` for off (the HUD editor sets it) |
| `hud` | `true` | Top left overlay |
| `bedReport` | `true` | Chat report after each bed |
| `bedReportRange` | `16.0` | Only report beds this close to the head |
| `doubleBedCue` | `true` | HUD countdown to when a second bed counts in full |
| `doubleBedStartHeight` | `20` | How high the dragon starts in double bed practice and for `/charge` |
| `doubleBedDistance` | `96` | How far out it starts |
| `doubleBedSpread` | `2` | Each charge starts up to this much higher or lower |
| `doubleBedCountdown` | `3` | Seconds before each charge |
| `doubleBedMoveEveryRep` | `false` | A new spot after every rep, not just after doubles |
| `doubleBedProtect` | `true` | You can't be hurt in double bed practice |
| `keyCharge` | `V` | Double bed practice (in it: new spot and next charge) |

Your times, PBs and runs are saved in the `config/endpractice-*.txt` files. Delete one to start that stat over.

## With Oldschool Ranked

OldschoolPrac and Oldschool Ranked run fine together. While ranked is doing anything (queued, in a private room, loading, in a match, or on the result before you leave the world), OldschoolPrac steps aside completely: any practice mode or pit sandbox stops, its commands, chat and keys do nothing, its HUD, rings, paths and pit planner line are hidden, spectator is switched back to survival, and the practice button is gone from the title screen. Everything comes back once ranked is idle again.

## Building it yourself

It's a standard Fabric Loom build with Ornithe's Ploceus plugin and Feather mappings. Gradle needs JDK 25 or newer.

```
./gradlew build          # the jar ends up in build/libs
./gradlew runClient      # a dev client with the mod
./gradlew runSelfTest    # automated tests (see build.gradle for the options)
```
