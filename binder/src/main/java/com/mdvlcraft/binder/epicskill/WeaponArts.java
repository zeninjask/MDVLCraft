package com.mdvlcraft.binder.epicskill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import com.mdvlcraft.binder.wukong.WukongAnimations;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.gameasset.Animations;

/**
 * Weapons whose own right-click (or hold-right-click) ability is not part of their combo. That ability is taken off right
 * click (onRightClick blocks it, so right click stays Epic Fight's guard) and becomes the weapon's innate skill
 * (WeaponArtSkill): an attack animation borrowed from Epic Fight's movesets, and at the animation's hit the item's own
 * code runs as if it had been used, through Item.use / useOn / onUseTick / releaseUsing. The item's own cooldown is
 * cleared around that call: the innate's weapon charge is the cost instead (skill_parameters/art_*.json).
 *
 * Not here: weapons Epic Fight Cataclysm (p1nero_ec) already turns into innate skills (Astrape, Ceraunus, The
 * Annihilator, Gauntlet of Guard, Soul Render, Tidal Claws, Infernal Forge), the Demon Dweller (DemonDwellerSlashSkill),
 * casting staves (right click casts the selected spell), mode toggles (Thorns of Oblivion and Abyssal Tidecaller forms,
 * the Spellblade), Epic Knights' lance raise and block, Sword Soaring's flight, and the admin-only Gu Ding Dao.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class WeaponArts {
    /** How the item's ability is set off. */
    public enum Mode {
        /** Item.use, once. */
        USE,
        /** Item.useOn, on the block under the player (ground slams). */
        USE_ON,
        /** Item.releaseUsing after `charge` ticks of holding (charged throws and blasts). */
        RELEASE,
        /** Item.onUseTick for each of `ticks` held ticks at once, then releaseUsing (effects that wait for a held tick). */
        TICKS_THEN_RELEASE,
        /** Item.use, then Item.onUseTick once a game tick for `ticks` ticks, then releaseUsing (channelled effects). */
        CHANNEL
    }

    /** One weapon art: its skill name, items, how it fires, the animation, and the held time it stands for. */
    public record Art(String name, List<String> items, Mode mode, Supplier<AnimationAccessor<? extends AttackAnimation>> animation, int ticks,
                      boolean sneaking) {
    }

    public static final List<Art> ARTS = new ArrayList<>();
    private static final Map<ResourceLocation, Art> BY_ITEM = new HashMap<>();
    private static final Map<ServerPlayer, Pending> PENDING = new LinkedHashMap<>();
    private static boolean firing;

    static {
        // Cataclysm
        art("art_brontes", Mode.RELEASE, () -> Animations.WRATHFUL_LIGHTING, 30, "cataclysm:brontes");
        art("art_coral_throw", Mode.RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 20, "cataclysm:coral_spear", "cataclysm:coral_bardiche");
        art("art_bulwark_blast", Mode.TICKS_THEN_RELEASE, () -> Animations.FIST_AUTO3, 21, "cataclysm:gauntlet_of_bulwark");
        art("art_maelstrom", Mode.RELEASE, () -> Animations.FIST_DASH, 20, "cataclysm:gauntlet_of_maelstrom");
        art("art_shred", Mode.CHANNEL, () -> Animations.RELENTLESS_COMBO, 30, "cataclysm:meat_shredder");
        art("art_immolate", Mode.RELEASE, () -> Animations.WRATHFUL_LIGHTING, 45, "cataclysm:the_immolator");
        art("art_incinerate", Mode.RELEASE, WeaponArts::slam, 60, "cataclysm:the_incinerator");
        // Alex's Caves
        art("art_extinction", Mode.TICKS_THEN_RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 20, "alexscaves:extinction_spear");
        art("art_spear_throw", Mode.RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 20, "alexscaves:frostmint_spear", "alexscaves:limestone_spear");
        art("art_ortholance", Mode.RELEASE, () -> Animations.GRASPING_SPIRAL_FIRST, 60, "alexscaves:ortholance");
        // Legendary Monsters
        art("art_mossy_slam", Mode.USE_ON, WeaponArts::slam, 0, "legendary_monsters:mossy_hammer");
        art("art_bone_shockwave", Mode.USE_ON, WeaponArts::slam, 0, "legendary_monsters:dinosaur_bone_club");
        art("art_frost_fangs", Mode.USE, () -> Animations.SWORD_DASH, 0, "legendary_monsters:the_great_frost");
        art("art_lightning_fangs", Mode.USE, () -> Animations.AXE_DASH, 0, "legendary_monsters:axe_of_lightning");
        art("art_chorus_shift", Mode.USE, () -> Animations.SWORD_DASH, 0, "legendary_monsters:chorus_blade");
        art("art_fire_breath", Mode.USE, () -> Animations.AXE_AUTO2, 0, "legendary_monsters:fiery_jaw");
        art("art_anchor_slam", Mode.RELEASE, WeaponArts::slam, 15, "legendary_monsters:monstrous_anchor");
        art("art_javelin_throw", Mode.RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 10, "legendary_monsters:resurrected_javelin");
        art("art_soul_parry", Mode.CHANNEL, () -> Animations.SWEEPING_EDGE, 20, "legendary_monsters:soul_great_sword");
        art("art_annihilation", Mode.TICKS_THEN_RELEASE, WeaponArts::slam, 30, "legendary_monsters:the_tesseract");
        art("art_withered_dash", Mode.CHANNEL, () -> Animations.GRASPING_SPIRAL_FIRST, 20, "legendary_monsters:withered_scythe");
        // T.O Magic 'n Extras (every level of a weapon shares its art)
        art("art_berserk", Mode.RELEASE, () -> Animations.SWEEPING_EDGE, 20, levels("traveloptics:cursed_wraithblade"));
        art("art_devastator", Mode.TICKS_THEN_RELEASE, () -> Animations.SHARP_STAB, 57, levels("traveloptics:infernal_devastator"));
        art("art_plasma_laser", Mode.CHANNEL, () -> Animations.SHARP_STAB, 40, levels("traveloptics:mechanized_wraithblade"));
        art("art_desert_stele", Mode.RELEASE, WeaponArts::slam, 20, levels("traveloptics:scourge_of_the_sands"));
        art("art_stellothorn", Mode.RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 20, levels("traveloptics:stellothorn"));
        art("art_obliterate", Mode.TICKS_THEN_RELEASE, WeaponArts::slam, 40, levels("traveloptics:the_obliterator"));
        art("art_maelstrom_trident", Mode.RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 20, levels("traveloptics:trident_of_the_eternal_maelstrom"));
        art("art_void_rings", Mode.USE, WeaponArts::slam, 0, levels("traveloptics:voidstrike_reaper"));
        // Others
        art("art_twilight_throw", Mode.RELEASE, () -> Animations.SPEAR_TWOHAND_AUTO2, 10, "irons_spellbooks:twilight_gale");
        ARTS.add(new Art("art_spellbane_charge", List.of("iss_csw:spellbane_greatblade"), Mode.RELEASE, () -> Animations.GRASPING_SPIRAL_FIRST,
            20, true));
        art("art_anti_magic_shroud", Mode.USE, () -> Animations.SWEEPING_EDGE, 0, "antimagic:demon_slayer_sword");
        art("art_causality_break", Mode.USE, () -> Animations.SHARP_STAB, 0, "antimagic:demon_destroyer_sword");
        for (Art art : ARTS) {
            for (String item : art.items()) {
                BY_ITEM.put(ResourceLocation.tryParse(item), art);
            }
        }
    }

    private WeaponArts() {
    }

    /** A two-handed overhead smash into the ground on the spot (the Wukong staff's charged smash, level 1). */
    private static AnimationAccessor<? extends AttackAnimation> slam() {
        return WukongAnimations.SMASHES.get(1);
    }

    private static void art(String name, Mode mode, Supplier<AnimationAccessor<? extends AttackAnimation>> animation, int ticks, String... items) {
        ARTS.add(new Art(name, List.of(items), mode, animation, ticks, false));
    }

    /** A T.O weapon and its three upgraded levels. */
    private static String[] levels(String id) {
        return new String[] {id, id + "_level_one", id + "_level_two", id + "_level_three"};
    }

    @Nullable
    public static Art of(ItemStack stack) {
        return stack.isEmpty() ? null : BY_ITEM.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** True while an art runs the item's own code (the item's use is otherwise blocked). */
    public static boolean firing() {
        return firing;
    }

    /** Ticks from the start of the animation to its last hit. */
    public static int contactTicks(Art art) {
        AttackAnimation animation = art.animation().get().get();
        AttackAnimation.Phase last = animation.phases[animation.phases.length - 1];
        return Math.max(1, Math.round(last.contact * 20.0F));
    }

    /** Called by WeaponArtSkill as its animation starts: the art goes off at the animation's hit. */
    static void start(ServerPlayer player, Art art) {
        PENDING.put(player, new Pending(art, player.getMainHandItem(), contactTicks(art)));
    }

    // ---- right click no longer sets the ability off ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!firing && of(event.getItemStack()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!firing && of(event.getItemStack()) != null) {
            event.setUseItem(Result.DENY);
        }
    }

    // ---- firing ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Pending pending = PENDING.get(player);
        if (pending == null) {
            return;
        }
        if (!player.isAlive() || player.getMainHandItem() != pending.stack) {
            PENDING.remove(player);
            return;
        }
        if (pending.delay > 0) {
            pending.delay--;
            if (pending.delay == 0) {
                begin(player, pending);
            }
            return;
        }
        // a channel in progress
        int duration = pending.stack.getUseDuration();
        run(player, pending, () -> pending.stack.getItem().onUseTick(player.level(), player, pending.stack, duration - ++pending.held));
        if (pending.held >= pending.art.ticks()) {
            run(player, pending, () -> pending.stack.getItem().releaseUsing(pending.stack, player.level(), player, duration - pending.held));
            PENDING.remove(player);
        }
    }

    private static void begin(ServerPlayer player, Pending pending) {
        ItemStack stack = pending.stack;
        Item item = stack.getItem();
        Art art = pending.art;
        int duration = stack.getUseDuration();
        switch (art.mode()) {
            case USE -> {
                run(player, pending, () -> item.use(player.level(), player, InteractionHand.MAIN_HAND));
                PENDING.remove(player);
            }
            case USE_ON -> {
                BlockPos below = player.blockPosition().below();
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0.0, 0.5, 0.0), Direction.UP, below, false);
                run(player, pending, () -> item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
                PENDING.remove(player);
            }
            case RELEASE -> {
                run(player, pending, () -> item.releaseUsing(stack, player.level(), player, duration - art.ticks()));
                PENDING.remove(player);
            }
            case TICKS_THEN_RELEASE -> {
                run(player, pending, () -> {
                    for (int held = 1; held <= art.ticks(); held++) {
                        item.onUseTick(player.level(), player, stack, duration - held);
                    }
                    item.releaseUsing(stack, player.level(), player, duration - art.ticks());
                });
                PENDING.remove(player);
            }
            case CHANNEL -> run(player, pending, () -> item.use(player.level(), player, InteractionHand.MAIN_HAND));
        }
    }

    /** Runs the item's own code with its cooldown cleared before and after, sneaking if the art needs it. */
    private static void run(ServerPlayer player, Pending pending, Runnable work) {
        Item item = pending.stack.getItem();
        boolean wasSneaking = player.isShiftKeyDown();
        firing = true;
        try {
            player.getCooldowns().removeCooldown(item);
            if (pending.art.sneaking()) {
                player.setShiftKeyDown(true);
            }
            work.run();
        } finally {
            firing = false;
            player.setShiftKeyDown(wasSneaking);
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            player.getCooldowns().removeCooldown(item);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity());
    }

    private static final class Pending {
        final Art art;
        final ItemStack stack;
        int delay;
        int held;

        Pending(Art art, ItemStack stack, int delay) {
            this.art = art;
            this.stack = stack;
            this.delay = delay;
        }
    }
}
