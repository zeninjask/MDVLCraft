package com.mdvlcraft.binder.content;

import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Mobs of otherwise spawn-blocked mods that do spawn in the wild (WildSpawnsBiomeModifier, data/mdvlcraft/forge/biome_modifier/wild_spawns.json):
 * Iron's mages and the Aquamancer as rare hostile spawns in the dark (groups of up to 5), tougher than their structure
 * versions, and the Cursed Fate characters as very rare ones. Every mage, wild or not, drops 30 experience (and gives
 * 150 skill experience, the #mdvlcraft:mages case in each tree's experience.json). Mages never drop
 * inks (the scroll forge is disabled). Every hostile mob also gets {@link #HOSTILE_HEALTH} times its health and deals
 * {@link #HOSTILE_DAMAGE} times its damage to players.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class WildSpawns {
    /**
     * Spawn weight in the overworld's monster list, the chance a natural spawn goes ahead, and whether the binder sets its
     * placement rule (dark enough, on the ground). The characters' own mods register theirs, Cursed Fate directly through
     * SpawnPlacements, which throws on a second registration. A spawn comes in a group of 1 to maxGroup.
     */
    record Wild(int weight, double chance, boolean placement, int maxGroup) {
    }

    private static final Wild MAGE = new Wild(2, 0.75, true, 5);
    private static final Wild CHARACTER = new Wild(1, 0.05, false, 1);
    static final Map<ResourceLocation, Wild> ALLOWED = Map.ofEntries(
        Map.entry(id("irons_spellbooks", "archevoker"), MAGE),
        Map.entry(id("irons_spellbooks", "necromancer"), MAGE),
        Map.entry(id("irons_spellbooks", "cryomancer"), MAGE),
        Map.entry(id("irons_spellbooks", "pyromancer"), MAGE),
        Map.entry(id("irons_spellbooks", "priest"), MAGE),
        Map.entry(id("irons_spellbooks", "apothecarist"), MAGE),
        Map.entry(id("traveloptics", "aquamancer"), MAGE),
        Map.entry(id("cursedfate", "shinjuku_showdown_gojoa"), CHARACTER),
        Map.entry(id("cursedfate", "shinjuku_showdown_sukunaa"), CHARACTER),
        Map.entry(id("naoyaaddon", "naoya_npc"), CHARACTER),
        Map.entry(id("naoyaaddon", "naobito_npc"), CHARACTER)
    );
    private static final String WILD = "mdvlcraft.wild";
    private static final UUID WILD_HEALTH = UUID.fromString("3f6b1a52-8c1e-4d7a-9b0e-2a51c7d4e901");
    private static final UUID WILD_ARMOR = UUID.fromString("3f6b1a52-8c1e-4d7a-9b0e-2a51c7d4e902");
    private static final UUID HOSTILE_HEALTH_ID = UUID.fromString("3f6b1a52-8c1e-4d7a-9b0e-2a51c7d4e903");
    private static final double WILD_HEALTH_BONUS = 0.5;
    private static final double WILD_ARMOR_BONUS = 6.0;
    public static final double HOSTILE_HEALTH = 1.2;
    public static final double HOSTILE_DAMAGE = 1.1;
    private static final int MAGE_EXPERIENCE = 30;

    private WildSpawns() {
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static boolean allowed(EntityType<?> type) {
        return ALLOWED.containsKey(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    /** Registered on the mod bus (MDVLBinder). */
    @SuppressWarnings("unchecked")
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        ALLOWED.forEach((id, wild) -> {
            if (!wild.placement()) {
                return;
            }

            EntityType<Mob> type = (EntityType<Mob>)ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type == null || !id.equals(ForgeRegistries.ENTITY_TYPES.getKey(type))) {
                throw new IllegalStateException("Wild spawn " + id + " is not registered");
            }

            event.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random)
                    && Mob.checkMobSpawnRules(entityType, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(FinalizeSpawn event) {
        Mob mob = event.getEntity();
        Wild wild = ALLOWED.get(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
        if (!event.isSpawnCancelled() && event.getSpawnType() == MobSpawnType.NATURAL && wild != null) {
            if (mob.getRandom().nextDouble() >= wild.chance()) {
                event.setSpawnCancelled(true);
                return;
            }

            mob.getPersistentData().putBoolean(WILD, true);
            addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), WILD_HEALTH, "Wild spawn health", WILD_HEALTH_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL);
            addPermanent(mob.getAttribute(Attributes.ARMOR), WILD_ARMOR, "Wild spawn armour", WILD_ARMOR_BONUS, AttributeModifier.Operation.ADDITION);
            mob.setHealth(mob.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
            if (mob.getPersistentData().getBoolean(WILD)) {
                mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));
            }

            if (mob.getType().getCategory() == MobCategory.MONSTER) {
                AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
                if (health != null && health.getModifier(HOSTILE_HEALTH_ID) == null) {
                    boolean full = mob.getHealth() >= mob.getMaxHealth();
                    health.addPermanentModifier(new AttributeModifier(HOSTILE_HEALTH_ID, "Hostile mob health", HOSTILE_HEALTH - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    if (full) {
                        mob.setHealth(mob.getMaxHealth());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof Mob mob
            && (mob.getType().getCategory() == MobCategory.MONSTER || allowed(mob.getType()))) {
            event.setAmount(event.getAmount() * (float)HOSTILE_DAMAGE);
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (isMage(event.getEntity().getType())) {
            event.setDroppedExperience(MAGE_EXPERIENCE);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (isMage(event.getEntity().getType())) {
            event.getDrops().removeIf(drop -> {
                ResourceLocation item = ForgeRegistries.ITEMS.getKey(drop.getItem().getItem());
                return item.getNamespace().equals("irons_spellbooks") && item.getPath().endsWith("_ink");
            });
        }
    }

    private static boolean isMage(EntityType<?> type) {
        return ALLOWED.get(ForgeRegistries.ENTITY_TYPES.getKey(type)) == MAGE;
    }

    private static void addPermanent(AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation operation) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
        }
    }
}
