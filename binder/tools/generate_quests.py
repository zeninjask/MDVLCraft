"""Generates the pack's FTB Quests book: six chapters, one per boss tier (First Steps, then Wanderer to Legend).
Then come the Gates, Food (Farmer's Delight) and Brewing (Ars Elixirum) chapters (row_chapter): each its own thing, open from
the start, numbered on its own in the quest log with a letter (001G..., 001F..., 001B...).

Each tier chapter has a top row of progress quests (kill counts, an experience level, places to find) ending in a
capstone that opens the next chapter, and below it one column per boss: three lead-up quests themed on that boss, the
last of which rewards a compass to the boss's lair (/mdvlquest compass, command/QuestCompass) when it lives in a
structure (a Boss Sigil to an arena, integration/BossArena, when the pack doesn't generate that lair), then the kill. Capstones need a few of the tier's bosses (min_required_dependencies), never all of them.

Writes:
  base/{client,server}/overrides/config/ftbquests/quests/  the quest book (SNBT, read by FTB Quests on the server)
  src/main/resources/data/mdvlcraft/advancements/quests/   hidden "reach experience level N" advancements
  src/main/resources/data/mdvlcraft/advancements/quests/gates/  hidden gate-progress advancements (gate/GateAdvancements)
  src/main/resources/data/mdvlcraft/loot_tables/quests/    the boss reward crates, one per tier

Usage: python3 binder/tools/generate_quests.py   (build_packs.py also writes the book into its base before packing)
"""
import hashlib, json, os, shutil, sys
from pathlib import Path

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.join(HERE, '..', '..')
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'data', 'mdvlcraft')
OVERWORLD, NETHER, END = 'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'
LEVELS = [10, 15, 25, 30, 40, 50]


def qid(key):
    """A stable FTB Quests id for the key: 16 hex digits that fit a signed long, as FTB Quests parses them."""
    return '%016X' % (int(hashlib.sha1(key.encode()).hexdigest()[:16], 16) & 0x7FFFFFFFFFFFFFFF)


# Task and reward builders: dicts in FTB Quests' own keys.
def kill(entity, n, icon=None):
    return {'type': 'kill', 'entity': entity, 'value': ('L', n)}


def item(item_id, n=1):
    return {'type': 'item', 'item': item_id, 'count': ('L', n)}


def biome(b):
    return {'type': 'biome', 'biome': b}


def structure(s):
    return {'type': 'structure', 'structure': s}


def dimension(d):
    return {'type': 'dimension', 'dimension': d}


def level(n):
    return {'type': 'advancement', 'advancement': f'mdvlcraft:quests/level_{n}', 'criterion': ''}


def advancement(a):
    return {'type': 'advancement', 'advancement': a, 'criterion': ''}


def mob_kills(n):
    return {'type': 'stat', 'stat': 'minecraft:mob_kills', 'value': n}


def checkmark():
    return {'type': 'checkmark'}


def give(item_id, n=1):
    return {'type': 'item', 'item': item_id, 'count': n}


def xp(points):
    return {'type': 'xp', 'xp': points}


def command(cmd, title, icon):
    """A command reward named and drawn as what it gives (FTB Quests otherwise shows it as "Command")."""
    return {'type': 'command', 'command': cmd, 'elevate_perms': True, 'silent': True, 'title': title, 'icon': icon}


# Permanent stat bonuses, small: a custom reward that only shows the bonus; compat/ftbquests/QuestBonuses reads the tag
# and adds the attribute modifier for every completed quest at login, respawn and completion, so it is retroactive and
# never stacks.
BONUS_NAMES = {'minecraft:generic.max_health': ('Max Health', 'minecraft:golden_apple', '%g'),
               'irons_spellbooks:max_mana': ('Max Mana', 'minecraft:lapis_lazuli', '%g'),
               'irons_spellbooks:cooldown_reduction': ('Cooldown Reduction', 'minecraft:clock', 'pct')}


def bonus(attribute, amount):
    name, icon, fmt = BONUS_NAMES[attribute]
    shown = f'{amount * 100:g}%' if fmt == 'pct' else fmt % amount
    return {'type': 'custom', 'title': f'+{shown} {name} (permanent)', 'icon': icon, 'tags': [f'mdvlcraft_bonus={attribute}={amount}']}


HEALTH, MANA, COOLDOWN = 'minecraft:generic.max_health', 'irons_spellbooks:max_mana', 'irons_spellbooks:cooldown_reduction'


def crate(tier):
    return command(f'/loot give {{p}} loot mdvlcraft:quests/tier{tier}', f'Tier {tier} Reward Crate', 'minecraft:chest')


def compass(struct, dim, name):
    return command(f'/mdvlquest compass {{p}} {struct} {dim} {name}', f"Compass to {name}'s Lair", 'minecraft:compass')


# Lairs the pack doesn't generate (base kubejs empties their structure sets): their bosses are fought in an arena instead,
# reached with a Boss Sigil (integration/BossArena; the party comes along, integration/PartyTravel).
ARENA_LAIRS = {'traveloptics:void_cathedral'}


# Cataclysm lairs: Cataclysm Dimension moves each into a world of its own (at its 0,0), entered by sneaking and using the
# lair's Cataclysm eye (crafted; the party comes along, integration/PartyTravel); a compass in the old dimension finds nothing.
CATACLYSM_EYES = {
    'cataclysm:ancient_factory': ('cataclysm:mech_eye', 'Mech Eye', 'the Forge of Aeons'),
    'cataclysm:soul_black_smith': ('cataclysm:monstrous_eye', 'Monstrous Eye', "the Soul's Anvil"),
    'cataclysm:burning_arena': ('cataclysm:flame_eye', 'Flame Eye', "the Inferno's Maw"),
    'cataclysm:cursed_pyramid': ('cataclysm:desert_eye', 'Desert Eye', "the Pharaoh's Bane"),
    'cataclysm:frosted_prison': ('cataclysm:cursed_eye', 'Cursed Eye', 'the Eternal Frosthold'),
    'cataclysm:sunken_city': ('cataclysm:abyss_eye', 'Abyss Eye', 'the Abyssal Depths'),
    'cataclysm:ruined_citadel': ('cataclysm:void_eye', 'Void Eye', 'the Bastion Lost'),
    'cataclysm:acropolis': ('cataclysm:storm_eye', 'Storm Eye', 'the Sanctum Fallen'),
}


def arena_lair(struct):
    return struct.startswith('legendary_monsters:') or struct in ARENA_LAIRS


def sigil(entity, name):
    nbt = json.dumps({'Boss': entity, 'BossName': name}).replace('"Boss"', 'Boss').replace('"BossName"', 'BossName')
    return command(f'/give {{p}} mdvlcraft:boss_sigil{nbt}', f'Boss Sigil: {name}', 'mdvlcraft:boss_sigil')


ACCEPT = {'type': 'checkmark', 'title': 'Accept Quest'}


class Quest:
    """A quest; unless accept=False its first task is the Accept Quest checkmark and its tasks count in order, so only one
    quest is in progress at a time (compat/ftbquests/ActiveQuest)."""

    def __init__(self, key, title, x, y, tasks, rewards, desc=(), deps=(), icon=None, min_deps=0, shape=None, size=None, accept=True):
        self.id = qid(key)
        self.title, self.x, self.y = title, x, y
        self.accept = accept
        self.tasks, self.rewards, self.desc = ([ACCEPT] if accept else []) + list(tasks), rewards, list(desc)
        self.deps, self.icon, self.min_deps, self.shape, self.size = list(deps), icon, min_deps, shape, size


# ---------------------------------------------------------------------------------------------------------------
# Bosses: (key, name, entity, lead-up quests [(title, task(s), reward(s), description)], lair (structure, dimension) or
# None, kill description). The third lead-up quest's reward gets the compass added when the boss has a lair.
# ---------------------------------------------------------------------------------------------------------------
B = []


def boss(tier, key, name, entity, icon, steps, lair, hint):
    B.append(dict(tier=tier, key=key, name=name, entity=entity, icon=icon, steps=steps, lair=lair, hint=hint))


# Tier 1 - Wanderer (iron gear)
boss(1, 'dune_sentinel', 'the Dune Sentinel', 'legendary_monsters:dune_sentinel', 'minecraft:sandstone', [
    ('Sands of the Sentinel', [biome('minecraft:desert')], [give('minecraft:bread', 8)], 'Something ancient stirs under the dunes. Head into a desert.'),
    ('Husk Hunt', [kill('minecraft:husk', 10)], [give('minecraft:golden_carrot', 6)], 'Husks roam where the Sentinel sleeps. Thin them out.'),
    ('Shield Wall', [item('minecraft:shield')], [xp(100)], 'The Sentinel hits hard. Carry a shield.'),
], ('legendary_monsters:ruined_pyramid', OVERWORLD), 'It guards a Ruined Pyramid in the desert.')
boss(1, 'overgrown_colossus', 'the Overgrown Colossus', 'legendary_monsters:overgrown_colossus', 'minecraft:moss_block', [
    ('Into the Green', [biome('#minecraft:is_jungle')], [give('minecraft:bread', 8)], 'A giant of moss sleeps somewhere in the jungle. Head into it.'),
    ('Clear the Undergrowth', [kill('minecraft:spider', 10)], [give('minecraft:golden_carrot', 6)], 'Spiders nest in the canopy. Clear a path.'),
    ('Vine Cutter', [item('minecraft:iron_axe')], [xp(100)], 'You will need an axe to cut through the vines.'),
], ('legendary_monsters:mossy_temple', OVERWORLD), 'It sleeps in a Mossy Temple deep in the jungle.')
boss(1, 'frostbitten_golem', 'the Frostbitten Golem', 'legendary_monsters:frostbitten_golem', 'minecraft:packed_ice', [
    ('Frozen Wastes', [biome('#forge:is_snowy')], [give('minecraft:bread', 8)], 'Cold winds carry the groan of a frozen golem. Find snow.'),
    ('Stray Arrows', [kill('minecraft:stray', 10)], [give('minecraft:arrow', 32)], 'Strays haunt the snowfields. Put them down.'),
    ('Warm Hearth', [item('minecraft:campfire')], [xp(100)], 'Bring fire to fight the frost.'),
], ('legendary_monsters:frostbitten_temple', OVERWORLD), 'It waits in a Frostbitten Temple in snowy plains, taiga or ice spikes.')

