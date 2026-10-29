package com.mdvlcraft.binder.specs;

import com.mdvlcraft.binder.monk.Fists;
import com.mdvlcraft.binder.combat.KatanaWeapons;
import com.mdvlcraft.binder.modified.MartialSpells;
import com.mdvlcraft.binder.modified.Summons;
import com.mdvlcraft.binder.modified.Targets;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/** Shared pieces for the specialisation mechanics: delayed work, ground zones, decoys, and what a hit or a weapon is. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SpecKit {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Zone> ZONES = new ArrayList<>();
    /** Mobs pinned on a decoy (or a forced target), until a game time. */
    private static final Map<UUID, Pin> PINNED = new HashMap<>();
    /** Last game time each player was falling (off the ground, falling), for critical hits (critical). */
    private static final Map<UUID, Long> FALLING = new HashMap<>();

    private SpecKit() {
    }

    // ---- time ----

    /** Runs `work` on the server thread `ticks` from now. */
    public static void later(ServerLevel level, int ticks, Runnable work) {
        TASKS.add(new Task(level.getServer().getTickCount() + ticks, work));
    }

    /** A patch of ground that does `everySecond` (with itself) once a second until `ticks` have passed. */
    public static void zone(ServerLevel level, Vec3 centre, double radius, int ticks, @Nullable ParticleOptions particle,
                            Consumer<Zone> everySecond) {
        ZONES.add(new Zone(level, centre, radius, level.getGameTime() + ticks, particle, everySecond));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext();) {
            Task task = it.next();
            if (task.at <= now) {
                due.add(task);
                it.remove();
            }
        }
        due.forEach(task -> task.work.run());
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext();) {
            Zone zone = it.next();
            long time = zone.level.getGameTime();
            if (time >= zone.until) {
                it.remove();
            } else if (time % 20 == 0) {
                zone.everySecond.accept(zone);
            } else if (zone.particle != null && time % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = zone.level.random.nextDouble() * Math.PI * 2;
                    double r = Math.sqrt(zone.level.random.nextDouble()) * zone.radius;
                    zone.level.sendParticles(zone.particle, zone.centre.x + Math.cos(a) * r, zone.centre.y + 0.1, zone.centre.z + Math.sin(a) * r,
                        1, 0.0, 0.05, 0.0, 0.0);
                }
            }
        }
        PINNED.values().removeIf(pin -> pin.until <= now);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        TASKS.clear();
        FALLING.clear();
        ZONES.clear();
        PINNED.clear();
    }

    // ---- targets ----

    public static List<LivingEntity> enemies(LivingEntity of, Vec3 centre, double radius) {
        return Targets.enemies(of, centre, radius);
    }

    public static List<LivingEntity> allies(LivingEntity of, Vec3 centre, double radius) {
        return Targets.allies(of, centre, radius);
    }

    /** The nearest enemy of `of` within `radius` of `centre`, other than `except`. */
    @Nullable
    public static LivingEntity nearestEnemy(LivingEntity of, Vec3 centre, double radius, @Nullable Entity except) {
        LivingEntity best = null;
        for (LivingEntity e : enemies(of, centre, radius)) {
            if (e != except && e != of && (best == null || e.distanceToSqr(centre) < best.distanceToSqr(centre))) {
                best = e;
            }
        }
        return best;
    }

    /** A summon's owner, if it is one (Iron's summons, the pack's Modified summons). */
    @Nullable
    public static LivingEntity owner(Entity entity) {
        return Summons.owner(entity) instanceof LivingEntity owner && owner != entity ? owner : null;
    }

    /** Makes `mob` attack `target` and stay on it until `ticks` from now (see onChangeTarget). */
    public static void pin(Mob mob, LivingEntity target, int ticks) {
        mob.setTarget(target);
        PINNED.put(mob.getUUID(), new Pin(target.getUUID(), mob.level().getServer().getTickCount() + ticks));
    }

    /** Whether `mob` is pinned (no picking its own target). */
    public static boolean pinned(Mob mob) {
        return PINNED.containsKey(mob.getUUID());
    }

    /** Blinds a mob: it drops its target and can't take a new one for `ticks`. */
    public static void blind(Mob mob, int ticks) {
        mob.setTarget(null);
        mob.getPersistentData().putLong("mdvlcraft_blind", mob.level().getGameTime() + ticks);
        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, ticks, 0));
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && event.getEntity().getPersistentData().getLong("mdvlcraft_blind") > event.getEntity().level().getGameTime()) {
            event.setCanceled(true);
            return;
        }
        Pin pin = PINNED.get(event.getEntity().getUUID());
        if (pin != null && (event.getNewTarget() == null || !event.getNewTarget().getUUID().equals(pin.target))) {
            event.setCanceled(true);
        }
    }

    /** A stand-in for `of` at `pos` that the enemies within `radius` attack for `ticks`, then it goes. */
    public static void decoy(ServerLevel level, LivingEntity of, Vec3 pos, double radius, int ticks) {
        ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
        stand.setYRot(of.getYRot());
        stand.setInvisible(false);
        stand.setCustomName(of.getName());
        stand.setCustomNameVisible(false);
        stand.getPersistentData().putBoolean("mdvlcraft_decoy", true);
        level.addFreshEntity(stand);
        for (LivingEntity enemy : enemies(of, pos, radius)) {
            if (enemy instanceof Mob mob) {
                pin(mob, stand, ticks);
            }
        }
        later(level, ticks, () -> {
            stand.discard();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, pos.x, pos.y + 1.0, pos.z, 12, 0.3, 0.5, 0.3, 0.02);
        });
    }

    /** Epic Fight's short stun plus a root, for "stunned" or "staggered" effects. */
    public static void stun(LivingEntity target, int ticks) {
        MartialSpells.stun(target, yesman.epicfight.world.damagesource.StunType.SHORT, ticks / 20.0F);
        MartialSpells.root(target, ticks);
    }

    // ---- mana ----

    /** Gives a player `amount` mana, up to their max, and tells their client. */
    public static void addMana(net.minecraft.server.level.ServerPlayer player, float amount) {
        io.redspace.ironsspellbooks.api.magic.MagicData magic = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
        float max = (float)player.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
        magic.setMana(Math.min(max, magic.getMana() + amount));
        io.redspace.ironsspellbooks.setup.PacketDistributor.sendToPlayer(player, new io.redspace.ironsspellbooks.network.SyncManaPacket(magic));
    }

    public static float maxMana(Player player) {
        return (float)player.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
    }

    // ---- hits ----

    /**
     * Whether a melee hit by `player` now is a critical hit: they were falling within the last 10 ticks. Vanilla checks
     * the moment of the swing, but an Epic Fight attack lands at its animation's contact frame, often after touching down.
     */
    public static boolean critical(Player player) {
        Long at = FALLING.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= 10;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase == TickEvent.Phase.END && !player.level().isClientSide && !player.onGround() && player.fallDistance > 0.0F
            && !player.onClimbable() && !player.isInWater()) {
            FALLING.put(player.getUUID(), player.level().getGameTime());
        }
    }

    /** The school of the Iron's spell that dealt `source`, or null if no spell did. */
    @Nullable
    public static ResourceLocation school(DamageSource source) {
        return source instanceof SpellDamageSource spell && spell.spell() != null ? spell.spell().getSchoolType().getId() : null;
    }

    public static boolean school(DamageSource source, String... paths) {
        ResourceLocation id = school(source);
        if (id == null) {
            return false;
        }
        for (String path : paths) {
            if (id.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean spell(DamageSource source) {
        return source instanceof SpellDamageSource;
    }

    /** A player's own weapon or fist hit (not a spell, arrow or skill effect). */
    public static boolean melee(DamageSource source, Entity attacker) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == attacker;
    }

    public static WeaponCategory category(ItemStack stack) {
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        return cap == null ? null : cap.getWeaponCategory();
    }

    public static boolean katana(LivingEntity holder) {
        ItemStack stack = holder.getMainHandItem();
        return KatanaWeapons.isKatana(category(stack), stack);
    }

    public static boolean dagger(LivingEntity holder) {
        return category(holder.getMainHandItem()) == yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.DAGGER
            || named(holder.getMainHandItem(), "dagger", "knife", "stiletto", "kunai", "dirk", "stylet");
    }

    public static boolean spear(LivingEntity holder) {
        return category(holder.getMainHandItem()) == yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.SPEAR
            || named(holder.getMainHandItem(), "spear", "halberd", "glaive", "pike", "lance");
    }

    public static boolean axe(LivingEntity holder) {
        return category(holder.getMainHandItem()) == yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.AXE
            || named(holder.getMainHandItem(), "axe");
    }

    public static boolean unarmed(LivingEntity holder) {
        return holder instanceof Player player && Fists.unarmed(player);
    }

    /** Whether the item's id contains any of `words`. */
    public static boolean named(ItemStack stack, String... words) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath();
        for (String word : words) {
            if (path.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Epic Fight's patch of a living entity, if it has one. */
    @Nullable
    public static LivingEntityPatch<?> patch(LivingEntity entity) {
        return EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
    }

    private record Task(int at, Runnable work) {
    }

    private record Pin(UUID target, int until) {
    }

    /** A patch of ground (SpecKit.zone). */
    public record Zone(ServerLevel level, Vec3 centre, double radius, long until, @Nullable ParticleOptions particle,
                       Consumer<Zone> everySecond) {
        public boolean inside(Entity entity) {
            return entity.position().distanceToSqr(this.centre) <= this.radius * this.radius && Math.abs(entity.getY() - this.centre.y) < 3.0;
        }
    }
}
