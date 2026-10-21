package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Modified abilities that help rather than hurt: conjured food (eaten, it gives its boon), cleansing and blessing an
 * ally, a shrine to return to, and the thief's touch on locks and graves.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BoonSpells {
    private static final String BOON = "mdvlcraft_boon";
    private static final String SHRINE = "mdvlcraft_wayshrine";

    private BoonSpells() {
    }

    /** A conjured food tagged with `kind`; eating it runs the boon (here, or in another file's LivingEntityUseItemEvent.Finish handler via kindOf). */
    static ItemStack boon(Item item, String kind, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.getOrCreateTag().putString(BOON, kind);
        stack.setHoverName(Component.translatable("item.mdvlcraft.boon." + kind).withStyle(style -> style.withItalic(false)));
        return stack;
    }

    /** Conjures `count` of a boon food for the caster, or for every player ally within 6 blocks if `party`. */
    public static class Food extends ModifiedSpell {
        private final Item item;
        private final String kind;
        private final int count;
        private final boolean party;

        Food(String key, Item item, String kind, int count, boolean party) {
            super(key, 0);
            this.item = item;
            this.kind = kind;
            this.count = count;
            this.party = party;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            List<LivingEntity> fed = this.party ? Targets.allies(caster, caster.position(), 6.0) : List.of(caster);
            for (LivingEntity ally : fed) {
                if (ally instanceof Player player) {
                    ItemStack food = boon(this.item, this.kind, this.count);
                    if (!player.getInventory().add(food)) {
                        player.spawnAtLocation(food);
                    }
                }
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.8, 0.5, 0.8, 0.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** The boon kind of a conjured food, or "" for any other item. */
    static String kindOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(BOON);
    }

    /** What eating a boon food does. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        CompoundTag tag = event.getItem().getTag();
        if (tag == null || !tag.contains(BOON)) {
            return;
        }
        LivingEntity eater = event.getEntity();
        switch (tag.getString(BOON)) {
            case "alms" -> eater.addEffect(new MobEffectInstance(MobEffects.LUCK, 2400, 0));
            case "forage" -> {
                eater.removeEffect(MobEffects.POISON);
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
            case "peach" -> {
                eater.heal(10.0F);
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            }
            default -> {   // another file's boon food (it handles its own)
            }
        }
    }

    /** The ally looked at within 16 blocks, or the caster. */
    static LivingEntity allyOrSelf(LivingEntity caster) {
        LivingEntity looked = Targets.looked(caster, 16.0F);
        return looked != null && Summons.allied(caster, looked) ? looked : caster;
    }

    /** Baptism: the ally looked at (or the caster) loses its harmful effects and gets Regeneration (10 s) and Water Breathing (3 min). */
    public static class Baptism extends ModifiedSpell {
        Baptism() {
            super("baptism", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = allyOrSelf(caster);
            target.getActiveEffects().stream().filter(e -> !e.getEffect().isBeneficial()).map(MobEffectInstance::getEffect).toList()
                .forEach(target::removeEffect);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), caster);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(1.0), target.getZ(), 40, 0.4, 0.3, 0.4, 0.1);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ancestral Blessing: the ally looked at (or the caster) gets Regeneration and Ancestral Ward (a quarter less spirit damage) for 15 s. */
    public static class AncestorBlessing extends ModifiedSpell {
        AncestorBlessing() {
            super("ancestor_blessing", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = allyOrSelf(caster);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0), caster);
            target.addEffect(new MobEffectInstance(BinderEffects.SPIRIT_WARD.get(), 300, 0), caster);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(1.0), target.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Wayshrine: players only. Cast within 8 blocks of your shrine (or with none in this dimension) to set it where you
     * stand; cast farther away in the same dimension to return to it.
     */
    public static class Wayshrine extends ModifiedSpell {
        Wayshrine() {
            super("wayshrine", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return caster instanceof ServerPlayer;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
                CompoundTag shrine = player.getPersistentData().getCompound(SHRINE);
                boolean here = shrine.getString("Dimension").equals(server.dimension().location().toString());
                Vec3 at = new Vec3(shrine.getDouble("X"), shrine.getDouble("Y"), shrine.getDouble("Z"));
                if (here && player.position().distanceTo(at) > 8.0) {
                    CloneSpells.puff(server, player.position());
                    player.teleportTo(at.x, at.y, at.z);
                    CloneSpells.puff(server, at);
                    level.playSound(null, BlockPos.containing(at), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    CompoundTag set = new CompoundTag();
                    set.putString("Dimension", server.dimension().location().toString());
                    set.putDouble("X", player.getX());
                    set.putDouble("Y", player.getY());
                    set.putDouble("Z", player.getZ());
                    player.getPersistentData().put(SHRINE, set);
                    player.displayClientMessage(Component.translatable("ability.mdvlcraft.modified.shrine_set").withStyle(ChatFormatting.GOLD), true);
                    server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                    level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
                }
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** The block looked at within 5 blocks, or null. */
    @Nullable
    static BlockPos lookedBlock(LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        BlockHitResult hit = caster.level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(5.0)), ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    /** Lockpick: the container looked at loses its lock; an iron door or trapdoor looked at opens or closes. */
    public static class Lockpick extends ModifiedSpell {
        Lockpick() {
            super("lockpick", 0);
        }

        static boolean pickable(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR)
                || level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container && container.saveWithoutMetadata().contains("Lock");
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            BlockPos pos = lookedBlock(caster);
            if (pos == null || !pickable(level, pos)) {
                MartialSpells.refuse(caster, "nothing_locked");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            BlockPos pos = lookedBlock(caster);
            if (pos != null && level instanceof ServerLevel server) {
                BlockState state = server.getBlockState(pos);
                if (state.getBlock() instanceof DoorBlock door) {
                    door.setOpen(caster, server, state, pos, !state.getValue(BlockStateProperties.OPEN));
                } else if (state.getBlock() instanceof TrapDoorBlock) {
                    server.setBlockAndUpdate(pos, state.cycle(BlockStateProperties.OPEN));
                } else if (server.getBlockEntity(pos) instanceof BaseContainerBlockEntity container) {
                    CompoundTag saved = container.saveWithoutMetadata();
                    saved.remove("Lock");
                    container.load(saved);
                    container.setChanged();
                }
                server.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Grave Robbing: digs up the soil, gravel or sand looked at; a quarter of the time it turns up bones, one in ten gold,
     * and one in ten a skeleton that fights the robber.
     */
    public static class GraveRob extends ModifiedSpell {
        GraveRob() {
            super("grave_rob", 0);
        }

        static boolean grave(BlockState state) {
            return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.SOUL_SAND);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            BlockPos pos = lookedBlock(caster);
            if (pos == null || !grave(level.getBlockState(pos))) {
                MartialSpells.refuse(caster, "nothing_to_dig");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            BlockPos pos = lookedBlock(caster);
            if (pos != null && level instanceof ServerLevel server && grave(server.getBlockState(pos))) {
                server.destroyBlock(pos, true, caster);
                Vec3 at = Vec3.atCenterOf(pos);
                float roll = caster.getRandom().nextFloat();
                if (roll < 0.25F) {
                    caster.spawnAtLocation(new ItemStack(Items.BONE, 1 + caster.getRandom().nextInt(3)));
                } else if (roll < 0.35F) {
                    caster.spawnAtLocation(new ItemStack(Items.GOLD_NUGGET, 2 + caster.getRandom().nextInt(4)));
                } else if (roll < 0.45F) {
                    Skeleton skeleton = EntityType.SKELETON.create(server);
                    if (skeleton == null) {
                        throw new IllegalStateException("Cannot create a skeleton");
                    }
                    skeleton.moveTo(at.x, at.y, at.z, caster.getYRot() + 180.0F, 0.0F);
                    skeleton.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
                    skeleton.setTarget(caster);
                    server.addFreshEntity(skeleton);
                    server.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 20, 0.3, 0.6, 0.3, 0.05);
                }
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new Food("alms", Items.BREAD, "alms", 1, true),
            new Food("forage", Items.SWEET_BERRIES, "forage", 4, false),
            new Food("immortal_peach", Items.APPLE, "peach", 1, false),
            new Baptism(), new AncestorBlessing(), new Wayshrine(), new Lockpick(), new GraveRob());
    }
}