# Tier 2 - Adventurer (iron to diamond)
boss(2, 'possessed_paladin', 'the Possessed Paladin', 'legendary_monsters:posessed_paladin', 'minecraft:golden_helmet', [
    ('A Fallen Kingdom', [biome('minecraft:plains')], [give('minecraft:cooked_beef', 8)], 'The tale of a fallen kingdom starts on the plains. Its knight never left.'),
    ('Knightly Steel', [item('minecraft:iron_chestplate')], [give('minecraft:iron_ingot', 6)], 'Meet steel with steel.'),
    ('Provisions', [item('minecraft:cooked_beef', 16)], [xp(200)], 'The kingdom is large. Pack food.'),
], ('legendary_monsters:collapsed_kingdom', OVERWORLD), 'It haunts the arena of a Collapsed Kingdom on plains and in forests.')
boss(2, 'cloud_golem', 'the Cloud Golem', 'legendary_monsters:cloud_golem', 'minecraft:white_wool', [
    ('Peak Season', [biome('#minecraft:is_mountain')], [give('minecraft:cooked_beef', 8)], 'Climb the mountains, where the tale of the Cloud Golem begins.'),
    ('Soft Landing', [item('minecraft:water_bucket')], [give('minecraft:feather', 16)], 'A long fall waits for the careless. Carry water.'),
    ('Night Flyers', [kill('minecraft:phantom', 5)], [xp(200)], 'Phantoms circle the heights. Clear the sky.'),
], ('legendary_monsters:cloudy_temple', OVERWORLD), 'It lives in a Cloudy Temple on mountain peaks, meadows and cherry groves.')
boss(2, 'ancient_guardian', 'the Ancient Guardian', 'legendary_monsters:ancient_guardian', 'minecraft:mossy_stone_bricks', [
    ('Dark Woods', [biome('minecraft:dark_forest')], [give('minecraft:cooked_beef', 8)], 'The trail of the Ancient Guardian starts in the dark forest.'),
    ('Bonebreaker', [kill('minecraft:skeleton', 20)], [give('minecraft:arrow', 32)], 'Skeletons guard the approach.'),
    ('Diamond Edge', [item('minecraft:diamond_sword')], [xp(200)], 'Only a diamond blade will crack its stone.'),
], ('legendary_monsters:ancient_stronghold', OVERWORLD), 'It guards an Ancient Stronghold in dark forest and taiga.')
boss(2, 'elder_guardian', 'the Elder Guardian', 'minecraft:elder_guardian', 'minecraft:prismarine', [
    ('Deep Water', [biome('#minecraft:is_deep_ocean')], [give('minecraft:cooked_cod', 8)], 'Ocean monuments sit on the floor of deep oceans.'),
    ('Drowned Sailors', [kill('minecraft:drowned', 15)], [give('minecraft:prismarine_shard', 8)], 'The drowned swarm near the monument.'),
    ('Sea Legs', [item('minecraft:oak_boat')], [xp(200)], 'Bring a boat for the crossing.'),
], ('betteroceanmonuments:ocean_monument', OVERWORLD), 'It rules an Ocean Monument in deep oceans.')
boss(2, 'dead_king', 'the Dead King', 'irons_spellbooks:dead_king', 'minecraft:wither_skeleton_skull', [
    ('Restless Dead', [kill('minecraft:zombie', 25)], [give('minecraft:bread', 8)], 'The dead walk more often near the Catacombs.'),
    ('And So a Quest Begins', [item('irons_spellbooks:wayward_compass')], [give('minecraft:torch', 32)], 'Craft a Wayward Compass. Its trapped soul knows the way.'),
    ('Light the Crypt', [item('minecraft:torch', 32)], [xp(200)], 'The Catacombs are dark and deep. Bring light.'),
], ('irons_spellbooks:catacombs', OVERWORLD), 'His throne is at the end of the Catacombs, buried between Y -40 and 20.')
boss(2, 'ancient_knight', 'the Ancient Knight', 'irons_spellbooks:citadel_keeper', 'minecraft:chiseled_stone_bricks', [
    ('Hedge Magic', [kill('minecraft:witch', 5)], [give('minecraft:glass_bottle', 8)], 'Witches know the old wards. Take their secrets.'),
    ('Scholar', [item('minecraft:book', 8)], [give('minecraft:lapis_lazuli', 16)], 'Study the Lost Citadel before you go.'),
    ('Plate Armour', [item('minecraft:diamond_chestplate')], [xp(200)], 'The Knight never tires. Armour up.'),
], ('irons_spellbooks:citadel', OVERWORLD), 'It guards the Lost Citadel. Its magic ward needs an Enchanted Ward Amulet to pass.')

# Tier 3 - Veteran (diamond, the Nether)
boss(3, 'harbinger', 'the Harbinger', 'cataclysm:the_harbinger', 'minecraft:redstone_block', [
    ('Machine Parts', [item('minecraft:redstone', 32)], [give('minecraft:iron_ingot', 8)], 'A forgotten factory hums deep underground.'),
    ('Delver', [biome('minecraft:dripstone_caves')], [give('minecraft:torch', 32)], 'Go deep. The factory lies under the surface.'),
    ('Golden Reserve', [item('minecraft:golden_apple', 2)], [xp(300)], 'The Harbinger fights to the last. Bring golden apples.'),
], ('cataclysm:ancient_factory', OVERWORLD), 'It is built deep inside an Ancient Factory underground.')
boss(3, 'netherite_monstrosity', 'the Netherite Monstrosity', 'cataclysm:netherite_monstrosity', 'minecraft:netherite_block', [
    ('Into the Inferno', [dimension(NETHER)], [give('minecraft:cooked_porkchop', 8)], 'The Soul Blacksmith forge burns somewhere in the Nether.'),
    ('Hog Hunter', [kill('minecraft:hoglin', 10)], [give('minecraft:gold_ingot', 8)], 'Hoglins charge in the crimson woods.'),
    ('Heat Shield', [item('minecraft:magma_cream', 8)], [xp(300)], 'Brew fire resistance before you face it.'),
], ('cataclysm:soul_black_smith', NETHER), 'It works the forge of the Soul Blacksmith in the Nether.')
boss(3, 'ignis', 'Ignis', 'cataclysm:ignis', 'minecraft:blaze_powder', [
    ('Flame Hunter', [kill('minecraft:blaze', 10)], [give('minecraft:gold_ingot', 8)], 'Blazes carry the fire that feeds Ignis.'),
    ('Blaze Rods', [item('minecraft:blaze_rod', 8)], [give('minecraft:magma_cream', 4)], 'Gather rods for fire resistance.'),
    ('Gilded Offering', [item('minecraft:gold_ingot', 16)], [xp(300)], 'Piglins guard the arena. Gold keeps them calm.'),
], ('cataclysm:burning_arena', NETHER), 'It waits in the Burning Arena over the lava lakes of the Nether wastes.')
boss(3, 'lava_eater', 'the Lava Eater', 'legendary_monsters:lava_eater', 'minecraft:magma_block', [
    ('Wasteland', [biome('minecraft:nether_wastes')], [give('minecraft:cooked_porkchop', 8)], 'The Lava Eater surfaces in the Nether wastes.'),
    ('Slime and Fire', [kill('minecraft:magma_cube', 10)], [give('minecraft:magma_cream', 4)], 'Magma cubes swarm around its pools.'),
    ('Lava Sample', [item('minecraft:lava_bucket')], [xp(300)], 'Bring back a bucket of what it eats.'),
], ('legendary_monsters:lava_eater_spawn', NETHER), 'It nests in the Nether wastes.')
boss(3, 'warped_fungussus', 'the Warped Fungussus', 'legendary_monsters:warped_fungussus', 'minecraft:warped_wart_block', [
    ('Warped Woods', [biome('minecraft:warped_forest')], [give('minecraft:cooked_porkchop', 8)], 'Something grows huge in the warped forest.'),
    ('Tall Strangers', [kill('minecraft:enderman', 10)], [give('minecraft:ender_pearl', 4)], 'Endermen roam the warped forest.'),
    ('Fungal Samples', [item('minecraft:warped_fungus', 16)], [xp(300)], 'Gather warped fungi to track its spores.'),
], ('legendary_monsters:warped_fungussus_nest', NETHER), 'Its nest is in the warped forest.')
boss(3, 'skeletosaurus', 'the Skeletosaurus', 'legendary_monsters:skeletosaurus', 'minecraft:bone_block', [
    ('Valley of Souls', [biome('minecraft:soul_sand_valley')], [give('minecraft:cooked_porkchop', 8)], 'Giant bones lie in the soul sand valley.'),
    ('Bone Pile', [kill('minecraft:skeleton', 15)], [give('minecraft:arrow', 32)], 'Skeletons spawn thick in the valley.'),
    ('Bone Collector', [item('minecraft:bone', 32)], [xp(300)], 'Collect bones to follow the trail.'),
], ('legendary_monsters:skeletosaurus_nest', NETHER), 'Its nest is in the soul sand valley.')
boss(3, 'withered_abomination', 'the Withered Abomination', 'legendary_monsters:withered_abomination', 'minecraft:soul_soil', [
    ('Blackened Bones', [kill('minecraft:wither_skeleton', 15)], [give('minecraft:coal', 16)], 'Wither skeletons stalk the Nether fortresses.'),
    ('Soul Ground', [item('minecraft:soul_soil', 16)], [give('minecraft:cooked_porkchop', 8)], 'The remains stand on soul soil.'),
    ('Antidote', [item('minecraft:milk_bucket')], [xp(300)], 'Milk cures the wither. Bring a bucket.'),
], ('legendary_monsters:soul_fortress_remains', NETHER), 'It lurks in the Soul Fortress Remains in the soul sand valley and Nether wastes.')

