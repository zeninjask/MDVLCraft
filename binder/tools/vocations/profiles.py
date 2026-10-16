"""The bonus profile of every archetype's base: which class spokes it keeps and which generic twigs it favours.

Every archetype of a class used to have the same class spokes and the same fifteen generic twigs, so only its signature
stats told it apart. Now each keeps three of its class's spokes (CLASS) and spends the same number of twig nodes on its
own six major and three minor twig bonuses (MAJOR, MINOR; titles from generate_skill_trees.GENERIC).

Balance: a base has exactly as many bonus nodes as before and every node keeps its per-node value, so each archetype
gets the same number of equally weighted bonus nodes and costs the same points; only the mix differs. The class
spokes an archetype drops are paid back as twig nodes (9 per spoke).
"""

CLASS = {
    # Warrior pool: Toughness (health), Bulk (armour), Stand Firm (knockback resistance), Second Wind (regeneration)
    "guardian": ("Bulk", "Toughness", "Stand Firm"),
    "brute": ("Toughness", "Stand Firm", "Second Wind"),
    "samurai": ("Toughness", "Bulk", "Second Wind"),
    "berserker": ("Toughness", "Second Wind", "Bulk"),
    "monk": ("Second Wind", "Stand Firm", "Toughness"),
    # Mage pool: Mana Well, Flow (mana regen), Quick Casting, Scholar (experience), Warding (spell resistance)
    "blaze": ("Mana Well", "Quick Casting", "Flow"),
    "frost": ("Warding", "Mana Well", "Flow"),
    "water": ("Flow", "Scholar", "Quick Casting"),
    "lightning": ("Quick Casting", "Mana Well", "Scholar"),
    "holy": ("Warding", "Flow", "Scholar"),
    "warlock": ("Mana Well", "Quick Casting", "Warding"),
    "necromancer": ("Mana Well", "Scholar", "Warding"),
    "druid": ("Flow", "Scholar", "Warding"),
    "spirit": ("Quick Casting", "Flow", "Warding"),
    # Rogue pool: Fleet (speed), Quick Hands (attack speed), Shadow (stealth), Recovery (stamina regen), Quick Draught
    "assassin": ("Shadow", "Quick Hands", "Recovery"),
    "thief": ("Fleet", "Shadow", "Quick Draught"),
    "stalker": ("Quick Hands", "Recovery", "Fleet"),
    "wanderer": ("Fleet", "Recovery", "Quick Draught"),
}

MAJOR = {
    "guardian": ("Hardened", "Vitality", "Footing", "Willpower", "Endurance", "Mending"),
    "brute": ("Vitality", "Might", "Footing", "Hearty", "Mending", "Endurance"),
    "samurai": ("Readiness", "Might", "Endurance", "Second Breath", "Swiftness", "Focus"),
    "berserker": ("Might", "Readiness", "Swiftness", "Mending", "Vitality", "Second Breath"),
    "monk": ("Swiftness", "Second Breath", "Endurance", "Meditation", "Attunement", "Bounce"),
    "blaze": ("Focus", "Attunement", "Meditation", "Readiness", "Swiftness", "Willpower"),
    "frost": ("Hardened", "Vitality", "Willpower", "Attunement", "Meditation", "Footing"),
    "water": ("Meditation", "Swiftness", "Mending", "Attunement", "Bounce", "Second Breath"),
    "lightning": ("Swiftness", "Focus", "Readiness", "Attunement", "Bounce", "Second Breath"),
    "holy": ("Mending", "Vitality", "Willpower", "Meditation", "Hearty", "Attunement"),
    "warlock": ("Attunement", "Focus", "Willpower", "Meditation", "Swiftness", "Might"),
    "necromancer": ("Attunement", "Vitality", "Hearty", "Focus", "Willpower", "Mending"),
    "druid": ("Mending", "Hearty", "Meditation", "Vitality", "Swiftness", "Footing"),
    "spirit": ("Readiness", "Swiftness", "Meditation", "Endurance", "Focus", "Might"),
    "assassin": ("Might", "Readiness", "Swiftness", "Second Breath", "Focus", "Bounce"),
    "thief": ("Swiftness", "Bounce", "Hearty", "Second Breath", "Readiness", "Endurance"),
    "stalker": ("Readiness", "Focus", "Endurance", "Swiftness", "Might", "Second Breath"),
    "wanderer": ("Hearty", "Endurance", "Swiftness", "Vitality", "Mending", "Bounce"),
}

MINOR = {
    "guardian": ("Hearty", "Second Breath", "Might"),
    "brute": ("Hardened", "Readiness", "Bounce"),
    "samurai": ("Vitality", "Footing", "Willpower"),
    "berserker": ("Hearty", "Bounce", "Endurance"),
    "monk": ("Readiness", "Mending", "Focus"),
    "blaze": ("Vitality", "Hearty", "Mending"),
    "frost": ("Focus", "Mending", "Hearty"),
    "water": ("Focus", "Vitality", "Willpower"),
    "lightning": ("Meditation", "Willpower", "Endurance"),
    "holy": ("Hardened", "Focus", "Footing"),
    "warlock": ("Vitality", "Mending", "Readiness"),
    "necromancer": ("Meditation", "Hardened", "Endurance"),
    "druid": ("Attunement", "Bounce", "Willpower"),
    "spirit": ("Attunement", "Second Breath", "Bounce"),
    "assassin": ("Endurance", "Vitality", "Hearty"),
    "thief": ("Mending", "Footing", "Vitality"),
    "stalker": ("Hardened", "Bounce", "Vitality"),
    "wanderer": ("Footing", "Second Breath", "Attunement"),
}
