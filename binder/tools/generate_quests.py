"""Generates the pack's FTB Quests book: six chapters, one per boss tier (First Steps, then Wanderer to Legend).

Each tier chapter has a top row of progress quests (kill counts, an experience level, places to find) ending in a
capstone that opens the next chapter, and below it one column per boss: three lead-up quests themed on that boss, the
last of which rewards a compass to the boss's lair (/mdvlquest compass, command/QuestCompass) when it lives in a
structure, then the kill. Capstones need a few of the tier's bosses (min_required_dependencies), never all of them.

Writes:
  base/{client,server}/overrides/config/ftbquests/quests/  the quest book (SNBT, read by FTB Quests on the server)
  src/main/resources/data/mdvlcraft/advancements/quests/   hidden "reach experience level N" advancements
  src/main/resources/data/mdvlcraft/loot_tables/quests/    the boss reward crates, one per tier

Usage: python3 binder/tools/generate_quests.py   (build_packs.py also writes the book into its base before packing)
"""
import hashlib, json, os, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.join(HERE, '..', '..')
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'data', 'mdvlcraft')
OVERWORLD, NETHER, END = 'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'
LEVELS = [10, 15, 25, 30, 40, 50]


def qid(key):
    return hashlib.sha1(key.encode()).hexdigest()[:16].upper()


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


def command(cmd):
    return {'type': 'command', 'command': cmd, 'elevate_perms': True, 'silent': True}


def crate(tier):
    return command(f'/loot give {{p}} loot mdvlcraft:quests/tier{tier}')