# Tier 4 - Champion (netherite)
boss(4, 'wither', 'the Wither', 'minecraft:wither', 'minecraft:nether_star', [
    ('Three Skulls', [item('minecraft:wither_skeleton_skull', 3)], [give('minecraft:golden_apple', 1)], 'Wither skeletons drop the skulls you need.'),
    ('Soul Sand', [item('minecraft:soul_sand', 4)], [give('minecraft:milk_bucket', 1)], 'The Wither is built on soul sand.'),
    ('Golden Stock', [item('minecraft:golden_apple', 4)], [xp(400)], 'Fight it far from home, with golden apples.'),
], None, 'Build it from soul sand and three wither skeleton skulls, far from your base.')
boss(4, 'ancient_remnant', 'the Ancient Remnant', 'cataclysm:ancient_remnant', 'minecraft:chiseled_sandstone', [
    ('Tomb Raider', [kill('minecraft:husk', 20)], [give('minecraft:golden_carrot', 8)], 'A cursed pyramid holds a desert colossus.'),
    ('Netherite Pick', [item('minecraft:netherite_pickaxe')], [give('minecraft:tnt', 4)], 'Its tomb is sealed tight.'),
    ('Apples of Gold', [item('minecraft:golden_apple', 4)], [xp(400)], 'The Remnant is relentless. Bring golden apples.'),
], ('cataclysm:cursed_pyramid', OVERWORLD), 'It sleeps in the Cursed Pyramid in the desert.')
boss(4, 'maledictus', 'Maledictus', 'cataclysm:maledictus', 'minecraft:blue_ice', [
    ('Snowblind', [biome('minecraft:snowy_plains')], [give('minecraft:cooked_beef', 8)], 'A frozen prison holds a cursed knight in the snowy plains.'),
    ('Frozen Arrows', [kill('minecraft:stray', 20)], [give('minecraft:arrow', 32)], 'Strays guard the prison.'),
    ('Blue Ice', [item('minecraft:blue_ice', 8)], [xp(400)], 'Gather blue ice to find the coldest place.'),
], ('cataclysm:frosted_prison', OVERWORLD), 'It is chained in the Frosted Prison in the snowy plains.')
boss(4, 'leviathan', 'the Leviathan', 'cataclysm:the_leviathan', 'minecraft:heart_of_the_sea', [
    ('Abyss', [biome('#minecraft:is_deep_ocean')], [give('minecraft:cooked_cod', 8)], 'A sunken city lies in the deep ocean.'),
    ('Drowned Horde', [kill('minecraft:drowned', 30)], [give('minecraft:prismarine_crystals', 8)], 'The drowned swarm above the city.'),
    ('Turtle Shell', [item('minecraft:turtle_helmet')], [xp(400)], 'The Leviathan can only be hurt in water. Breathe easy.'),
], ('cataclysm:sunken_city', OVERWORLD), 'It sleeps in the Sunken City in deep oceans. It can only be hurt in water.')
boss(4, 'ender_dragon', 'the Ender Dragon', 'minecraft:ender_dragon', 'minecraft:dragon_head', [
    ('Eyes of Ender', [item('minecraft:ender_eye', 12)], [give('minecraft:ender_pearl', 8)], 'Twelve eyes open the portal.'),
    ('Arrow Storm', [item('minecraft:arrow', 64)], [give('minecraft:spectral_arrow', 16)], 'Shoot the crystals from afar.'),
    ('Bowyer', [item('minecraft:bow')], [xp(400)], 'Bring a bow for the crystals.'),
], ('betterstrongholds:stronghold', OVERWORLD), 'Find a stronghold and its End portal.')
boss(4, 'ender_guardian', 'the Ender Guardian', 'cataclysm:ender_guardian', 'minecraft:purpur_block', [
    ('The Far Lands', [dimension(END)], [give('minecraft:cooked_beef', 8)], 'A ruined citadel floats on the outer End islands.'),
    ('Outer Island Patrol', [kill('minecraft:enderman', 20)], [give('minecraft:ender_pearl', 8)], 'Endermen crowd the outer islands.'),
    ('Chorus', [item('minecraft:chorus_fruit', 16)], [xp(400)], 'Chorus fruit gets you out of tight spots.'),
], ('cataclysm:ruined_citadel', END), 'It guards the Ruined Citadel on the outer End highlands.')
boss(4, 'endersent', 'the Endersent', 'legendary_monsters:endersent', 'minecraft:end_stone_bricks', [
    ('Highlands', [biome('minecraft:end_highlands')], [give('minecraft:cooked_beef', 8)], 'The trail of the Endersent leads to the End highlands.'),
    ('Tall Shadows', [kill('minecraft:enderman', 25)], [give('minecraft:ender_pearl', 8)], 'The Endersent commands the endermen.'),
    ('Pearls', [item('minecraft:ender_pearl', 16)], [xp(400)], 'Gather pearls to keep up with it.'),
], ('legendary_monsters:ancient_tower_remains', END), 'It haunts the Ancient Tower Remains on the outer End islands.')
boss(4, 'annihilation_pursuer', 'the Annihilation Pursuer', 'legendary_monsters:annihilation_pursuer', 'minecraft:end_rod', [
    ('Midlands', [biome('minecraft:end_midlands')], [give('minecraft:cooked_beef', 8)], 'Something not of this world was seen over the End midlands.'),
    ('Shulker Hunt', [kill('minecraft:shulker', 10)], [give('minecraft:shulker_shell', 2)], 'Shulkers guard the outer cities.'),
    ('Shells', [item('minecraft:shulker_shell', 8)], [xp(400)], 'Gather shulker shells.'),
], ('legendary_monsters:space_station', END), 'It guards a Space Station on the outer End islands.')

# Tier 5 - Legend (best gear)
boss(5, 'scylla', 'Scylla', 'cataclysm:scylla', 'minecraft:trident', [
    ('Warm Waters', [biome('minecraft:warm_ocean')], [give('minecraft:cooked_cod', 16)], 'An acropolis rises from the warm ocean.'),
    ('Guardian Purge', [kill('minecraft:guardian', 20)], [give('minecraft:prismarine_shard', 16)], 'Guardians patrol the warm waters.'),
    ('Conduit', [item('minecraft:conduit')], [xp(600)], 'A conduit lets you fight underwater.'),
], ('cataclysm:acropolis', OVERWORLD), 'She rules the Acropolis in the warm ocean.')
boss(5, 'warden', 'the Warden', 'minecraft:warden', 'minecraft:sculk_shrieker', [
    ('Deep Dark', [biome('minecraft:deep_dark')], [give('minecraft:white_wool', 16)], 'Ancient cities lie in the deep dark.'),
    ('Soft Steps', [item('minecraft:white_wool', 16)], [give('minecraft:golden_carrot', 16)], 'Wool muffles your steps around sculk.'),
    ('Echoes', [item('minecraft:echo_shard', 4)], [xp(600)], 'Echo shards are found in ancient cities.'),
], ('minecraft:ancient_city', OVERWORLD), 'It rises in Ancient Cities in the deep dark.')
boss(5, 'nightwarden', 'the Nightwarden', 'traveloptics:the_nightwarden', 'minecraft:crying_obsidian', [
    ('The Far End', [dimension(END)], [give('minecraft:cooked_beef', 16)], 'The Nightwarden is imprisoned beyond the End.'),
    ('Cathedral Watch', [kill('minecraft:enderman', 30)], [give('minecraft:ender_pearl', 16)], 'Clear the endermen of the End.'),
    ('Netherite Blade', [item('minecraft:netherite_sword')], [xp(600)], 'Nothing less than netherite.'),
], ('traveloptics:void_cathedral', END), 'It is imprisoned in the Void Cathedral on the End highlands.')
boss(5, 'wraithon', 'the Wraithon', 'wraithon:wraithon', 'minecraft:netherite_ingot', [
    ('Star of the Wither', [item('minecraft:nether_star')], [give('minecraft:netherite_scrap', 2)], 'The Wither drops a nether star.'),
    ('Egg of the Dragon', [item('minecraft:dragon_egg')], [give('minecraft:golden_apple', 4)], 'Take the egg from the End fountain.'),
    ('Showdown Edict', [item('wraithon:showdown_edict')], [xp(600)], 'Craft the Showdown Edict: six netherite ingots, the dragon egg and a nether star.'),
], None, 'Use the Showdown Edict to enter its sanctum.')


# ---------------------------------------------------------------------------------------------------------------
# Chapters
# ---------------------------------------------------------------------------------------------------------------
TIERS = {
    1: dict(name='Wanderer', file='wanderer', icon='minecraft:iron_sword', level=15, kills=50, min_bosses=2,
            subtitle='Iron gear. Your first bosses.'),
    2: dict(name='Adventurer', file='adventurer', icon='minecraft:diamond_sword', level=25, kills=150, min_bosses=3,
            subtitle='Diamond gear. Bosses across the Overworld.'),
    3: dict(name='Veteran', file='veteran', icon='minecraft:blaze_rod', level=30, kills=300, min_bosses=3,
            subtitle='The Nether and its giants.'),
    4: dict(name='Champion', file='champion', icon='minecraft:dragon_head', level=40, kills=500, min_bosses=4,
            subtitle='Netherite gear. The End awaits.'),
    5: dict(name='Legend', file='legend', icon='minecraft:nether_star', level=50, kills=1000, min_bosses=4,
            subtitle='The strongest things in the world.'),
}

PROGRESS = {
    1: [('Desert Patrol', [kill('minecraft:husk', 15)], 'Husks roam the deserts.'),
        ('Stray Hunter', [kill('minecraft:stray', 15)], 'Strays haunt the snow.'),
        ('Wanderlust', [biome('minecraft:desert'), biome('#minecraft:is_jungle'), biome('#forge:is_snowy')], 'See the world: a desert, a jungle and a snowy land.')],
    2: [('Raid Breaker', [kill('minecraft:pillager', 20)], 'Pillagers threaten the villages.'),
        ('Deep Trouble', [kill('minecraft:drowned', 15)], 'The drowned rise at night near water.'),
        ('Arcane Tools', [item('minecraft:enchanting_table')], 'Enchant your gear.'),
        ('Diamond Age', [item('minecraft:diamond_pickaxe')], 'Diamond tools for diamond work.')],
    3: [('Blaze Runner', [kill('minecraft:blaze', 30)], 'Blazes guard the Nether fortresses.'),
        ('Wither Bones', [kill('minecraft:wither_skeleton', 20)], 'Wither skeletons drop the skulls you will need later.'),
        ('Burning Arena Guards', [kill('cataclysm:ignited_revenant', 10)], 'Ignited Revenants guard the Burning Arena.'),
        ('Fortress Found', [structure('betterfortresses:fortress')], 'Find a Nether fortress.'),
        ('Bastion Breaker', [structure('minecraft:bastion_remnant')], 'Find a bastion remnant.')],
    4: [('Void Walkers', [kill('minecraft:enderman', 50)], 'Endermen carry the pearls you need.'),
        ('Shell Shock', [kill('minecraft:shulker', 20)], 'Shulkers guard the End cities.'),
        ('Deeplings', [kill('cataclysm:deepling', 15)], 'Deeplings swarm around the Sunken City.'),
        ('The Stronghold', [structure('betterstrongholds:stronghold')], 'Find a stronghold.'),
        ('City of the End', [structure('minecraft:end_city')], 'Find an End city.')],
    5: [('Echoes in the Dark', [structure('minecraft:ancient_city')], 'Find an ancient city.')],
}


