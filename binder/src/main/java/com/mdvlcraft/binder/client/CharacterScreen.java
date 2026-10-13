package com.mdvlcraft.binder.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.mixin.puffish.SkillsClientModAccessor;
import com.mdvlcraft.binder.skills.Respec;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

public class CharacterScreen extends Screen {
    private static final int W = 440;
    private static final int H = 254;
    private static final ResourceLocation RACE_LAYER = ResourceLocation.fromNamespaceAndPath("origins", "origin");
    private static final ResourceLocation MARTIAL_LAYER = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "martial");
    private static final ResourceLocation JOB_LAYER = ResourceLocation.fromNamespaceAndPath("origins-classes", "class");
    private static final Map<ResourceLocation, String> ROW_KEYS = Map.of(
        RACE_LAYER, "screen.mdvlcraft.character.race",
        MARTIAL_LAYER, "screen.mdvlcraft.character.martial",
        JOB_LAYER, "screen.mdvlcraft.character.job"
    );
    private static final int TOOLTIP_WIDTH = 220;
    private static final String CLASS_DEFINITIONS_PATH = "/data/mdvlcraft/puffish_skills/categories/classes/definitions.json";
    private static final JsonObject CLASS_DEFINITIONS = readClassDefinitions();
    private static final ResourceLocation SKY = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/astro_sky.png");
    private static final ResourceLocation ASTROLABE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/character/astrolabe.png");
    private static final ResourceLocation EXPERIENCE = ResourceLocation.fromNamespaceAndPath("puffish_attributes", "experience");
    private static final ResourceLocation CLASSES = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "classes");
    private static final int NAVY = -267249868;
    private static final int GOLD = -5600176;
    private static final int GOLD_FAINT = 1621789776;
    private static final int HEADING = -2047366;
    private static final int LABEL = -2693889;
    private static final int VALUE = -1;
    private static final int MUTED = -7562556;
    private static final float REFERENCE_HIT = 10.0F;
    private final Screen parent;
    private static Method alArmor;
    private static Method alProtection;
    private static boolean alLooked;

    public CharacterScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.character"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        float scale = this.scale();
        int right = Math.round((this.width + 440.0F * scale) / 2.0F);
        int bottom = Math.round((this.height + 254.0F * scale) / 2.0F);
        this.addRenderableWidget(
            new AstroGui.Button(
                right - 66, bottom - 18, 56, 14, Component.translatable("screen.mdvlcraft.menu.back"), () -> Minecraft.getInstance().setScreen(this.parent)
            )
        );
    }

    private float scale() {
        return Math.min(1.0F, Math.min((this.width - 16) / 440.0F, (this.height - 16) / 254.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (AbilityKeys.MENU.matches(key, scan)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(key, scan, modifiers);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            float scale = this.scale();
            float originX = (this.width - 440.0F * scale) / 2.0F;
            float originY = (this.height - 254.0F * scale) / 2.0F;
            int localX = Math.round((mouseX - originX) / scale);
            int localY = Math.round((mouseY - originY) / scale);
            g.pose().pushPose();
            g.pose().translate(originX, originY, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            this.panel(g);
            Font font = this.font;
            g.drawString(font, "✦ " + Component.translatable("screen.mdvlcraft.character").getString(), 10, 8, -2047366, false);
            g.drawString(font, player.getName(), 22, 26, -1, false);
            g.fill(10, 37, 430, 38, 1621789776);
            int left = 18;
            int mid = 160;
            int right = 302;
            int colW = 124;
            Optional<ClientCategoryData> archetype = highestArchetype();
            int y = 46;
            y = this.row(
                g,
                left,
                colW,
                y,
                Component.translatable("screen.mdvlcraft.character.level"),
                archetype.<String>map(a -> String.valueOf(a.getCurrentLevel())).orElse("n/a")
            );
            List<FormattedCharSequence> tooltip = null;
            if (!pickedArchetypes().isEmpty() && hovering(localX, localY, left, colW, y)) {
                tooltip = this.classLines();
            }

            y = this.row(g, left, colW, y, Component.translatable("screen.mdvlcraft.character.class"), className());
            for (ResourceLocation layer : List.of(RACE_LAYER, MARTIAL_LAYER, JOB_LAYER)) {
                Origin origin = origin(player, layer);
                if (origin != null && hovering(localX, localY, left, colW, y)) {
                    tooltip = this.powerLines(origin);
                }

                y = this.row(g, left, colW, y, Component.translatable(ROW_KEYS.get(layer)), origin == null ? "n/a" : origin.getName().getString());
            }

            y = this.row(
                g,
                left,
                colW,
                y,
                Component.translatable("screen.mdvlcraft.character.experience"),
                archetype.<String>map(a -> a.getCurrentExperience() + " / " + (a.getCurrentExperience() + a.getExperienceToNextLevel())).orElse("n/a")
            );
            y += 8;
            y = this.heading(g, left, y, "screen.mdvlcraft.character.attributes");
            y = this.attr(g, player, left, colW, y, "minecraft:generic.attack_damage", v -> fixed(v, 1));
            y = this.attr(g, player, left, colW, y, "minecraft:generic.attack_speed", v -> fixed(v, 2));
            y = this.attr(g, player, left, colW, y, "attributeslib:crit_chance", CharacterScreen::percent);
            y = this.attr(g, player, left, colW, y, "attributeslib:crit_damage", CharacterScreen::percent);
            y = this.attr(g, player, left, colW, y, "attributeslib:life_steal", CharacterScreen::percent);
            y = this.attr(g, player, left, colW, y, "irons_spellbooks:spell_power", CharacterScreen::percent);
            y = this.attr(g, player, left, colW, y, "irons_spellbooks:cooldown_reduction", v -> signedPercent(v - 1.0));
            y = this.attr(g, player, left, colW, y, "irons_spellbooks:mana_regen", CharacterScreen::percent);
            this.attr(g, player, left, colW, y, "minecraft:generic.movement_speed", v -> percent(v / 0.1));
            int var38 = 46;
            var38 = this.heading(g, mid, var38, "screen.mdvlcraft.character.base_stats");
            var38 = this.row(
                g,
                mid,
                colW,
                var38,
                Component.translatable("screen.mdvlcraft.character.hp"),
                fixed(player.getHealth(), 0) + " / " + fixed(player.getMaxHealth(), 0)
            );
            var38 = this.row(
                g,
                mid,
                colW,
                var38,
                Component.translatable("screen.mdvlcraft.character.mana"),
                ClientMagicData.getPlayerMana() + " / " + fixed(player.getAttributeValue((Attribute)AttributeRegistry.MAX_MANA.get()), 0)
            );
            LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(player);
            var38 = this.row(
                g,
                mid,
                colW,
                var38,
                Component.translatable("screen.mdvlcraft.character.stamina"),
                patch == null ? "–" : fixed(patch.getStamina(), 0) + " / " + fixed(patch.getMaxStamina(), 0)
            );
            var38 += 6;
            var38 = this.row(
                g,
                mid,
                colW,
                var38,
                Component.translatable("screen.mdvlcraft.character.poise"),
                fixed(player.getAttributeValue((Attribute)EpicFightAttributes.STUN_ARMOR.get()), 1)
            );
            var38 = this.row(g, mid, colW, var38, Component.translatable("screen.mdvlcraft.character.luck"), fixed(player.getAttributeValue(Attributes.LUCK), 1));
            this.row(g, mid, colW, var38, Component.translatable("screen.mdvlcraft.character.experience_gain"), signedPercent(experienceBonus(player)));
            g.blit(ASTROLABE, mid, 130, 120, 120, 0.0F, 0.0F, 132, 132, 132, 132);
            int var45 = 46;
            var45 = this.heading(g, right, var45, "screen.mdvlcraft.character.attack_power");
            int weapons = 0;

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.isEmpty() && !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty()) {
                    double damage = valueWith(player, Attributes.ATTACK_DAMAGE, stack);
                    double speed = valueWith(player, Attributes.ATTACK_SPEED, stack);
                    String name = slot + 1 + "  " + stack.getHoverName().getString();
                    var45 = this.row(g, right, colW, var45, Component.literal(this.font.plainSubstrByWidth(name, colW - 34)), fixed(damage * speed, 1));
                    weapons++;
                }
            }

            if (weapons == 0) {
                g.drawString(font, Component.translatable("screen.mdvlcraft.character.no_weapons"), right + 4, var45, -7562556, false);
                var45 += 11;
            }

            var45 += 8;
            var45 = this.heading(g, right, var45, "screen.mdvlcraft.character.defence");
            double armor = player.getAttributeValue(Attributes.ARMOR);
            double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            var45 = this.row(
                g,
                right,
                colW,
                var45,
                Component.translatable("screen.mdvlcraft.character.physical"),
                fixed(armor, 1) + "  /  " + negation(player, player.damageSources().mobAttack(player))
            );
            var45 = this.row(g, right, colW, var45, Component.translatable("screen.mdvlcraft.character.toughness"), fixed(toughness, 1));
            var45 = this.row(
                g,
                right,
                colW,
                var45,
                Component.translatable("screen.mdvlcraft.character.projectile"),
                negation(player, player.damageSources().arrow(null, null))
            );
            var45 = this.row(
                g, right, colW, var45, Component.translatable("screen.mdvlcraft.character.fire"), negation(player, player.damageSources().onFire())
            );
            var45 = this.row(
                g,
                right,
                colW,
                var45,
                Component.translatable("screen.mdvlcraft.character.blast"),
                negation(player, player.damageSources().explosion(null, null))
            );
            var45 = this.row(g, right, colW, var45, Component.translatable("screen.mdvlcraft.character.fall"), negation(player, player.damageSources().fall()));
            var45 = this.attr(g, player, right, colW, var45, "minecraft:generic.knockback_resistance", CharacterScreen::percent);
            this.attr(g, player, right, colW, var45, "attributeslib:dodge_chance", CharacterScreen::percent);
            g.fill(10, 232, 430, 233, 1621789776);
            g.drawString(
                font, Component.translatable("screen.mdvlcraft.character.footer", AbilityKeys.MENU.getTranslatedKeyMessage()), 14, 239, -7562556, false
            );
            g.pose().popPose();
            super.render(g, mouseX, mouseY, partialTick);
            if (tooltip != null) {
                g.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    private void panel(GuiGraphics g) {
        g.fill(0, 0, 440, 254, -267249868);

        for (int x = 0; x < 440; x += 128) {
            for (int y = 0; y < 254; y += 128) {
                g.blit(SKY, x, y, 0.0F, 0.0F, Math.min(128, 440 - x), Math.min(128, 254 - y), 128, 128);
            }
        }

        g.fill(0, 0, 440, 1, -5600176);
        g.fill(0, 253, 440, 254, -5600176);
        g.fill(0, 0, 1, 254, -5600176);
        g.fill(439, 0, 440, 254, -5600176);
        g.fill(3, 3, 437, 4, 1621789776);
        g.fill(3, 250, 437, 251, 1621789776);
        g.fill(3, 3, 4, 251, 1621789776);
        g.fill(436, 3, 437, 251, 1621789776);
        g.fill(150, 46, 151, 224, 1621789776);
        g.fill(292, 46, 293, 224, 1621789776);
    }

    private int heading(GuiGraphics g, int x, int y, String key) {
        g.drawString(this.font, "✧ " + Component.translatable(key).getString(), x - 4, y, -2047366, false);
        return y + 13;
    }

    private int row(GuiGraphics g, int x, int width, int y, Component label, String value) {
        int room = width - 4 - this.font.width(value) - 4;
        String text = label.getString();
        if (this.font.width(text) > room) {
            text = this.font.plainSubstrByWidth(text, room - this.font.width(".")) + ".";
        }

        g.drawString(this.font, text, x + 4, y, -2693889, false);
        g.drawString(this.font, value, x + width - this.font.width(value), y, -1, false);
        return y + 11;
    }

    private int attr(GuiGraphics g, LocalPlayer player, int x, int width, int y, String id, DoubleFunction<String> format) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse(id));
        if (attribute != null && player.getAttribute(attribute) != null) {
            String shortName = "screen.mdvlcraft.character.short." + ResourceLocation.parse(id).getPath();
            Component label = I18n.exists(shortName) ? Component.translatable(shortName) : Component.translatable(attribute.getDescriptionId());
            return this.row(g, x, width, y, label, format.apply(player.getAttributeValue(attribute)));
        } else {
            return y;
        }
    }

    /** The player's pick on an Origins layer, or null before they have picked one. */
    private static Origin origin(LocalPlayer player, ResourceLocation layerId) {
        ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsAPI.getLayersRegistry().key(), layerId);
        ResourceKey<Origin> origin = IOriginContainer.get(player).map(container -> container.getOrigin(layer)).orElse(null);
        if (origin == null || !OriginsAPI.getOriginsRegistry().containsKey(origin)) {
            return null;
        }

        return OriginsAPI.getOriginsRegistry().get(origin);
    }

    /** Whether the panel-space mouse position is on the row drawn at y. */
    private static boolean hovering(int localX, int localY, int x, int width, int y) {
        return localX >= x && localX < x + width && localY >= y - 1 && localY < y + 10;
    }

    /** The class name, then each picked archetype with its level and its description from the Classes page. */
    private List<FormattedCharSequence> classLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.literal(className()).withStyle(ChatFormatting.GOLD).getVisualOrderText());
        for (ClientCategoryData archetype : pickedArchetypes()) {
            String id = archetype.getConfig().id().getPath();
            if (!CLASS_DEFINITIONS.has(id)) {
                throw new IllegalStateException("Classes page has no definition for " + id);
            }

            lines.add(Component.translatable("screen.mdvlcraft.respec.level", archetype.getConfig().title(), archetype.getCurrentLevel())
                .withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            String description = CLASS_DEFINITIONS.getAsJsonObject(id).get("description").getAsString();
            lines.addAll(this.font.split(Component.literal(description).withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
        }

        return lines;
    }

    private static JsonObject readClassDefinitions() {
        try (InputStream in = CharacterScreen.class.getResourceAsStream(CLASS_DEFINITIONS_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + CLASS_DEFINITIONS_PATH);
            }

            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + CLASS_DEFINITIONS_PATH, e);
        }
    }

    /** The origin's name, then each visible power's name and description, wrapped for a tooltip. */
    private List<FormattedCharSequence> powerLines(Origin origin) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(origin.getName().copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
        origin.getValidPowers().map(holder -> holder.value().getData()).filter(power -> !power.hidden()).forEach(power -> {
            lines.add(power.getName().copy().withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            lines.addAll(this.font.split(power.getDescription().copy().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
        });
        return lines;
    }

    private static String className() {
        List<ClientCategoryData> picked = pickedArchetypes();
        if (picked.isEmpty()) {
            return "n/a";
        }

        if (picked.size() == 1) {
            return picked.get(0).getConfig().title().getString();
        }

        String key = "screen.mdvlcraft.class." + picked.get(0).getConfig().id().getPath() + "." + picked.get(1).getConfig().id().getPath();
        return I18n.exists(key) ? I18n.get(key) : picked.get(0).getConfig().title().getString() + " / " + picked.get(1).getConfig().title().getString();
    }

    static List<ClientCategoryData> pickedArchetypes() {
        return ((SkillsClientModAccessor)SkillsClientMod.getInstance())
            .mdvlcraft$screenData()
            .streamCategories()
            .filter(c -> !c.getConfig().id().equals(CLASSES) && c.hasExperience())
            .sorted(Comparator.comparingInt(c -> Respec.ARCHETYPES.indexOf(c.getConfig().id().getPath())))
            .toList();
    }

    private static Optional<ClientCategoryData> highestArchetype() {
        return ((SkillsClientModAccessor)SkillsClientMod.getInstance())
            .mdvlcraft$screenData()
            .streamCategories()
            .filter(c -> !c.getConfig().id().equals(CLASSES) && c.hasExperience())
            .max(Comparator.comparingInt(ClientCategoryData::getCurrentLevel).thenComparingInt(ClientCategoryData::getCurrentExperience));
    }

    private static double valueWith(LocalPlayer player, Attribute attribute, ItemStack stack) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return 0.0;
        }

        Set<UUID> held = new HashSet<>();
        player.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute).forEach(mx -> held.add(mx.getId()));
        List<AttributeModifier> modifiers = new ArrayList<>();
        instance.getModifiers().stream().filter(mx -> !held.contains(mx.getId())).forEach(modifiers::add);
        Collection<AttributeModifier> item = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute);
        modifiers.addAll(item);
        double base = instance.getBaseValue();

        for (AttributeModifier m : modifiers) {
            if (m.getOperation() == Operation.ADDITION) {
                base += m.getAmount();
            }
        }

        double value = base;

        for (AttributeModifier m : modifiers) {
            if (m.getOperation() == Operation.MULTIPLY_BASE) {
                value += base * m.getAmount();
            }
        }

        for (AttributeModifier m : modifiers) {
            if (m.getOperation() == Operation.MULTIPLY_TOTAL) {
                value *= 1.0 + m.getAmount();
            }
        }

        return attribute.sanitizeValue(value);
    }

    private static String negation(LocalPlayer player, DamageSource source) {
        float armor = (float)player.getAttributeValue(Attributes.ARMOR);
        float toughness = (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float amount = 10.0F;
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount = afterArmor(player, source, amount, armor, toughness);
        }

        int protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
        if (protection > 0 && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            amount = afterProtection(player, source, amount, protection);
        }

        return percent(1.0 - amount / 10.0F);
    }

    private static void lookUpApothic() {
        if (!alLooked) {
            alLooked = true;

            try {
                Class<?> rules = Class.forName("dev.shadowsoffire.attributeslib.api.ALCombatRules");
                alArmor = rules.getMethod("getDamageAfterArmor", LivingEntity.class, DamageSource.class, float.class, float.class, float.class);
                alProtection = rules.getMethod("getDamageAfterProtection", LivingEntity.class, DamageSource.class, float.class, float.class);
            } catch (ReflectiveOperationException e) {
                alArmor = null;
                alProtection = null;
            }
        }
    }

    private static float afterArmor(LocalPlayer player, DamageSource source, float amount, float armor, float toughness) {
        lookUpApothic();

        try {
            if (alArmor != null) {
                return (Float)alArmor.invoke(null, player, source, amount, armor, toughness);
            }
        } catch (ReflectiveOperationException var6) {
        }

        return CombatRules.getDamageAfterAbsorb(amount, armor, toughness);
    }

    private static float afterProtection(LocalPlayer player, DamageSource source, float amount, int protection) {
        lookUpApothic();

        try {
            if (alProtection != null) {
                return (Float)alProtection.invoke(null, player, source, amount, (float)protection);
            }
        } catch (ReflectiveOperationException var5) {
        }

        return CombatRules.getDamageAfterMagicAbsorb(amount, protection);
    }

    private static String fixed(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** Puffish's experience attribute is dynamic: it multiplies dropped experience the way DynamicModification.applyTo does, so read its modifiers. */
    private static double experienceBonus(LocalPlayer player) {
        AttributeInstance instance = player.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(EXPERIENCE));
        if (instance == null) {
            throw new IllegalStateException("puffish_attributes:experience is missing on the player");
        }

        double initial = 1.0 + instance.getModifiers(Operation.ADDITION).stream().mapToDouble(AttributeModifier::getAmount).sum();
        double result = initial * (1.0 + instance.getModifiers(Operation.MULTIPLY_BASE).stream().mapToDouble(AttributeModifier::getAmount).sum());
        for (AttributeModifier modifier : instance.getModifiers(Operation.MULTIPLY_TOTAL)) {
            result *= 1.0 + modifier.getAmount();
        }

        return result - 1.0;
    }

    private static String percent(double fraction) {
        return fixed(fraction * 100.0, fraction * 100.0 % 1.0 == 0.0 ? 0 : 1) + "%";
    }

    private static String signedPercent(double fraction) {
        return (fraction > 0.0 ? "+" : "") + percent(fraction);
    }
}