def compass(struct, dim, name):
    return command(f'/mdvlquest compass {{p}} {struct} {dim} {name}')


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
    ('Into the Green', [biome('#minecraft:is_jungle')], [give('minecraft:bread', 8)], 'Deep in the jungle, a temple of moss hides a giant.'),
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
    ('A Fallen Kingdom', [biome('minecraft:plains')], [give('minecraft:cooked_beef', 8)], 'Ruins of a kingdom lie on the plains. Its knight never left.'),
    ('Knightly Steel', [item('minecraft:iron_chestplate')], [give('minecraft:iron_ingot', 6)], 'Meet steel with steel.'),
    ('Provisions', [item('minecraft:cooked_beef', 16)], [xp(200)], 'The kingdom is large. Pack food.'),
], ('legendary_monsters:collapsed_kingdom', OVERWORLD), 'It haunts the arena of a Collapsed Kingdom on plains and in forests.')
boss(2, 'cloud_golem', 'the Cloud Golem', 'legendary_monsters:cloud_golem', 'minecraft:white_wool', [
    ('Peak Season', [biome('#minecraft:is_mountain')], [give('minecraft:cooked_beef', 8)], 'Climb the mountains. A temple floats near the peaks.'),
    ('Soft Landing', [item('minecraft:water_bucket')], [give('minecraft:feather', 16)], 'A long fall waits for the careless. Carry water.'),
    ('Night Flyers', [kill('minecraft:phantom', 5)], [xp(200)], 'Phantoms circle the heights. Clear the sky.'),
], ('legendary_monsters:cloudy_temple', OVERWORLD), 'It lives in a Cloudy Temple on mountain peaks, meadows and cherry groves.')
boss(2, 'ancient_guardian', 'the Ancient Guardian', 'legendary_monsters:ancient_guardian', 'minecraft:mossy_stone_bricks', [
    ('Dark Woods', [biome('minecraft:dark_forest')], [give('minecraft:cooked_beef', 8)], 'An old stronghold hides among dark forests and taiga.'),
    ('Bonebreaker', [kill('minecraft:skeleton', 20)], [give('minecraft:arrow', 32)], 'Skeletons guard the approach.'),
    ('Diamond Edge', [item('minecraft:diamond_sword')], [xp(200)], 'Only a diamond blade will crack its stone.'),
], ('legendary_monsters:ancient_stronghold', OVERWORLD), 'It guards an Ancient Stronghold in dark forest and taiga.')
boss(2, 'elder_guardian', 'the Elder Guardian', 'minecraft:elder_guardian', 'minecraft:prismarine', [
    ('Deep Water', [biome('#minecraft:is_deep_ocean')], [give('minecraft:cooked_cod', 8)], 'Ocean monuments sit on the floor of deep oceans.'),
    ('Drowned Sailors', [kill('minecraft:drowned', 15)], [give('minecraft:prismarine_shard', 8)], 'The drowned swarm near the monument.'),
    ('Sea Legs', [item('minecraft:oak_boat')], [xp(200)], 'Bring a boat for the crossing.'),
], ('minecraft:monument', OVERWORLD), 'It rules an Ocean Monument in deep oceans.')
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
    ('Blackened Bones', [kill('minecraft:wither_skeleton', 15)], [give('minecraft:coal', 16)], 'Wither skeletons stalk the old fortress remains.'),
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
], ('minecraft:stronghold', OVERWORLD), 'Find a stronghold and its End portal.')
boss(4, 'ender_guardian', 'the Ender Guardian', 'cataclysm:ender_guardian', 'minecraft:purpur_block', [
    ('The Far Lands', [dimension(END)], [give('minecraft:cooked_beef', 8)], 'A ruined citadel floats on the outer End islands.'),
    ('Outer Island Patrol', [kill('minecraft:enderman', 20)], [give('minecraft:ender_pearl', 8)], 'Endermen crowd the outer islands.'),
    ('Chorus', [item('minecraft:chorus_fruit', 16)], [xp(400)], 'Chorus fruit gets you out of tight spots.'),
], ('cataclysm:ruined_citadel', END), 'It guards the Ruined Citadel on the outer End highlands.')
boss(4, 'endersent', 'the Endersent', 'legendary_monsters:endersent', 'minecraft:end_stone_bricks', [
    ('Highlands', [biome('minecraft:end_highlands')], [give('minecraft:cooked_beef', 8)], 'A ruined tower stands on the End highlands.'),
    ('Tall Shadows', [kill('minecraft:enderman', 25)], [give('minecraft:ender_pearl', 8)], 'The Endersent commands the endermen.'),
    ('Pearls', [item('minecraft:ender_pearl', 16)], [xp(400)], 'Gather pearls to keep up with it.'),
], ('legendary_monsters:ancient_tower_remains', END), 'It haunts the Ancient Tower Remains on the outer End islands.')
boss(4, 'annihilation_pursuer', 'the Annihilation Pursuer', 'legendary_monsters:annihilation_pursuer', 'minecraft:end_rod', [
    ('Midlands', [biome('minecraft:end_midlands')], [give('minecraft:cooked_beef', 8)], 'Something not of this world crashed on the End midlands.'),
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
    ('The Far End', [dimension(END)], [give('minecraft:cooked_beef', 16)], 'A void cathedral stands on the End highlands.'),
    ('Cathedral Watch', [kill('minecraft:enderman', 30)], [give('minecraft:ender_pearl', 16)], 'Clear the endermen around the cathedral.'),
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
        ('Fortress Found', [structure('minecraft:fortress')], 'Find a Nether fortress.'),
        ('Bastion Breaker', [structure('minecraft:bastion_remnant')], 'Find a bastion remnant.')],
    4: [('Void Walkers', [kill('minecraft:enderman', 50)], 'Endermen carry the pearls you need.'),
        ('Shell Shock', [kill('minecraft:shulker', 20)], 'Shulkers guard the End cities.'),
        ('Deeplings', [kill('cataclysm:deepling', 15)], 'Deeplings swarm around the Sunken City.'),
        ('The Stronghold', [structure('minecraft:stronghold')], 'Find a stronghold.'),
        ('City of the End', [structure('minecraft:end_city')], 'Find an End city.')],
    5: [('Echoes in the Dark', [structure('minecraft:ancient_city')], 'Find an ancient city.')],
}