def first_steps():
    q = []
    welcome = Quest('fs_welcome', 'Welcome to MDVLCraft', 0, 0, [checkmark()], [give('minecraft:bread', 8), give('minecraft:torch', 16)],
                    ['Pick your origin, class and martial path, then press Done.', '',
                     'This book points you at the next boss, in order of difficulty. Each chapter ends in a capstone that opens the next.', '',
                     'Open Quests from the MDVLCraft menu. Take one quest at a time: pick it and press Accept. Abandon drops it and loses its progress.'],
                    icon='minecraft:writable_book', shape='gear', size=1.5, accept=False)
    zombies = Quest('fs_zombies', 'First Blood', 2, -1, [kill('minecraft:zombie', 20)], [give('minecraft:iron_ingot', 4), xp(50)], ['Zombies are everywhere at night.'], [welcome.id])
    skeletons = Quest('fs_skeletons', 'Bone Collector', 2, 0, [kill('minecraft:skeleton', 20)], [give('minecraft:arrow', 32), xp(50)], ['Skeletons keep their distance. Close it.'], [welcome.id])
    spiders = Quest('fs_spiders', 'Eight Legs', 2, 1, [kill('minecraft:spider', 10)], [give('minecraft:bread', 8), xp(50)], ['Spiders climb walls. Watch above.'], [welcome.id])
    village = Quest('fs_village', 'Neighbours', 0, 2, [structure('#mdvlcraft:villages')], [give('minecraft:emerald', 4)], ['Find a village. Its people trade, and some can be recruited.'], [welcome.id])
    bed = Quest('fs_bed', 'A Place to Rest', 0, -2, [advancement('minecraft:adventure/sleep_in_bed')], [give('minecraft:torch', 16)], ['Sleep in a bed to set your spawn.'], [welcome.id])
    backpack = Quest('fs_backpack', 'Pack Up', 2, 2, [item('backpacked:backpack')], [give('minecraft:leather', 8)],
                     ['Craft a backpack. Its augment bays add effects like Funnelling, Tool Swapper, Void and Lantern.'], [village.id])
    lvl = Quest('fs_level', 'Getting Stronger', 4, -1, [level(10)], [xp(100)], ['Reach experience level 10.'], [zombies.id])
    hunter = Quest('fs_hunter', 'Monster Hunter', 4, 0, [mob_kills(25)], [give('minecraft:cooked_beef', 8), xp(100)], ['Kill 25 hostile mobs of any kind.'], [skeletons.id])
    iron = Quest('fs_iron', 'Iron Age', 6, 0, [item('minecraft:iron_helmet'), item('minecraft:iron_chestplate'), item('minecraft:iron_leggings'), item('minecraft:iron_boots')],
                 [give('minecraft:diamond', 1), xp(150)], ['Wear a full set of iron armour. The Wanderer chapter opens.'], [lvl.id, hunter.id],
                 icon='minecraft:iron_chestplate', shape='hexagon', size=1.5)
    q += [welcome, zombies, skeletons, spiders, village, bed, backpack, lvl, hunter, iron]
    return dict(file='first_steps', title='First Steps', icon='minecraft:oak_sapling', subtitle='Learn the ropes.', quests=q), iron.id


def tier_chapter(tier, gate):
    t = TIERS[tier]
    q = []
    x = 0
    prev = gate
    progress_ids = []
    hunter = Quest(f't{tier}_hunter', f'Monster Hunter {"I" * tier if tier < 4 else ("IV" if tier == 4 else "V")}', x, 0, [mob_kills(t['kills'])],
                   [give('minecraft:golden_apple', 1), xp(100 * tier)], [f'Kill {t["kills"]} hostile mobs in total.'], [gate])
    q.append(hunter)
    progress_ids.append(hunter.id)
    for i, (title, tasks, desc) in enumerate(PROGRESS[tier]):
        pq = Quest(f't{tier}_p{i}', title, 2 + i * 2, 0 if i % 2 == 0 else -1, tasks, [xp(100 * tier)], [desc], [gate])
        q.append(pq)
        progress_ids.append(pq.id)
    lvl = Quest(f't{tier}_level', f'Level {t["level"]}', 2 + len(PROGRESS[tier]) * 2, -1, [level(t['level'])], [xp(150 * tier)], [f'Reach experience level {t["level"]}.'], [gate])
    q.append(lvl)

    bosses = [b for b in B if b['tier'] == tier]
    kill_ids = []
    for col, b in enumerate(bosses):
        bx = col * 2
        prev_id = gate
        for step, (title, tasks, rewards, desc) in enumerate(b['steps']):
            rewards = list(rewards)
            if step == 2 and b['lair']:
                struct, dim = b['lair']
                if arena_lair(struct):
                    rewards.append(sigil(b['entity'], strip_article(b['name'])))
                    desc = desc + ' Reward: a Boss Sigil that takes you and your party to its arena.'
                elif struct in CATACLYSM_EYES:
                    eye, eye_name, world = CATACLYSM_EYES[struct]
                    rewards.append(give(eye))
                    desc = desc + f' Reward: a {eye_name}. Sneak and use it to travel to {world}, with your party.'
                else:
                    rewards.append(compass(struct, dim, strip_article(b['name'])))
                    desc = desc + ' Reward: a compass to the lair.'
            sq = Quest(f'{b["key"]}_{step}', title, bx, 2 + step, tasks, rewards, [desc], [prev_id], icon=None)
            q.append(sq)
            prev_id = sq.id
        hint = b['hint']
        if b['lair'] and arena_lair(b['lair'][0]):
            hint = 'Use its Boss Sigil (the reward of the quest before) to face it in an arena, with your party.'
        elif b['lair'] and b['lair'][0] in CATACLYSM_EYES:
            _, eye_name, world = CATACLYSM_EYES[b['lair'][0]]
            hint = (f'Its lair lies in {world}, a world of its own. Sneak and use a {eye_name} (the reward of the quest before, '
                    f'or crafted) to travel there with your party; the Return Crystal it drops brings you home.')
        kq = Quest(f'{b["key"]}_kill', f'Defeat {b["name"]}', bx, 5.25, [kill(b['entity'], 1)], [crate(tier), xp(250 * tier)],
                   [hint], [prev_id], icon=b['icon'], shape='octagon', size=1.5)
        q.append(kq)
        kill_ids.append(kq.id)

    cap_x = max(2 + len(PROGRESS[tier]) * 2 + 2, len(bosses) * 2)
    cap_tasks = [level(t['level']), mob_kills(t['kills'])]
    cap_title = {1: 'Proven', 2: 'Into the Fire', 3: 'Forged in Flame', 4: 'Dragonslayer', 5: 'Living Legend'}[tier]
    cap_rewards = {1: [give('minecraft:diamond', 2), bonus(HEALTH, 2)],
                   2: [give('minecraft:obsidian', 10), give('minecraft:flint_and_steel'), bonus(MANA, 10)],
                   3: [give('minecraft:netherite_upgrade_smithing_template'), bonus(COOLDOWN, 0.02)],
                   4: [give('minecraft:enchanted_golden_apple'), bonus(HEALTH, 2)],
                   5: [give('minecraft:nether_star'), crate(5), bonus(MANA, 15), bonus(COOLDOWN, 0.03)]}[tier]
    if tier == 2:
        cap_tasks.append(dimension(NETHER))
    if tier == 4:
        cap_tasks.append(kill('minecraft:ender_dragon', 1))
    cap_desc = [f'Defeat any {t["min_bosses"]} bosses of this chapter, reach level {t["level"]} and kill {t["kills"]} hostile mobs.']
    if tier < 5:
        cap_desc.append(f'Opens the {TIERS[tier + 1]["name"]} chapter.')
    cap = Quest(f't{tier}_capstone', cap_title, cap_x, 2, cap_tasks, cap_rewards + [xp(500 * tier)], cap_desc, kill_ids,
                icon=t['icon'], min_deps=t['min_bosses'], shape='hexagon', size=2.0)
    q.append(cap)
    return dict(file=t['file'], title=f'{tier}. {t["name"]}', icon=t['icon'], subtitle=t['subtitle'], quests=q), cap.id


# ---------------------------------------------------------------------------------------------------------------
# Gates chapter: its own thing, open from the start, after the tier chapters. The DQ9 quest log
# (client/QuestLogScreen) numbers it on its own (001G, 002G, ...) in the order written here, which is the order of
# difficulty. Gate events FTB Quests can't see are hidden advancements mdvlcraft:quests/gates/<name> with an
# impossible criterion, granted by gate/GateAdvancements; the attuned/tempered ones are plain inventory_changed ones.
# ---------------------------------------------------------------------------------------------------------------
RANKS = 'edcbas'
THEMES = ['crypt', 'warren', 'frozen', 'warcamp', 'spire', 'forge']
THEME_NAMES = {'crypt': 'Gravekeeper Crypt', 'warren': 'Goblin Warren', 'frozen': 'Frozen Hollow', 'warcamp': 'Orc War Camp',
               'spire': 'Mage Spire', 'forge': 'Infernal Forge'}
sys.path.insert(0, str(Path(__file__).resolve().parent / 'gates'))
from new_themes import NEW_THEMES   # the 2.1.53 themes, after the first six
THEMES += list(NEW_THEMES)
THEME_NAMES.update({k: t['name'] for k, t in NEW_THEMES.items()})
RULES = ['no_potions', 'no_healing', 'statues', 'narrowing_door']
GATE_EVENTS = (['open_gate', 'gate_kill', 'elite_kill', 'enter_red', 'enter_red_s', 'enter_trial', 'enter_modified', 'clear_red',
                'clear_red_s', 'clear_modified', 'break', 'boss_chest', 'forge', 'tower_enter', 'tower_boss_5',
                'tower_hidden', 'reroll', 'seal_trade', 'tower_pick', 'tower_farm']
               + [f'{kind}_{r}' for kind in ('enter', 'clear', 'vein') for r in RANKS]
               + [f'enter_theme_{t}' for t in THEMES] + [f'clear_trial_{r}' for r in RULES]
               + [f'tower_floor_{n}' for n in (5, 10, 25, 50, 55, 100)])
GATE_DATA_ADVANCEMENTS = ['attuned', 'tempered', 'tempered_s']


def gadv(name):
    assert name in GATE_EVENTS or name in GATE_DATA_ADVANCEMENTS, name
    return advancement(f'mdvlcraft:quests/gates/{name}')


def gates_cleared(n):
    """Gates cleared (any rank, red and trials too): the custom stat gate/GateAdvancements counts at every clear."""
    return {'type': 'stat', 'stat': 'mdvlcraft:gates_cleared', 'value': n}


def skill_xp(n):
    return command(f'/mdvl gate quest skillxp {{p}} {n}', f'{n} Skill Experience', 'minecraft:experience_bottle')


def give_key(rank, theme):
    """An attuned key (NBT theme, as the attunement recipes make it)."""
    return {'type': 'item', 'item': {'id': f'mdvlcraft:gate_key_{rank}', 'Count': ('b', 1), 'tag': {'theme': theme}}, 'count': 1}


def g(item_id):
    return f'mdvlcraft:{item_id}'


