package com.mdvlcraft.binder.orca;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Weapons from Orca's JujutsuCraft, with the Epic Fight movesets of Jason's Jujutsu Epic (data/mdvlcraft/capabilities/
 * weapons, types jjc_*), both used with permission (see the README's credits). Models and textures are under
 * assets/mdvlcraft/.../jjc/. Their stats match Cursed Fate's cursed tools (9 attack damage, the axes 10); Playful Cloud
 * hits twice as hard. Their own JujutsuCraft abilities aren't carried over: each has an innate skill (jjk/JjkSkill).
 * Willow sells them (integration/Willow).
 */
public final class OrcaWeapons {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    /** Like Cursed Fate's cursed tools: netherite level, 2031 uses, +5 damage. */
    static final Tier CURSED_TOOL = new Tier() {
        public int getUses() {
            return 2031;
        }

        public float getSpeed() {
            return 4.0F;
        }

        public float getAttackDamageBonus() {
            return 5.0F;
        }

        public int getLevel() {
            return 4;
        }

        public int getEnchantmentValue() {
            return 22;
        }

        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };
    /** Each weapon and its Willow price in jujutsu tokens. */
    public static final Map<RegistryObject<Item>, Integer> PRICES = new LinkedHashMap<>();
    public static final RegistryObject<Item> FESTER_LIFE_BLADE = sword("fester_life_blade", 4, -3.0F, 10);
    public static final RegistryObject<Item> G_STAFF = sword("g_staff", 3, -2.4F, 10);
    public static final RegistryObject<Item> BLAZING_COURAGE = sword("blazing_courage", 3, -2.3F, 10);
    public static final RegistryObject<Item> HARISEN = sword("harisen", 4, -3.0F, 10);
    public static final RegistryObject<Item> SWORD_OF_EXTERMINATION = sword("sword_of_extermination", 3, -2.3F, 10);
    public static final RegistryObject<Item> SWORD_MIWA_KASUMI = sword("sword_miwa_kasumi", 3, -2.3F, 10);
    public static final RegistryObject<Item> NYOI_STAFF = sword("nyoi_staff", 3, -2.4F, 10);
    public static final RegistryObject<Item> SWORD_KUSAKABE = sword("sword_kusakabe", 3, -2.3F, 10);
    public static final RegistryObject<Item> BLACK_ROPE = register("black_rope", () -> new OrcaGeoWeapon("black_rope", "item/hair_black", 3, -2.4F), 10);
    /** Twice the damage of a staff like it (9): 18. */
    public static final RegistryObject<Item> PLAYFUL_CLOUD = register("playful_cloud",
        () -> new OrcaGeoWeapon("playful_cloud", "item/playful_cloud", 12, -2.4F), 25);

    private OrcaWeapons() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static RegistryObject<Item> sword(String id, int damage, float speed, int price) {
        return register(id, () -> new OrcaSword(id, damage, speed), price);
    }

    private static RegistryObject<Item> register(String id, java.util.function.Supplier<Item> item, int price) {
        RegistryObject<Item> object = ITEMS.register(id, item);
        PRICES.put(object, price);
        return object;
    }

    static Item.Properties properties() {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
    }

    /** "Cursed Tool" (or the special-grade line) under the name. */
    static void tooltip(String id, List<Component> tooltip) {
        tooltip.add(Component.translatable("item.mdvlcraft." + id + ".desc").withStyle(ChatFormatting.GRAY));
    }

    static class OrcaSword extends SwordItem {
        private final String id;

        OrcaSword(String id, int damage, float speed) {
            super(CURSED_TOOL, damage, speed, properties());
            this.id = id;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this.id, tooltip);
        }
    }
}