def first_steps():
    q = []
    welcome = Quest('fs_welcome', 'Welcome to MDVLCraft', 0, 0, [checkmark()], [give('minecraft:bread', 8), give('minecraft:torch', 16)],
                    ['Pick your origin, class and martial path, then tick this quest.', '',
                     'This book points you at the next boss, in order of difficulty. Each chapter ends in a capstone that opens the next.', '',
                     'Take one quest at a time: tick Accept Quest to start it. Type /quest cancel to abandon it and lose its progress.'],
                    icon='minecraft:writable_book', shape='gear', size=1.5, accept=False)
    zombies = Quest('fs_zombies', 'First Blood', 2, -1, [kill('minecraft:zombie', 20)], [give('minecraft:iron_ingot', 4), xp(50)], ['Zombies are everywhere at night.'], [welcome.id])
    skeletons = Quest('fs_skeletons', 'Bone Collector', 2, 0, [kill('minecraft:skeleton', 20)], [give('minecraft:arrow', 32), xp(50)], ['Skeletons keep their distance. Close it.'], [welcome.id])
    spiders = Quest('fs_spiders', 'Eight Legs', 2, 1, [kill('minecraft:spider', 10)], [give('minecraft:bread', 8), xp(50)], ['Spiders climb walls. Watch above.'], [welcome.id])
    creepers = Quest('fs_creepers', 'Boom', 4, 1, [kill('minecraft:creeper', 5)], [give('minecraft:gunpowder', 8), xp(50)], ['Kill creepers before they reach you.'], [spiders.id])
    village = Quest('fs_village', 'Neighbours', 0, 2, [structure('#minecraft:village')], [give('minecraft:emerald', 4)], ['Find a village. Its people trade, and some can be recruited.'], [welcome.id])
    bed = Quest('fs_bed', 'A Place to Rest', 0, -2, [advancement('minecraft:adventure/sleep_in_bed')], [give('minecraft:torch', 16)], ['Sleep in a bed to set your spawn.'], [welcome.id])
    backpack = Quest('fs_backpack', 'Pack Up', 2, 2, [item('backpacked:backpack')], [give('minecraft:leather', 8)],
                     ['Craft a backpack. Its augment bays add effects like Funnelling, Tool Swapper, Void and Lantern.'], [village.id])
    lvl = Quest('fs_level', 'Getting Stronger', 4, -1, [level(10)], [xp(100)], ['Reach experience level 10.'], [zombies.id])
    hunter = Quest('fs_hunter', 'Monster Hunter', 4, 0, [mob_kills(25)], [give('minecraft:cooked_beef', 8), xp(100)], ['Kill 25 hostile mobs of any kind.'], [skeletons.id])
    iron = Quest('fs_iron', 'Iron Age', 6, 0, [item('minecraft:iron_helmet'), item('minecraft:iron_chestplate'), item('minecraft:iron_leggings'), item('minecraft:iron_boots')],
                 [give('minecraft:diamond', 1), xp(150)], ['Wear a full set of iron armour. The Wanderer chapter opens.'], [lvl.id, hunter.id],
                 icon='minecraft:iron_chestplate', shape='hexagon', size=1.5)
    q += [welcome, zombies, skeletons, spiders, creepers, village, bed, backpack, lvl, hunter, iron]
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
                rewards.append(compass(struct, dim, strip_article(b['name'])))
                desc = desc + ' Reward: a compass to the lair.'
            sq = Quest(f'{b["key"]}_{step}', title, bx, 2 + step, tasks, rewards, [desc], [prev_id], icon=None)
            q.append(sq)
            prev_id = sq.id
        kq = Quest(f'{b["key"]}_kill', f'Defeat {b["name"]}', bx, 5.25, [kill(b['entity'], 1)], [crate(tier), xp(250 * tier)],
                   [b['hint']], [prev_id], icon=b['icon'], shape='octagon', size=1.5)
        q.append(kq)
        kill_ids.append(kq.id)

    cap_x = max(2 + len(PROGRESS[tier]) * 2 + 2, len(bosses) * 2)
    cap_tasks = [level(t['level']), mob_kills(t['kills'])]
    cap_title = {1: 'Proven', 2: 'Into the Fire', 3: 'Forged in Flame', 4: 'Dragonslayer', 5: 'Living Legend'}[tier]
    cap_rewards = {1: [give('minecraft:diamond', 2)], 2: [give('minecraft:obsidian', 10), give('minecraft:flint_and_steel')],
                   3: [give('minecraft:netherite_upgrade_smithing_template')], 4: [give('minecraft:enchanted_golden_apple')],
                   5: [give('minecraft:nether_star'), crate(5)]}[tier]
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
            'default_autoclaim_rewards': 'disabled', 'default_quest_shape': 'circle', 'default_quest_disable_jei': False,
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
    ids = [qu.id for c in chapters for qu in c['quests']]
    assert len(ids) == len(set(ids)), 'duplicate quest ids'
    for side in ('client', 'server'):
        write_book(chapters, os.path.join(base, side, 'overrides', 'config', 'ftbquests', 'quests'))
    write_level_advancements()
    write_crates()
    print(f'{len(chapters)} chapters, {len(ids)} quests, {sum(1 for b in B if b["lair"])} lair compasses')


if __name__ == '__main__':
    main()