# (key, title, tasks, rewards, description, deps (keys; None = the quest before), icon, shape). One row per stage.
GATE_ROWS = [
    ('Your first gate (E)', [
        ('first_key', 'Your First Key', [item(g('gate_key_e'))], [give(g('gate_key_e')), xp(50)],
         'Gates are portals to dungeons, ranked E (weakest) to S. Each needs a key of its rank. Craft an E-Rank Gate Key: '
         '4 iron ingots, 4 amethyst shards and an ender pearl. The reward is a spare.', [], g('gate_key_e'), 'gear'),
        ('open_gate', 'Open the Way', [gadv('open_gate')], [xp(50)],
         'Right-click the top of a block with the key, where the gate has room to stand. The key is used up and the gate '
         'stabilises for a few seconds. A gate nobody clears breaks after two hours.', None, 'minecraft:ender_eye', None),
        ('enter_e', 'Step Inside', [gadv('enter_e')], [skill_xp(100)],
         'Walk into the gate. You and your party (up to the gate\'s cap) arrive in its dungeon; each room fills with monsters '
         'when you step in. You can walk back out of a normal gate.', None, g('gate_key_e'), None),
        ('gate_kill', 'First Blood', [gadv('gate_kill')], [xp(100)],
         'Kill a gate monster. They grow stronger with the gate\'s rank.', None, 'minecraft:iron_sword', None),
        ('essence_e', 'Essence of the Fallen', [item(g('essence_stone_e'))], [give(g('mana_crystal_e'))],
         'Gate monsters sometimes drop essence stones of the gate\'s rank; elites and bosses drop more. Stones craft keys '
         'and pay the Gate Forge.', None, g('essence_stone_e'), None),
        ('vein_e', 'Crystal Vein', [gadv('vein_e')], [give(g('essence_stone_e'))],
         'Glowing mana crystal veins grow in gate rooms. Mine one for crystals: E and D veins need a stone pickaxe, C iron, '
         'B diamond, A and S netherite. Fortune helps.', None, g('mana_crystal_ore_e'), None),
        ('elite_kill', 'Elite Hunter', [gadv('elite_kill')], [give(g('essence_stone_e'), 2)],
         'Elites wait in the rooms before the boss: tougher than the rest and worth one or two stones.', None, 'minecraft:golden_sword', None),
        ('clear_e', 'E-Rank Cleared', [gadv('clear_e')], [skill_xp(200), give(g('gate_key_e'))],
         'Kill the gate\'s boss to clear it. Everyone present earns skill experience (E 200, up to S 6000) and the way out '
         'opens where the boss fell.', None, g('gate_key_e'), 'hexagon'),
        ('boss_chest', 'Spoils of the Gate', [gadv('boss_chest')], [give(g('essence_stone_e'), 2)],
         'A boss chest appears after the clear. Each player sees only their own roll: stones, crystals, treasure, sometimes '
         'a key of the next rank. Whatever you leave in it comes to you once you are out.', None, 'minecraft:chest', None),
    ]),
    ('Forge and tools (E)', [
        ('forge_item', 'The Gate Forge', [item(g('gate_forge'))], [xp(100)],
         'Craft a Gate Forge from iron ingots, amethyst shards, an E essence stone and iron blocks.', ['boss_chest'], g('gate_forge'), None),
        ('forge', 'Open for Business', [gadv('forge')], [give(g('essence_stone_e'), 2)],
         'Place the forge and use it. It trades stones and crystals for Temper Runes, rerolls the modifiers '
         'of A and S keys, and later takes Tower Seals.', None, g('gate_forge'), None),
        ('rune_e', 'Temper Rune', [item(g('temper_rune_e'))], [give(g('mana_crystal_e'))],
         'Trade 6 E essence stones and 2 E mana crystals at the forge for an E Temper Rune.', ['forge'], g('temper_rune_e'), None),
        ('tempered', 'Tempered Steel', [gadv('tempered')], [give(g('essence_stone_e'), 2)],
         'In an anvil, gear on the left and a rune on the right. A tempered weapon hits gate monsters harder (+2% at E, up to '
         '+12% at S); tempered armour takes less from them. A higher rune replaces a lower one.', None, 'minecraft:anvil', None),
        ('attune', 'Attunement', [gadv('attuned')], [give_key('e', 'frozen')],
         'Craft a key with a catalyst to choose its theme: bone block (Gravekeeper Crypt), torch (Goblin Warren), packed ice '
         '(Frozen Hollow), iron axe (Orc War Camp), lapis block (Mage Spire), magma block (Infernal Forge). Unattuned keys '
         'pick a theme that suits their rank.', ['clear_e'], 'minecraft:bone_block', None),
        ('silk', 'Keep the Ore', [item(g('mana_crystal_ore_e'))], [xp(100)],
         'Mine a crystal vein with Silk Touch to keep the ore block itself.', ['vein_e'], g('mana_crystal_ore_e'), None),
        ('stock_e', 'Stockpile', [item(g('essence_stone_e'), 4), item(g('mana_crystal_e'), 4)], [skill_xp(150)],
         'Gather 4 E essence stones and 4 E mana crystals: with a gold block, that is a D key.', ['clear_e'], g('mana_crystal_e'), None),
    ]),
    ('Rank D', [
        ('key_d', 'A Darker Key', [item(g('gate_key_d'))], [xp(150)],
         'Craft a D-Rank Gate Key: 4 E stones, 4 E crystals and a gold block. Every key above E is made from the stones and '
         'crystals of the rank below.', ['stock_e'], g('gate_key_d'), None),
        ('enter_d', 'Rank D', [gadv('enter_d')], [skill_xp(200)],
         'Open and enter a D-rank gate: more rooms, more monsters, harder hits.', None, g('gate_key_d'), None),
        ('clear_d', 'D-Rank Cleared', [gadv('clear_d')], [skill_xp(400), give_key('d', 'warren')],
         'Clear a D-rank gate.', None, g('gate_key_d'), 'hexagon'),
        ('stock_d', 'D Stockpile', [item(g('essence_stone_d'), 4), item(g('mana_crystal_d'), 4)], [xp(200)],
         'Gather 4 D essence stones and 4 D mana crystals: with a diamond, that is a C key.', None, g('essence_stone_d'), None),
        ('rune_d', 'D Temper Rune', [item(g('temper_rune_d'))], [give(g('essence_stone_d'), 2)],
         'Trade 6 D stones and 2 D crystals at the forge for a D Temper Rune and temper your gear again.', ['clear_d'], g('temper_rune_d'), None),
        ('tourist', 'Gate Tourist', [gadv(f'enter_theme_{t}') for t in THEMES], [give(g('essence_stone_d'), 2), skill_xp(300)],
         'Enter a gate of each of the six themes: ' + ', '.join(THEME_NAMES[t] for t in THEMES) + '. Attuned keys of any '
         'rank choose the theme.', ['enter_d'], 'minecraft:filled_map', None),
        ('break', 'Dungeon Break', [gadv('break')], [xp(200)],
         'A gate nobody clears breaks two hours after it opens: its monsters pour out around it and the gate is gone. '
         'Be within 128 blocks when one breaks.', ['enter_d'], 'minecraft:tnt', None),
    ]),
    ('Rank C', [
        ('key_c', 'Diamond Key', [item(g('gate_key_c'))], [xp(250)],
         'Craft a C-Rank Gate Key: 4 D stones, 4 D crystals and a diamond.', ['stock_d'], g('gate_key_c'), None),
        ('enter_c', 'Rank C', [gadv('enter_c')], [skill_xp(400)],
         'Enter a C-rank gate. Elites are common now; bring tempered gear.', None, g('gate_key_c'), None),
        ('vein_c', 'Iron Bites Deeper', [gadv('vein_c')], [give(g('essence_stone_c'))],
         'Mine a C crystal vein. It needs an iron pickaxe or better.', None, g('mana_crystal_ore_c'), None),
        ('clear_c', 'C-Rank Cleared', [gadv('clear_c')], [skill_xp(800), give_key('c', 'spire'), bonus(MANA, 10)],
         'Clear a C-rank gate.', None, g('gate_key_c'), 'hexagon'),
        ('stock_c', 'C Stockpile', [item(g('essence_stone_c'), 4), item(g('mana_crystal_c'), 3)], [xp(300)],
         'Gather 4 C essence stones and 3 C mana crystals: with 2 blaze rods, that is a B key.', None, g('essence_stone_c'), None),
        ('rune_c', 'C Temper Rune', [item(g('temper_rune_c'))], [give(g('essence_stone_c'), 2)],
         'Trade 6 C stones and 2 C crystals at the forge for a C Temper Rune.', ['clear_c'], g('temper_rune_c'), None),
    ]),
    ('Rank B and red gates', [
        ('key_b', 'Blazing Key', [item(g('gate_key_b'))], [xp(400)],
         'Craft a B-Rank Gate Key: 4 C stones, 3 C crystals and 2 blaze rods.', ['stock_c'], g('gate_key_b'), None),
        ('enter_b', 'Rank B', [gadv('enter_b')], [skill_xp(800)],
         'Enter a B-rank gate. From B up, a gate may turn red the first time someone steps in.', None, g('gate_key_b'), None),
        ('vein_b', 'Diamond Edge', [gadv('vein_b')], [give(g('essence_stone_b'))],
         'Mine a B crystal vein. It needs a diamond pickaxe or better.', None, g('mana_crystal_ore_b'), None),
        ('clear_b', 'B-Rank Cleared', [gadv('clear_b')], [skill_xp(1500), give(g('essence_stone_b'))],
         'Clear a B-rank gate.', None, g('gate_key_b'), 'hexagon'),
        ('enter_red', 'Seeing Red', [gadv('enter_red')], [give(g('essence_stone_c'))],
         'B, A and S gates can turn red on first entry (B 5%, A 8%, S 12%). A red gate seals behind you: no leaving, '
         'nobody new gets in. Inside is a wild land with camps around a boss\'s lair. Die and you are out.',
         ['enter_b'], 'minecraft:crying_obsidian', None),
        ('clear_red', 'Survive the Red', [gadv('clear_red')], [skill_xp(2000), give(g('essence_stone_b'), 2)],
         'Kill the lair\'s boss: the seal breaks and an exit pad of crying obsidian appears. Red gates pay half again in '
         'stones and experience.', None, 'minecraft:crying_obsidian', 'hexagon'),
        ('stock_b', 'B Stockpile', [item(g('essence_stone_b'), 4), item(g('mana_crystal_b'), 3)], [xp(400)],
         'Gather 4 B essence stones and 3 B mana crystals: with 2 netherite scrap, that is an A key.', ['clear_b'], g('essence_stone_b'), None),
        ('rune_b', 'B Temper Rune', [item(g('temper_rune_b'))], [give(g('essence_stone_b'), 2)],
         'Trade 6 B stones and 2 B crystals at the forge for a B Temper Rune.', ['clear_b'], g('temper_rune_b'), None),
    ]),
    ('Rank A and modifiers', [
        ('key_a', 'Netherite Key', [item(g('gate_key_a'))], [xp(500)],
         'Craft an A-Rank Gate Key: 4 B stones, 3 B crystals and 2 netherite scrap. A and S keys roll gate modifiers when '
         'made (A up to 1, S 1 to 3); hover over the key to see them.', ['stock_b'], g('gate_key_a'), None),
        ('enter_a', 'Rank A', [gadv('enter_a')], [skill_xp(1500)],
         'Enter an A-rank gate.', None, g('gate_key_a'), None),
        ('vein_a', 'Netherite Pick', [gadv('vein_a')], [give(g('essence_stone_a'))],
         'Mine an A crystal vein. It needs a netherite pickaxe.', None, g('mana_crystal_ore_a'), None),
        ('enter_modified', 'Twisted Gate', [gadv('enter_modified')], [give(g('essence_stone_a'))],
         'Enter a gate with modifiers: Enraged, Swarming, Cursed, Volatile, Silenced, Elite Pack, Crimson or Ironclad. Each '
         'makes it harder and adds 20% stones and 15% clear experience. Use the Gate Forge with a key in hand to reroll it '
         'for 4 A stones.', ['enter_a'], 'minecraft:fermented_spider_eye', None),
        ('clear_a', 'A-Rank Cleared', [gadv('clear_a')], [skill_xp(3000), give(g('gate_key_a'))],
         'Clear an A-rank gate.', ['enter_a'], g('gate_key_a'), 'hexagon'),
        ('clear_modified', 'Against the Odds', [gadv('clear_modified')], [skill_xp(1500)],
         'Clear a gate that has modifiers.', ['enter_modified'], 'minecraft:fermented_spider_eye', None),
        ('stock_a', 'A Stockpile', [item(g('essence_stone_a'), 4), item(g('mana_crystal_a'), 3)], [xp(500)],
         'Gather 4 A essence stones and 3 A mana crystals: with a netherite ingot and a nether star, that is an S key.',
         ['clear_a'], g('essence_stone_a'), None),
        ('rune_a', 'A Temper Rune', [item(g('temper_rune_a'))], [give(g('essence_stone_a'), 2)],
         'Trade 6 A stones and 2 A crystals at the forge for an A Temper Rune.', ['clear_a'], g('temper_rune_a'), None),
    ]),
    ('Rank S', [
        ('key_s', 'The Highest Key', [item(g('gate_key_s'))], [xp(800)],
         'Craft an S-Rank Gate Key: 4 A stones, 3 A crystals, a netherite ingot and a nether star.', ['stock_a'], g('gate_key_s'), None),
        ('enter_s', 'Rank S', [gadv('enter_s')], [skill_xp(3000)],
         'Enter an S-rank gate.', None, g('gate_key_s'), None),
        ('vein_s', 'Heart of the Gate', [gadv('vein_s')], [give(g('essence_stone_s'))],
         'Mine an S crystal vein.', None, g('mana_crystal_ore_s'), None),
        ('clear_s', 'S-Rank Cleared', [gadv('clear_s')], [skill_xp(6000), give(g('essence_stone_s'), 2), bonus(COOLDOWN, 0.02)],
         'Clear an S-rank gate.', None, g('gate_key_s'), 'hexagon'),
        ('rune_s', 'S Temper Rune', [item(g('temper_rune_s'))], [give(g('mana_crystal_s'), 2)],
         'Trade 6 S stones and 2 S crystals at the forge for an S Temper Rune, the top tier. Later, Tower Seals buy them too.',
         ['clear_s'], g('temper_rune_s'), None),
        ('tempered_s', 'Masterwork', [gadv('tempered_s')], [xp(1000)],
         'Temper a piece of gear with an S rune.', None, 'minecraft:netherite_sword', None),
    ]),
    ('Trials and the hidden boss', [
        ('enter_trial', 'Trial by Rule', [gadv('enter_trial')], [give(g('essence_stone_b'), 2)],
         'Some S gates that don\'t turn red become trial gates, with a rule shown as you enter and on a stone tablet: No '
         'Potions, No Healing, Statues or The Narrowing Door.', ['enter_s'], 'minecraft:chiseled_stone_bricks', None),
        ('trial_trophy', 'Trial Trophy', [item(g('gate_trophy'))], [skill_xp(3000)],
         'Clear a trial gate. Everyone present earns a Trial Trophy; trials pay half again in stones, veins and experience, '
         'and the boss chest often rolls twice.', None, g('gate_trophy'), 'hexagon'),
        ('trial_no_potions', 'No Potions', [gadv('clear_trial_no_potions')], [give(g('essence_stone_s'))],
         'Clear a No Potions trial: no potion can be drunk or thrown.', ['trial_trophy'], 'minecraft:glass_bottle', None),
        ('trial_no_healing', 'No Healing', [gadv('clear_trial_no_healing')], [give(g('essence_stone_s'))],
         'Clear a No Healing trial: only food heals you.', ['trial_trophy'], 'minecraft:cooked_beef', None),
        ('trial_statues', 'Statues', [gadv('clear_trial_statues')], [give(g('essence_stone_s'))],
         'Clear a Statues trial: stone statues hunt you room by room and move only while nobody looks at them. They '
         'can\'t be hurt.', ['trial_trophy'], 'minecraft:armor_stand', None),
        ('trial_door', 'The Narrowing Door', [gadv('clear_trial_narrowing_door')], [give(g('essence_stone_s'))],
         'Clear a Narrowing Door trial: whoever leaves can\'t come back in, and if nobody stays inside, the gate fails.',
         ['trial_trophy'], 'minecraft:iron_door', None),
        ('enter_red_s', 'Crimson Crown', [gadv('enter_red_s')], [give(g('mana_crystal_s'), 2)],
         'Enter a red S gate.', ['clear_s', 'clear_red'], 'minecraft:redstone_block', None),
        ('clear_red_s', 'The Hidden Boss', [gadv('clear_red_s')], [skill_xp(9000)],
         'In a red S gate the lair boss is not the end: a hidden boss rises in the arena, stronger and glowing. Only its '
         'death breaks the seal.', None, 'minecraft:wither_skeleton_skull', 'hexagon'),
        ('black', 'Black Essence', [item(g('essence_stone_black'))], [give(g('castle_sigil'))],
         'Hidden bosses are the only source of Black Essence Stones. With 4 of them, 4 S stones and a netherite ingot you '
         'could craft a Castle Sigil; this quest gives you one.', None, g('essence_stone_black'), None),
    ]),
    ('The Demon Castle', [
        ('tower_enter', 'The Demon Castle', [gadv('tower_enter')], [give(g('tower_seal'), 2)],
         'Use the Castle Sigil on the top of a block to open the castle (the sigil is kept). The tower has no top; your floor '
         'is saved. Leave by a landing\'s lever and you keep it; die and you drop to the first floor of your block of five. '
         'Sneak-use the sigil to start from your checkpoint. Allies who reached your floor may join.', ['black'], g('castle_sigil'), 'gear'),
        ('floor_5', 'Floor 5', [gadv('tower_floor_5')], [give(g('essence_stone_s'), 2)],
         'Climb to floor 5. Clear a floor\'s rooms and its chest opens; every fifth floor is a boss floor.', None, 'minecraft:ladder', None),
        ('boss_5', 'First Warden', [gadv('tower_boss_5')], [skill_xp(3000)],
         'Beat the floor 5 boss. The bars open, its chest waits in the arena, you earn Tower Seals (more the first time) '
         'and your checkpoint moves to floor 6.', None, g('tower_seal'), 'hexagon'),
        ('seals', 'Seal Collector', [item(g('tower_seal'), 6)], [give(g('essence_stone_black'))],
         'Hold 6 Tower Seals. With 2 S mana crystals they buy an S Temper Rune at the Gate Forge.', None, g('tower_seal'), None),
        ('floor_10', 'Floor 10', [gadv('tower_floor_10')], [give(g('tower_seal'), 2)],
         'Climb to floor 10. From floor 11 tower modifiers join in, one more every ten floors.', ['boss_5'], 'minecraft:ladder', None),
        ('floor_25', 'Floor 25', [gadv('tower_floor_25')], [give(g('tower_seal'), 4), give(g('essence_stone_black'))],
         'Climb to floor 25. Its bosses are A-rank now.', None, 'minecraft:ladder', None),
        ('floor_50', 'Floor 50', [gadv('tower_floor_50')], [give(g('tower_seal'), 4), skill_xp(8000)],
         'Climb to floor 50.', None, 'minecraft:ladder', 'hexagon'),
        ('floor_55', 'Legend Floors', [gadv('tower_floor_55')], [give(g('tower_seal'), 4)],
         'Climb to floor 55. From here the bosses are S-rank with two extra modifiers.', None, 'minecraft:ladder', None),
        ('tower_hidden', 'Hidden in the Castle', [gadv('tower_hidden')], [give(g('essence_stone_black'), 2), skill_xp(10000)],
         'On boss floors from 55 a hidden boss may rise when the boss dies. Defeat it.', None, 'minecraft:wither_skeleton_skull', 'hexagon'),
        ('floor_100', 'The Hundredth Floor', [gadv('tower_floor_100')], [give(g('tower_seal'), 8), skill_xp(15000)],
         'Climb to floor 100.', ['floor_55'], 'minecraft:nether_star', 'hexagon'),
        ('castle_trophy', 'Castle Trophy', [item(g('castle_trophy'))], [xp(1000)],
         'Trade 16 Tower Seals at the Gate Forge for a Castle Trophy.', ['seals'], g('castle_trophy'), None),
        ('master', 'Master of Gates', [gadv('tower_floor_100'), item(g('castle_trophy'))], [skill_xp(20000), give(g('castle_trophy')), bonus(HEALTH, 2)],
         'Reach floor 100 and hold a Castle Trophy. Every rank, every red gate, every trial: you have seen them all.',
         ['floor_100', 'castle_trophy', 'clear_red_s', 'trial_trophy'], g('castle_trophy'), 'gear'),
    ]),
    ('Veteran and castle extras', [
        ('clears_10', 'Gate Regular', [gates_cleared(10)], [skill_xp(1000), give(g('essence_stone_c'), 2)],
         'Clear 10 gates, any rank. Red gates and trials count too; everyone present at a clear counts it.', ['clear_e'],
         g('gate_key_d'), None),
        ('clears_50', 'Gate Veteran', [gates_cleared(50)], [skill_xp(4000), give(g('essence_stone_a'), 2)],
         'Clear 50 gates.', None, g('gate_key_b'), None),
        ('clears_100', 'Gatebreaker', [gates_cleared(100)], [skill_xp(10000), give(g('essence_stone_black'))],
         'Clear 100 gates.', None, g('gate_key_s'), 'hexagon'),
        ('reroll', 'Fate Rerolled', [gadv('reroll')], [give(g('essence_stone_a'), 4)],
         'Use the Gate Forge with an A or S key in hand to reroll its modifiers for 4 A essence stones. This quest pays '
         'them back.', ['key_a'], 'minecraft:fermented_spider_eye', None),
        ('seal_trade', 'Sealed Bargain', [gadv('seal_trade')], [give(g('tower_seal'), 2)],
         'Trade Tower Seals at the Gate Forge: 6 seals and 2 S mana crystals for an S Temper Rune, or 16 seals for a '
         'Castle Trophy.', ['seals'], g('tower_seal'), None),
        ('tower_pick', 'Pick Your Floor', [gadv('tower_pick')], [xp(500)],
         'Use the Castle Sigil and pick a start below your floor from its menu: the checkpoint, floor 1, or any block start '
         '(6, 11, 16 ...) up to your checkpoint. Sneak-use starts from the checkpoint at once.', ['boss_5'], g('castle_sigil'), None),
        ('tower_farm', 'Farm Floors', [gadv('tower_farm')], [give(g('tower_seal'), 2)],
         'Kill a castle monster on a floor more than 10 below your best. Farm floors are safer, but pay half: half the '
         'essence stones, half the seals and a 50% chance at the floor chest.', ['floor_25'], 'minecraft:iron_hoe', None),
    ]),
]


