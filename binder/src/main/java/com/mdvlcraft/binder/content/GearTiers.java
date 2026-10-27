package com.mdvlcraft.binder.content;

import com.google.common.collect.Multimap;
import com.stereowalker.reforged.Reforged;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reforged tiers (data: kubejs/data/mdvlcraft/tiered_modifiers, Reforged's own pools are switched off).
 * Its pools name mdvlcraft:tiered/weapons, /bows, /armour and /tools, which ItemVerifierMixin resolves here: a weapon is
 * anything that deals attack damage in the main hand except pickaxes, shovels and hoes; a bow is any projectile weapon;
 * armour is any armour piece; a tool is a pickaxe, shovel or hoe. Each kind rolls the same seven tiers (mdvlcraft:<kind>/<tier>);
 * the tier files carry no attributes: the tier scales the item's own attack damage, armour or arrow damage here, so four
 * armour pieces add up instead of compounding, and adds half that share of attack speed or draw speed, and 5x it as flat
 * armour toughness per piece (Godsmithed +25%: +12.5% attack speed, +12.5% draw speed, +1.25 toughness). Godsmithed gear has four times the durability (ItemStackMixin).
 * Reforged itself tiers the main inventory and open containers; on login the armour and off-hand slots are rolled too.
 * A tier from before this table (any id outside the four kinds) is dropped first, so old gear rerolls on the new table.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GearTiers {
    public static final String TAG_PREFIX = "mdvlcraft:tiered/";
    private static final List<String> KINDS = List.of("weapon/", "bow/", "armor/", "tool/");
    private static final Map<String, Double> SCALE = Map.of("broken", -0.20, "crude", -0.10, "worn", -0.05, "regular", 0.0,
        "fine", 0.05, "mastercrafted", 0.10, "godsmithed", 0.25);
    private static final UUID BASE_ATTACK_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID ARROW_DAMAGE = UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c13");
    private static final UUID DRAW_SPEED = UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c14");
    private static final UUID ATTACK_SPEED = UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c15");
    /** One per armour slot, so a full set's toughness bonuses all count. */
    private static final Map<EquipmentSlot, UUID> TOUGHNESS = Map.of(
        EquipmentSlot.HEAD, UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c16"),
        EquipmentSlot.CHEST, UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c17"),
        EquipmentSlot.LEGS, UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c18"),
        EquipmentSlot.FEET, UUID.fromString("5d1c7f2e-3b8a-4e61-9f0c-2a7d4b6e8c19"));

    private GearTiers() {
    }

    public static boolean matches(String kind, Item item) {
        return switch (kind) {
            case "weapons" -> !(item instanceof ProjectileWeaponItem || isTool(item))
                && item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
            case "bows" -> item instanceof ProjectileWeaponItem;
            case "armour" -> item instanceof ArmorItem;
            case "tools" -> isTool(item);
            default -> throw new IllegalArgumentException("unknown Reforged gear kind " + kind);
        };
    }

    private static boolean isTool(Item item) {
        return item instanceof PickaxeItem || item instanceof ShovelItem || item instanceof HoeItem;
    }

    /** The stack's tier name ("fine") when it has one of this table's tiers, else null. */
    public static String tier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Reforged")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getCompound("Reforged").getString("Tier"));
        if (id == null || !id.getNamespace().equals("mdvlcraft")) {
            return null;
        }
        for (String kind : KINDS) {
            if (id.getPath().startsWith(kind)) {
                return id.getPath().substring(kind.length());
            }
        }
        return null;
    }

    /** Called before Reforged rolls a stack: a tier from an older table is removed so the stack rolls again. */
    public static void dropStaleTier(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Reforged") && tier(stack) == null) {
            stack.removeTagKey("Reforged");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttributes(ItemAttributeModifierEvent event) {
        String tier = tier(event.getItemStack());
        Double scale = tier == null ? null : SCALE.get(tier);
        if (scale == null || scale == 0) {
            return;
        }
        AttributeModifier.Operation add = AttributeModifier.Operation.ADDITION;
        if (scale(event, Attributes.ARMOR, scale) && TOUGHNESS.containsKey(event.getSlotType())) {
            event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS.get(event.getSlotType()), "Gear tier", scale * 5, add));
        }
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        if (scale(event, Attributes.ATTACK_DAMAGE, scale)) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED, "Gear tier", scale / 2, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (event.getItemStack().getItem() instanceof ProjectileWeaponItem) {
            Attribute arrows = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "arrow_damage"));
            Attribute draw = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "draw_speed"));
            if (arrows != null) {
                event.addModifier(arrows, new AttributeModifier(ARROW_DAMAGE, "Gear tier", scale, add));
            }
            if (draw != null) {
                event.addModifier(draw, new AttributeModifier(DRAW_SPEED, "Gear tier", scale / 2, add));
            }
        }
    }

    /** Scales the item's own flat modifiers of one attribute; attack damage counts the player's base 1 as part of the weapon. */
    private static boolean scale(ItemAttributeModifierEvent event, Attribute attribute, double scale) {
        Multimap<Attribute, AttributeModifier> all = event.getModifiers();
        List<AttributeModifier> flat = all.get(attribute).stream().filter(m -> m.getOperation() == AttributeModifier.Operation.ADDITION).toList();
        for (AttributeModifier m : flat) {
            double base = m.getId().equals(BASE_ATTACK_DAMAGE) ? 1 : 0;
            event.removeModifier(attribute, m);
            event.addModifier(attribute, new AttributeModifier(m.getId(), m.getName(), (m.getAmount() + base) * (1 + scale) - base, m.getOperation()));
        }
        return !flat.isEmpty();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getInventory().armor.forEach(GearTiers::roll);
        player.getInventory().offhand.forEach(GearTiers::roll);
    }

    private static void roll(ItemStack stack) {
        Reforged.attemptToAffixTier(stack);
    }
}
