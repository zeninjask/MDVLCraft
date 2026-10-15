package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Modified abilities that leave something in the world or take something from it. */
public final class UtilitySpells {
    private static final String PICKPOCKETED = "mdvlcraft_pickpocketed";

    private UtilitySpells() {
    }

    /** Rallying Banner: a banner planted for 20 s; allies within 6 blocks of it get Resistance I and Regeneration I. */
    public static class Rally extends ModifiedSpell {
        public Rally() {
            super("rally", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                ArmorStand banner = EntityType.ARMOR_STAND.create(server);
                CompoundTag flags = new CompoundTag();
                flags.putBoolean("Marker", true);
                flags.putBoolean("Invisible", true);
                flags.putBoolean("NoGravity", true);
                flags.putBoolean("Invulnerable", true);
                banner.readAdditionalSaveData(flags);
                banner.moveTo(caster.getX(), caster.getY() - 0.6, caster.getZ(), caster.getYRot(), 0.0F);
                banner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.RED_BANNER));
                Vec3 center = caster.position();
                Zones.start(server, center, caster, 0.0F, 400, 20, zone -> {
                    for (LivingEntity ally : Targets.allies(caster, center, 6.0)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, true));
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true));
                    }
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.3, center.z, 12, 3.0, 0.1, 3.0, 0.0);
                }, () -> {
                }, banner);
                server.addFreshEntity(banner);
                level.playSound(null, caster.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Campfire: a campfire for 30 s that heals (Regeneration I) and thaws allies within 5 blocks and cooks food dropped by it. */
    public static class Campfire extends ModifiedSpell {
        public Campfire() {
            super("campfire", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos pos = caster.blockPosition();
                boolean placed = server.getBlockState(pos).isAir() && server.setBlockAndUpdate(pos, Blocks.CAMPFIRE.defaultBlockState());
                Vec3 center = Vec3.atBottomCenterOf(pos);
                Zones.start(server, center, caster, 0.0F, 600, 20, zone -> {
                    for (LivingEntity ally : Targets.allies(caster, center, 5.0)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true));
                        ally.setTicksFrozen(0);
                    }
                    cook(server, center);
                }, () -> {
                    if (placed && server.getBlockState(pos).is(Blocks.CAMPFIRE)) {
                        server.removeBlock(pos, false);
                    }
                }, null);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }

        private static void cook(ServerLevel level, Vec3 center) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(2.0))) {
                ItemStack raw = item.getItem();
                Optional<CampfireCookingRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(raw), level);
                recipe.ifPresent(r -> {
                    ItemStack cooked = r.getResultItem(level.registryAccess()).copy();
                    cooked.setCount(raw.getCount());
                    item.setItem(cooked);
                    level.sendParticles(ParticleTypes.SMOKE, item.getX(), item.getY() + 0.3, item.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
                });
            }
        }
    }

    /** Pickpocket: pulls one item of its loot out of the creature looked at (within 10 blocks); each creature only once. */
    public static class Pickpocket extends ModifiedSpell {
        public Pickpocket() {
            super("pickpocket", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            LivingEntity target = Targets.looked(caster, 10.0F);
            if (!(target instanceof Mob)) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            if (target.getPersistentData().getBoolean(PICKPOCKETED)) {
                MartialSpells.refuse(caster, "already_robbed");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server && Targets.looked(caster, 10.0F) instanceof Mob mob) {
                mob.getPersistentData().putBoolean(PICKPOCKETED, true);
                LootTable table = server.getServer().getLootData().getLootTable(mob.getLootTable());
                LootParams params = new LootParams.Builder(server)
                    .withParameter(LootContextParams.THIS_ENTITY, mob)
                    .withParameter(LootContextParams.ORIGIN, mob.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, caster.damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
                List<ItemStack> loot = table.getRandomItems(params);
                if (loot.isEmpty()) {
                    MartialSpells.refuse(caster, "nothing_to_steal");
                } else {
                    ItemStack stolen = loot.get(server.random.nextInt(loot.size()));
                    if (!(caster instanceof Player player && player.getInventory().add(stolen))) {
                        caster.spawnAtLocation(stolen);
                    }
                    server.sendParticles(ParticleTypes.WITCH, mob.getX(), mob.getY(0.6), mob.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
                    level.playSound(null, caster.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.7F);
                }
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }
}