# ---------------------------------------------------------------------------------------------------------------
# Food chapter (Farmer's Delight), numbered 001F... in the quest log. Rows as in GATE_ROWS.
# ---------------------------------------------------------------------------------------------------------------
def fd(item_id):
    return f'farmersdelight:{item_id}'


FOOD_ROWS = [
    ('Tools of the kitchen', [
        ('knife', 'A Sharp Edge', [item(fd('flint_knife'))], [give('minecraft:bread', 4), xp(50)],
         'Craft a Flint Knife: a flint and a stick. Knives cut food on a cutting board and harvest straw from grass and wheat.',
         [], fd('flint_knife'), 'gear'),
        ('board', 'Cutting Board', [item(fd('cutting_board'))], [xp(50)],
         'Craft a Cutting Board from planks and sticks. Place an item on it and use a knife on it to slice it.', None, fd('cutting_board'), None),
        ('wild', 'Wild Harvest', [item(fd('cabbage')), item(fd('tomato')), item(fd('onion')), item(fd('rice'))],
         [give('minecraft:bone_meal', 8), xp(100)],
         'Wild crops grow in the open: cabbages and beetroots by beaches, tomatoes in hot places, onions and carrots in '
         'temperate ones, rice by water. Break them to gather the crop and its seeds. Crops only grow in their seasons.',
         None, fd('wild_cabbages'), None),
        ('straw', 'Straw Gatherer', [item(fd('straw'), 8)], [give('minecraft:string', 8)],
         'Break grass or wheat with a knife in your main hand to gather straw.', ['knife'], fd('straw'), None),
        ('pot', 'The Cooking Pot', [item(fd('cooking_pot'))], [xp(100)],
         'Craft a Cooking Pot: bricks, iron ingots, a wooden shovel and a water bucket. It cooks meals over heat.', ['board'],
         fd('cooking_pot'), None),
        ('stove', 'Heat Source', [item(fd('stove'))], [xp(100)],
         'Craft a Stove: iron ingots, bricks and a campfire. Set the pot on it (a campfire under the pot works too).',
         None, fd('stove'), None),
    ]),
    ('Meals', [
        ('soup', 'Hearty Soup', [item(fd('vegetable_soup'))], [give('minecraft:bowl', 4), xp(150)],
         'Cook a Vegetable Soup in the pot: a carrot, a potato, a beetroot and a leafy green, served in a bowl.', ['stove'],
         fd('vegetable_soup'), None),
        ('stew', 'Stew Day', [item(fd('beef_stew'))], [xp(150)],
         'Cook a Beef Stew: raw beef, a carrot and a potato, served in a bowl. Hearty meals grant Nourishment, which stops '
         'hunger from draining.', None, fd('beef_stew'), None),
        ('skillet', 'The Skillet', [item(fd('skillet'))], [xp(150)],
         'Craft a Skillet: iron ingots and a brick. Place it over heat to cook food on it, or swing it: it is a weapon too.', ['stove'], fd('skillet'), None),
        ('fried_rice', 'Fried Rice', [item(fd('fried_rice'))], [xp(150)],
         'Cook Fried Rice: rice, an egg, a carrot and an onion.', ['soup'], fd('fried_rice'), None),
        ('pasta', 'Pasta Night', [item(fd('raw_pasta')), item(fd('pasta_with_meatballs'))], [xp(200)],
         'Make wheat dough (wheat with water or an egg), cut it into raw pasta on the cutting board, then cook Pasta with '
         'Meatballs: pasta, minced beef and tomato sauce.',
         None, fd('pasta_with_meatballs'), None),
        ('sandwiches', 'Lunch Break', [item(fd('hamburger')), item(fd('chicken_sandwich'))], [xp(200)],
         'Assemble a Hamburger and a Chicken Sandwich at a crafting table. Bread, cooked meat and greens.', None, fd('hamburger'), None),
    ]),
    ('The farm', [
        ('compost', 'Rich Soil', [item(fd('organic_compost')), item(fd('rich_soil'))], [give('minecraft:bone_meal', 16)],
         'Craft Organic Compost from dirt, straw, bone meal and rotten flesh (or tree bark). Placed, it slowly turns into Rich Soil, faster '
         'next to light and water. Crops grow faster on rich soil, and mushrooms planted on it grow into colonies.',
         ['straw'], fd('organic_compost'), None),
        ('crates', 'Crate Stacker', [item(fd('cabbage_crate')), item(fd('tomato_crate')), item(fd('onion_crate'))], [xp(200)],
         'Pack nine of a crop into a crate to store it.', ['wild'], fd('cabbage_crate'), None),
        ('rope', 'Learning the Ropes', [item(fd('rope'), 16)], [give('minecraft:lead', 2)],
         'Craft Rope from straw. Tomatoes climb it; you can climb it too.', ['straw'], fd('rope'), None),
        ('comfort', 'Comfort Food', [item(fd('chicken_soup')), item(fd('baked_cod_stew'))], [xp(200)],
         'Cook a Chicken Soup and a Baked Cod Stew. Warm soups grant Comfort: no slowness from the cold, and regeneration '
         'while you are fed.', ['stew'], fd('chicken_soup'), None),
    ]),
    ('Feasts', [
        ('roast_chicken', 'Roast Chicken', [item(fd('roast_chicken_block'))], [xp(300)],
         'Craft a Roast Chicken feast: cooked chicken, baked potatoes, bread, an egg, carrots, an onion and a bowl. Place it, '
         'then use a bowl on it to serve a portion; it feeds several.',
         ['pasta'], fd('roast_chicken_block'), None),
        ('shepherds_pie', "Shepherd's Pie", [item(fd('shepherds_pie_block'))], [xp(300)],
         "Craft a Shepherd's Pie feast: cooked mutton, baked potatoes, milk, onions and a bowl.", None, fd('shepherds_pie_block'), None),
        ('ham', 'Honey Glazed Ham', [item(fd('honey_glazed_ham_block'))], [xp(300)],
         'Craft a Honey Glazed Ham feast: smoked ham, cooked rice, sweet berries, a honey bottle and a bowl.', None, fd('honey_glazed_ham_block'), None),
        ('grand_feast', 'The Grand Feast', [item(fd('stuffed_pumpkin_block')), item(fd('rice_roll_medley_block'))],
         [give('minecraft:golden_carrot', 16), bonus(HEALTH, 2)],
         'Cook a Stuffed Pumpkin in the pot (served in a pumpkin) and craft a Rice Roll Medley from kelp, salmon and cod rolls. '
         'A well-fed adventurer is a tougher one.', ['ham', 'comfort', 'compost'],
         fd('stuffed_pumpkin_block'), 'hexagon'),
    ]),
]


