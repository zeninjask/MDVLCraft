package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.config.BinderConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "mdvlcraft")
public final class MobGear {
    public static final TagKey<EntityType<?>> ARMED = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("mdvlcraft", "armed_mobs"));
    public static final TagKey<EntityType<?>> ARMOURED = TagKey.create(
        Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("mdvlcraft", "armoured_mobs")
    );
    private static final Set<String> WEAPON_MODS = Set.of("epicfight", "magistuarmory");
    private static final Set<String> ARMOR_MODS = Set.of("minecraft", "magistuarmory", "fantasy_armor");
    /** Fantasy Armour's pieces are set to iron's values in its config (fantasy_armor-armor_attributes.toml), not on the item. */
    private static final String IRON_BY_CONFIG = "fantasy_armor";
    private static final String PENDING = "mdvlcraft.gear_pending";
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static List<Item> weapons;
    private static List<Item> weaponsUpToIron;
    private static Map<EquipmentSlot, List<Item>> armor;

    private MobGear() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalizeSpawn(FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (!event.isSpawnCancelled() && ContentEvents.AMBIENT_SPAWNS.contains(event.getSpawnType()) && (mob.getType().is(ARMED) || mob.getType().is(ARMOURED))
            )
         {
            mob.getPersistentData().putBoolean("mdvlcraft.gear_pending", true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && mob.getPersistentData().contains("mdvlcraft.gear_pending")) {
            mob.getPersistentData().remove("mdvlcraft.gear_pending");
            if (BinderConfig.mobGear()) {
                equip(mob, mob.getRandom());
            }
        }
    }

    private static void equip(Mob mob, RandomSource random) {
        if (mob.getType().is(ARMED) && random.nextDouble() < BinderConfig.mobWeaponChance() && !holdsRanged(mob)) {
            List<Item> pool = BinderConfig.mobWeaponsUpToIron() ? weaponsUpToIron() : weapons();
            if (!pool.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pool.get(random.nextInt(pool.size()))));
            }
        }

        if (mob.getType().is(ARMOURED)) {
            boolean armoured = random.nextDouble() < BinderConfig.mobArmorChance();

            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack worn = mob.getItemBySlot(slot);
                boolean tooStrong = !worn.isEmpty() && worn.getItem() instanceof ArmorItem piece && !upToIron(piece);
                if (tooStrong || armoured && worn.isEmpty() && random.nextDouble() < BinderConfig.mobArmorPieceChance()) {
                    List<Item> pool = armor().get(slot);
                    mob.setItemSlot(slot, pool.isEmpty() ? ItemStack.EMPTY : new ItemStack(pool.get(random.nextInt(pool.size()))));
                }
            }
        }
    }

    private static boolean holdsRanged(Mob mob) {
        Item held = mob.getMainHandItem().getItem();
        return held instanceof ProjectileWeaponItem || held instanceof TridentItem;
    }

    private static List<Item> weapons() {
        if (weapons == null) {
            List<Item> found = new ArrayList<>();

            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && WEAPON_MODS.contains(id.getNamespace()) && isMeleeWeapon(item, id)) {
                    found.add(item);
                }
            }

            weapons = List.copyOf(found);
        }

        return weapons;
    }

    private static List<Item> weaponsUpToIron() {
        if (weaponsUpToIron == null) {
            weaponsUpToIron = weapons()
                .stream()
                .filter(
                    item -> item instanceof TieredItem tiered
                        && tiered.getTier().getLevel() <= Tiers.IRON.getLevel()
                        && tiered.getTier().getAttackDamageBonus() <= Tiers.IRON.getAttackDamageBonus()
                )
                .toList();
        }

        return weaponsUpToIron;
    }

    private static boolean isMeleeWeapon(Item item, ResourceLocation id) {
        if (!(item instanceof ArmorItem) && !(item instanceof ShieldItem) && !(item instanceof ProjectileWeaponItem) && !(item instanceof TridentItem)) {
            return !id.getPath().contains("lance") && !DisabledContent.isForbidden(item)
                ? item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)
                : false;
        } else {
            return false;
        }
    }

    private static Map<EquipmentSlot, List<Item>> armor() {
        if (armor == null) {
            Map<EquipmentSlot, List<Item>> found = new EnumMap<>(EquipmentSlot.class);

            for (EquipmentSlot slot : ARMOR_SLOTS) {
                found.put(slot, new ArrayList<>());
            }

            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null
                    && ARMOR_MODS.contains(id.getNamespace())
                    && item instanceof ArmorItem piece
                    && upToIron(piece)
                    && found.containsKey(piece.getEquipmentSlot())) {
                    found.get(piece.getEquipmentSlot()).add(item);
                }
            }

            found.replaceAll((slot, items) -> List.copyOf((Collection<? extends Item>)items));
            armor = found;
        }

        return armor;
    }

    private static boolean upToIron(ArmorItem piece) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(piece);
        if (id != null && IRON_BY_CONFIG.equals(id.getNamespace())) {
            return true;
        }

        return piece.getDefense() <= ArmorMaterials.IRON.getDefenseForType(piece.getType())
            && piece.getToughness() <= ArmorMaterials.IRON.getToughness()
            && piece.getMaterial().getKnockbackResistance() <= ArmorMaterials.IRON.getKnockbackResistance();
    }
}
