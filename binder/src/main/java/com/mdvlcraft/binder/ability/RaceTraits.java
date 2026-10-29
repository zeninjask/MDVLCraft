package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The races' passive effects that Origins' power types can't express (tools/races/build_races.py marks them text-only
 * there; the text is what the picker shows). Each checks the player's race (RaceAbilities.race).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RaceTraits {
    private static final String WATCHED = "mdvlcraft_watched_over";
    private static final String EMBER = "mdvlcraft_ember_skin";
    private static final String OUTCAST = "mdvlcraft_outcast";
    private static final String LONG = "mdvlcraft_long_summon";
    private static Method expirationTick;

    private RaceTraits() {
    }

    static boolean is(Entity entity, String race) {
        return entity instanceof ServerPlayer player
            && ResourceLocation.fromNamespaceAndPath("mdvlcraft", race).equals(RaceAbilities.race(player));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T nearest(Level level, Class<T> type, Entity centre, double radius, java.util.function.Predicate<T> filter) {
        return level.getEntitiesOfClass(type, centre.getBoundingBox().inflate(radius), filter).stream()
            .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(centre))).orElse(null);
    }

    // ---- hits ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        // Stormkin, Waterlogged: water spells hurt 25% more
        if (is(victim, "stormkin") && SpecKit.school(source, "aqua", "water")) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        // Shrinekin, Black Flash Instinct: critical hits deal 15% more
        if (source.getDirectEntity() instanceof Player player && source.getEntity() == player && !source.is(DamageTypes.INDIRECT_MAGIC)
            && SpecKit.critical(player) && is(player, "shrinekin")) {
            event.setAmount(event.getAmount() * 1.15F);
        }
        // Curse-Bonded, Copy: 10% chance to learn the spell that hit you, for 60 s
        if (victim instanceof ServerPlayer player && source instanceof SpellDamageSource spellSource && spellSource.spell() != null
            && source.getEntity() != player && is(player, "curse_bonded") && player.getRandom().nextFloat() < 0.10F) {
            Ability.byId(spellSource.spell().getSpellResource()).ifPresent(ability -> {
                if (AbilityGrants.level(player, ability) > 0) {
                    return;
                }
                AbilityGrants.give(player, ability, 1);
                player.displayClientMessage(Component.translatable("message.mdvlcraft.race.copy", spellSource.spell().getDisplayName(player))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                SpecKit.later(player.serverLevel(), 1200, () -> AbilityGrants.give(player, ability, 0));
            });
        }
    }

    /** Piglin-born, Ember Skin: entering fire or lava gives 3 s of fire immunity (once per entry). */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && !victim.getPersistentData().getBoolean(EMBER) && is(victim, "piglinborn")) {
            victim.getPersistentData().putBoolean(EMBER, true);
            victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false, true));
            event.setCanceled(true);
        }
    }

    /** Curse-Bonded, Watched Over: once every 20 minutes the spirit blocks a fatal hit. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        long now = victim.level().getGameTime();
        if (is(victim, "curse_bonded") && now >= victim.getPersistentData().getLong(WATCHED)) {
            victim.getPersistentData().putLong(WATCHED, now + 24000);
            victim.setHealth(Math.max(1.0F, victim.getMaxHealth() * 0.1F));
            victim.invulnerableTime = 40;
            victim.level().playSound(null, victim.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 0.6F);
            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, victim.getX(), victim.getY(1.0), victim.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
            }
            event.setCanceled(true);
        }
    }

    // ---- targets ----

    /** Kitsune, Illager Ire: illagers go for a nearby kitsune first. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof AbstractIllager illager && event.getNewTarget() != null && !is(event.getNewTarget(), "kitsune")
            && !SpecKit.pinned(illager)) {
            Player kitsune = nearest(illager.level(), Player.class, illager, 16.0, p -> is(p, "kitsune") && !p.isCreative()
                && !p.isSpectator() && illager.hasLineOfSight(p));
            if (kitsune != null) {
                event.setNewTarget(kitsune);
            }
        }
    }

    // ---- once a second ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (player.getPersistentData().getBoolean(EMBER) && !player.isOnFire() && !player.isInLava()) {
            player.getPersistentData().remove(EMBER);
        }
        if (is(player, "kitsune")) {
            for (Fox fox : level.getEntitiesOfClass(Fox.class, player.getBoundingBox().inflate(16.0))) {
                trust(fox, player.getUUID());
            }
        }
        if (is(player, "illagerkin")) {
            outcast(level, player);
            bannerBearer(level, player);
        }
    }

    /** Kitsune, Fox Friend: foxes trust the kitsune (they don't run, and fight for it as for a player they trust). */
    private static void trust(Fox fox, UUID uuid) {
        CompoundTag data = new CompoundTag();
        fox.addAdditionalSaveData(data);
        ListTag trusted = data.getList("Trusted", Tag.TAG_INT_ARRAY);
        Tag mine = NbtUtils.createUUID(uuid);
        if (!trusted.contains(mine)) {
            trusted.add(0, mine);
            data.put("Trusted", trusted);
            fox.readAdditionalSaveData(data);
        }
    }

    /** Illagerkin, Outcast: a villager meeting one thinks the worst of them, so iron golems attack and prices go up. */
    private static void outcast(ServerLevel level, ServerPlayer player) {
        String key = player.getStringUUID();
        for (Villager villager : level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0))) {
            ListTag met = villager.getPersistentData().getList(OUTCAST, Tag.TAG_STRING);
            if (!met.contains(StringTag.valueOf(key))) {
                met.add(StringTag.valueOf(key));
                villager.getPersistentData().put(OUTCAST, met);
                villager.getGossips().add(player.getUUID(), GossipType.MAJOR_NEGATIVE, 25);
            }
        }
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(16.0))) {
            if (golem.getTarget() == null && !golem.isPlayerCreated() && !player.isCreative()) {
                golem.setTarget(player);
            }
        }
    }

    /** Illagerkin, Banner Bearer: wearing an ominous banner, the 2 nearest illagers fight for you (your target, or your attacker). */
    private static void bannerBearer(ServerLevel level, ServerPlayer player) {
        if (!ItemStack.matches(player.getItemBySlot(EquipmentSlot.HEAD), Raid.getLeaderBannerInstance())) {
            return;
        }
        LivingEntity enemy = player.getLastHurtMob() != null && player.tickCount - player.getLastHurtMobTimestamp() < 200
            ? player.getLastHurtMob() : player.getLastHurtByMob();
        if (enemy == null || !enemy.isAlive() || enemy instanceof AbstractIllager) {
            return;
        }
        level.getEntitiesOfClass(AbstractIllager.class, player.getBoundingBox().inflate(16.0), illager -> illager.getTarget() != enemy).stream()
            .sorted(java.util.Comparator.comparingDouble(i -> i.distanceToSqr(player))).limit(2)
            .forEach(illager -> SpecKit.pin(illager, enemy, 60));
    }

    /** Silent Steps (Sculkborn): sculk sensors and shriekers never hear the player. */
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() != null && is(event.getCause(), "sculkborn")) {
            event.setCanceled(true);
        }
    }

    /** Piglin-born, Zombify: back in the overworld from the nether, Nausea and Weakness for 10 s. */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getFrom() == Level.NETHER && event.getTo() == Level.OVERWORLD && is(event.getEntity(), "piglinborn")) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }
    }

    /** Curse-Bonded, Dread: animals won't breed within 4 blocks. */
    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        Mob parent = event.getParentA();
        if (parent != null && nearest(parent.level(), Player.class, parent, 4.0, p -> is(p, "curse_bonded")) != null) {
            event.setCanceled(true);
        }
    }

    /** Shrinekin, Mouth on the Palm: eating and drinking take half as long. */
    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        UseAnim anim = event.getItem().getUseAnimation();
        if ((anim == UseAnim.EAT || anim == UseAnim.DRINK) && is(event.getEntity(), "shrinekin")) {
            event.setDuration(Math.max(1, event.getDuration() / 2));
        }
    }

    /** Umbral, Long Summons: summons last 25% longer (their expiry, a tick after they join, is pushed back). */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob) || mob.getPersistentData().getBoolean(LONG)) {
            return;
        }
        SpecKit.later(level, 1, () -> {
            Entity owner = SummonManager.getOwner(mob);
            if (owner == null || !mob.isAlive() || !is(owner, "umbral")) {
                return;
            }
            int left = expirationTick(mob) - level.getServer().getTickCount();
            if (left > 0) {
                mob.getPersistentData().putBoolean(LONG, true);
                SummonManager.stopTrackingExpiration(mob);
                SummonManager.setDuration(mob, Math.round(left * 1.25F));
            }
        });
    }

    private static int expirationTick(Entity mob) {
        try {
            if (expirationTick == null) {
                expirationTick = SummonManager.class.getDeclaredMethod("getExpirationTick", UUID.class);
                expirationTick.setAccessible(true);
            }
            return (int)expirationTick.invoke(SummonManager.INSTANCE, mob.getUUID());
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }
}