# ---------------------------------------------------------------------------------------------------------------
# Brewing chapter (Ars Elixirum), numbered 001B... in the quest log. Rows as in GATE_ROWS.
# ---------------------------------------------------------------------------------------------------------------
def ex(item_id):
    return f'elixirum:{item_id}'


BREWING_ROWS = [
    ('The alchemist\'s bench', [
        ('cauldron', 'The Glass Cauldron', [item(ex('glass_cauldron'))], [xp(50)],
         'Craft a Glass Cauldron: gray stained glass and two iron ingots. Fill it with water and place it over a heat source '
         '(a campfire, fire or magma) to bring it to a boil.', [], ex('glass_cauldron'), 'gear'),
        ('solvent', 'Honey Solvent', [item(ex('honey_solvent'))], [give('minecraft:honeycomb', 2)],
         'Craft Honey Solvent: eight glass bottles around a honeycomb.', None, ex('honey_solvent'), None),
        ('extract', 'Essence Extraction', [item('minecraft:brewing_stand'), item(ex('extract'))], [xp(100)],
         'At a Brewing Stand, brew Honey Solvent with any ingredient. It draws out one of its essences as an Extract, and '
         'you learn what the ingredient is made of.', None, ex('extract'), None),
        ('elixir', 'Your First Elixir', [item(ex('elixir'))], [give('minecraft:glass_bottle', 4), xp(150)],
         'Drop ingredients into the boiling cauldron; their essences decide the effects. Then fill a glass bottle from it. '
         'Open the Collection to see what you have brewed and save the recipe.', None, ex('elixir'), None),
        ('eye', "Alchemist's Eye", [item(ex('alchemist_eye'))], [xp(150)],
         "Craft an Alchemist's Eye: an eye of ender, iron and string. Equipped, it shows the composition of ingredients "
         'you have discovered.', None, ex('alchemist_eye'), None),
    ]),
    ('Mastery', [
        ('shelf', 'Potion Shelf', [item(ex('potion_shelf'))], [xp(100)],
         'Craft Potion Shelves from planks and a stick to show off your elixirs.', ['elixir'], ex('potion_shelf'), None),
        ('apothecary', 'Apothecary', [item(ex('elixir'), 4)], [xp(300)],
         'Hold four elixirs. Brewing a saved recipe again raises its mastery and your overall alchemy mastery.', ['elixir'],
         ex('elixir'), None),
        ('witch_totem', 'Witch Totem', [item('minecraft:totem_of_undying'), item(ex('witch_totem_of_undying'))],
         [xp(500), bonus(MANA, 10), bonus(COOLDOWN, 0.02)],
         'Get a Totem of Undying (evokers drop them), then craft it with an elixir into a Witch Totem of Undying, which '
         'carries the elixir\'s effects.', ['apothecary'], ex('witch_totem_of_undying'), 'hexagon'),
    ]),
]


def row_chapter(file, title, icon, subtitle, rows):
    """A chapter laid out in rows, one quest per column; a quest needs the one before unless it names its deps."""
    q, ids, prev = [], {}, None
    for row, (_, quests) in enumerate(rows):
        for col, (key, qtitle, tasks, rewards, desc, deps, qicon, shape) in enumerate(quests):
            dep_ids = [prev] if deps is None else [ids[d] for d in deps]
            qu = Quest(f'{file}_{key}', qtitle, col * 2, row * 2.5, tasks, rewards, [desc], dep_ids, icon=qicon, shape=shape,
                       size=1.5 if shape else None)
            q.append(qu)
            ids[key] = prev = qu.id
    check_gate_rewards(q)
    return dict(file=file, title=title, icon=icon, subtitle=subtitle, quests=q)


def gates_chapter():
    return row_chapter('gates', 'Gates', 'mdvlcraft:gate_key_s', 'Keys, gates, red gates, trials and the Demon Castle.', GATE_ROWS)


def food_chapter():
    return row_chapter('food', 'Food', fd('cooking_pot'), "Farmer's Delight: knives, pots, meals and feasts.", FOOD_ROWS)


def brewing_chapter():
    return row_chapter('brewing', 'Brewing', ex('glass_cauldron'), 'Ars Elixirum: essences, elixirs and mastery.', BREWING_ROWS)


def check_gate_rewards(quests):
    """No reward may finish a later quest's item task by itself, so a reward never skips a step."""
    for i, qu in enumerate(quests):
        for r in qu.rewards:
            if r['type'] != 'item' or not isinstance(r['item'], str):
                continue
            for later in quests[i + 1:]:
                for t in later.tasks:
                    if t['type'] == 'item' and t['item'] == r['item']:
                        assert r['count'] < t['count'][1], f'{qu.title} gives {r["item"]}, finishing {later.title}'


def write_gate_advancements():
    out = f'{RES}/advancements/quests/gates'
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    advs = {name: {'criteria': {'done': {'trigger': 'minecraft:impossible'}}} for name in GATE_EVENTS}
    keys = [f'mdvlcraft:gate_key_{r}' for r in RANKS]
    advs['attuned'] = {'criteria': {t: {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [
        {'items': keys, 'nbt': '{theme:"%s"}' % t}]}} for t in THEMES}, 'requirements': [THEMES]}
    levels = [f'level_{n}' for n in range(1, 7)]
    advs['tempered'] = {'criteria': {f'level_{n}': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [
        {'nbt': '{mdvlcraft_temper:%d}' % n}]}} for n in range(1, 7)}, 'requirements': [levels]}
    advs['tempered_s'] = {'criteria': {'level_6': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [
        {'nbt': '{mdvlcraft_temper:6}'}]}}}}
    for name, adv in sorted(advs.items()):
        with open(f'{out}/{name}.json', 'w') as f:
            json.dump(adv, f, indent=2)
            f.write('\n')


def strip_article(name):
    return name[4:].title() if name.startswith('the ') else name


# ---------------------------------------------------------------------------------------------------------------
# SNBT writer (FTB Quests' own layout: tabs, one entry per line, no commas)
# ---------------------------------------------------------------------------------------------------------------
def snbt(value, indent=0):
    tab = '\t' * indent
    if isinstance(value, dict):
        if not value:
            return '{ }'
        lines = [f'{tab}\t{k}: {snbt(v, indent + 1)}' for k, v in value.items()]
        return '{\n' + '\n'.join(lines) + f'\n{tab}}}'
    if isinstance(value, list):
        if not value:
            return '[ ]'
        if all(isinstance(v, str) for v in value):
            return '[' + ', '.join(snbt(v) for v in value) + ']'
        return '[\n' + '\n'.join(f'{tab}\t{snbt(v, indent + 1)}' for v in value) + f'\n{tab}]'
    if isinstance(value, tuple):
        kind, n = value
        return f'{n}{kind}'
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, float):
        return f'{value}d'
    if isinstance(value, int):
        return str(value)
    return json.dumps(value, ensure_ascii=False)


def quest_nbt(qu, key):
    tasks = [dict(id=qid(f'{key}/task{i}'), **t) for i, t in enumerate(qu.tasks)]
    rewards = [dict(id=qid(f'{key}/reward{i}'), **r) for i, r in enumerate(qu.rewards)]
    out = {'id': qu.id, 'title': qu.title, 'x': float(qu.x), 'y': float(qu.y)}
    if qu.icon:
        out['icon'] = qu.icon
    if qu.shape:
        out['shape'] = qu.shape
    if qu.size:
        out['size'] = float(qu.size)
    if qu.desc:
        out['description'] = qu.desc
    if qu.deps:
        out['dependencies'] = qu.deps
    if qu.min_deps:
        out['min_required_dependencies'] = qu.min_deps
    if qu.accept:
        out['require_sequential_tasks'] = True
    out['tasks'] = tasks
    out['rewards'] = rewards
    return out


def write_book(chapters, out):
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(f'{out}/chapters')
    data = {'version': 13, 'title': 'MDVLCraft', 'icon': 'minecraft:compass', 'default_reward_team': False, 'default_consume_items': False,
            'default_autoclaim_rewards': 'enabled', 'default_quest_shape': 'circle', 'default_quest_disable_jei': False,
            'emergency_items_cooldown': 300, 'drop_loot_crates': False, 'disable_gui': False, 'grid_scale': 0.5,
            'pause_game': False, 'lock_message': '', 'progression_mode': 'linear', 'detection_delay': 20}
    open(f'{out}/data.snbt', 'w').write(snbt(data) + '\n')
    for order, ch in enumerate(chapters):
        nbt = {'id': qid(f'chapter/{ch["file"]}'), 'group': '', 'order_index': order, 'filename': ch['file'], 'title': ch['title'],
               'icon': ch['icon'], 'subtitle': [ch['subtitle']], 'default_quest_shape': '', 'default_hide_dependency_lines': False,
               'quests': [quest_nbt(qu, f'{ch["file"]}/{qu.id}') for qu in ch['quests']]}
        open(f'{out}/chapters/{ch["file"]}.snbt', 'w').write(snbt(nbt) + '\n')


# ---------------------------------------------------------------------------------------------------------------
# Hidden advancements and reward crates (binder resources)
# ---------------------------------------------------------------------------------------------------------------
def write_level_advancements():
    out = f'{RES}/advancements/quests'
    os.makedirs(out, exist_ok=True)
    for n in LEVELS:
        adv = {'criteria': {'level': {'trigger': 'minecraft:tick', 'conditions': {'player': [
            {'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'type_specific': {'type': 'player', 'level': {'min': n}}}}]}}}}
        json.dump(adv, open(f'{out}/level_{n}.json', 'w'), indent=2)
        open(f'{out}/level_{n}.json', 'a').write('\n')


CRATES = {
    1: [('minecraft:iron_ingot', 4, 8, 10), ('minecraft:bread', 8, 16, 8), ('minecraft:arrow', 16, 32, 8), ('minecraft:golden_carrot', 4, 8, 6),
        ('minecraft:diamond', 1, 1, 2), ('minecraft:experience_bottle', 4, 8, 4)],
    2: [('minecraft:diamond', 1, 2, 6), ('minecraft:gold_ingot', 4, 8, 8), ('minecraft:lapis_lazuli', 8, 16, 8), ('minecraft:golden_apple', 1, 2, 4),
        ('minecraft:experience_bottle', 8, 12, 6), ('minecraft:ender_pearl', 2, 4, 4)],
    3: [('minecraft:diamond', 2, 4, 8), ('minecraft:netherite_scrap', 1, 1, 3), ('minecraft:blaze_rod', 4, 8, 6), ('minecraft:golden_apple', 2, 3, 5),
        ('minecraft:experience_bottle', 12, 16, 6), ('minecraft:magma_cream', 4, 8, 4)],
    4: [('minecraft:netherite_scrap', 1, 2, 6), ('minecraft:totem_of_undying', 1, 1, 3), ('minecraft:golden_apple', 3, 5, 6),
        ('minecraft:ender_pearl', 8, 16, 6), ('minecraft:experience_bottle', 16, 24, 6), ('minecraft:diamond_block', 1, 1, 3)],
    5: [('minecraft:enchanted_golden_apple', 1, 1, 3), ('minecraft:netherite_ingot', 1, 1, 4), ('minecraft:totem_of_undying', 1, 2, 4),
        ('minecraft:experience_bottle', 24, 32, 6), ('minecraft:diamond_block', 2, 3, 4)],
}


def write_crates():
    out = f'{RES}/loot_tables/quests'
    os.makedirs(out, exist_ok=True)
    for tier, entries in CRATES.items():
        table = {'type': 'minecraft:chest', 'pools': [{'rolls': tier + 2, 'entries': [
            {'type': 'minecraft:item', 'name': name, 'weight': weight,
             'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
            for name, lo, hi, weight in entries]}]}
        json.dump(table, open(f'{out}/tier{tier}.json', 'w'), indent=2)
        open(f'{out}/tier{tier}.json', 'a').write('\n')


def main(base=os.path.join(REPO, 'base')):
    chapters = []
    ch, gate = first_steps()
    chapters.append(ch)
    for tier in range(1, 6):
        ch, gate = tier_chapter(tier, gate)
        chapters.append(ch)
    chapters.append(gates_chapter())   # after Legend; no dependency on the other chapters
    chapters.append(food_chapter())    # Food and Brewing likewise stand on their own
    chapters.append(brewing_chapter())
    ids = [qu.id for c in chapters for qu in c['quests']]
    assert len(ids) == len(set(ids)), 'duplicate quest ids'
    for side in ('client', 'server'):
        write_book(chapters, os.path.join(base, side, 'overrides', 'config', 'ftbquests', 'quests'))
    write_level_advancements()
    write_gate_advancements()
    write_crates()
    print(f'{len(chapters)} chapters, {len(ids)} quests, {sum(1 for b in B if b["lair"] and not arena_lair(b["lair"][0]) and b["lair"][0] not in CATACLYSM_EYES)} lair compasses')


if __name__ == '__main__':
    main()
